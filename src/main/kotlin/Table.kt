import org.tinylog.Logger
import statements.*
import utils.*
import java.math.BigDecimal
import java.sql.Date
import java.sql.Time
import java.sql.Timestamp
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSupertypeOf
import kotlin.reflect.jvm.javaType


@Suppress("UNCHECKED_CAST")
inline fun <reified E : Entity> autoTable(noinline columnsBody: Table<E>.CreateMethods.() -> Unit): Table<E> =
    Table(E::class) {
        columnsBody()
        E::class.properties.map { it as KMutableProperty1<E, Any?> }.forEach { prop ->
            if (prop.name == "id") return@forEach
            when {
                prop.returnType.isSupertypeOf(UByte::class.createType()) -> tinyint(prop as KMutableProperty1<E, UByte?>)
                prop.returnType.isSupertypeOf(Short::class.createType()) -> smallint(prop as KMutableProperty1<E, Short?>)
                prop.returnType.isSupertypeOf(Int::class.createType()) -> int(prop as KMutableProperty1<E, Int?>)
                prop.returnType.isSupertypeOf(Long::class.createType()) -> bigint(prop as KMutableProperty1<E, Long?>)
                prop.returnType.isSupertypeOf(Time::class.createType()) -> time(prop as KMutableProperty1<E, Time?>)
                prop.returnType.isSupertypeOf(Timestamp::class.createType()) -> timestamp(prop as KMutableProperty1<E, Timestamp?>)
                prop.returnType.isSupertypeOf(Date::class.createType()) -> date(prop as KMutableProperty1<E, Date?>)
                prop.returnType.isSupertypeOf(BigDecimal::class.createType()) -> decimal(prop as KMutableProperty1<E, BigDecimal?>, 10, 2)
                prop.returnType.isSupertypeOf(Float::class.createType()) -> real(prop as KMutableProperty1<E, Float?>)
                prop.returnType.isSupertypeOf(Double::class.createType()) -> double(prop as KMutableProperty1<E, Double?>)
                prop.returnType.isSupertypeOf(String::class.createType()) -> text(prop as KMutableProperty1<E, String?>)
                prop.returnType.isSupertypeOf(Boolean::class.createType()) -> bool(prop as KMutableProperty1<E, Boolean?>)
                else -> reference(prop as KMutableProperty1<E, Entity?>)
            }
        }
    }

inline fun <reified E : Entity> table(noinline columnsBody: Table<E>.CreateMethods.() -> Unit): Table<E> =
    Table(E::class, columnsBody)

enum class Action {
    Cascade, SetNull, SetDefault
}

open class Table<E : Entity>(
    val entityClass: KClass<E>,
    columnsBody: Table<E>.CreateMethods.() -> Unit
) {
    val cache = CacheMap<E>(Config.maxCacheSize)
    var tableName = entityClass.simpleName!!.transformCase(Case.Pascal, Case.Snake, true)
    val columns = mutableListOf<Column<*>>()
    val references = mutableListOf<Reference<*>>()
    val uniqueColumns = mutableSetOf<String>()
    internal val referencesAddMethods: MutableSet<() -> Unit> = mutableSetOf()
    private var defaultEntitiesMethod: () -> List<E> = { listOf() }

    val defaultEntities by lazy { defaultEntitiesMethod() }

    val size: Int
        get() = select(Entity::id) { it.count() }.toInt()

    fun isEmpty() = size == 0

    init {
        tables[entityClass] = this
        if (tableName in database.reservedKeyWords)
            throw LoggerException("\"$tableName\" is a reserved SQL keyword!")

        with(CreateMethods()) {
            @Suppress("UNCHECKED_CAST")
            serial(entityClass.properties.first { it.name == "id" } as KMutableProperty1<E, Int>).primaryKey()
            columnsBody()
        }

        if (Config.refreshTables) dropTable()
        createTable()
    }

    fun createTable() = create()
    fun dropTable() = drop()
    fun clearTable() = delete()


    operator fun plusAssign(entity: E) {
        add(entity)
    }

    fun add(entity: E): E? = insert(entity).getId()?.let { id -> entity.apply { this.id = id } }
    fun add(vararg entities: E): List<E> = add(entities.toList())
    fun add(entities: List<E>): List<E> = if (entities.isEmpty()) listOf() else insert(entities).getIds()
        .let { idList -> entities.apply { idList.forEachIndexed { index, id -> entities[index].id = id } } }


    operator fun get(id: Int): E? = findById(id)
    operator fun contains(entity: E): Boolean = select().apply {
        entity.properties.forEach { prop ->
            if (prop.name != "id")
                where { prop eq prop.returnValue(entity) }
        }
    }.getResultSet().next()

    fun containsAll(entities: List<E>): Boolean = entities.all(::contains)
    fun containsAny(entities: List<E>): Boolean = entities.any(::contains)
    operator fun contains(entities: List<E>): Boolean = containsAll(entities)

    fun all(loadReferences: Boolean = true, condition: WhereCondition? = null): List<E> = selectAll().where(condition)
        .apply { if (!loadReferences) lazy() }.getEntities()

    fun findById(id: Int, loadReferences: Boolean = true): E? =
        cache[id, loadReferences] ?: first(loadReferences) { Entity::id eq id }

    fun findIdOf(condition: WhereCondition): Int? = select(Entity::id).where(condition).getEntity()?.id

    fun first(loadReferences: Boolean = true, condition: WhereCondition? = null): E? =
        selectAll().where(condition).limit(1).apply { if (!loadReferences) lazy() }.getEntity()

    fun last(loadReferences: Boolean = true, condition: WhereCondition? = null): E? =
        selectAll().where(condition).orderByDescending().limit(1).apply { if (!loadReferences) lazy() }.getEntity()

    fun take(n: Int, loadReferences: Boolean = true) =
        selectAll().limit(n).apply { if (!loadReferences) lazy() }.getEntities()

    fun takeLast(n: Int, loadReferences: Boolean = true) =
        selectAll().orderByDescending().limit(n).apply { if (!loadReferences) lazy() }.getEntities()

    fun count(condition: WhereCondition): Int = select().where(condition).size


    operator fun set(id: Int, entity: E) = update(entity) { this.id = id }
    inline fun update(entity: E, vararg props: KMutableProperty1<E, *>, func: E.() -> Unit = {}) {
        func(entity)
        update(entity, props.toList())
    }

    fun update(entities: List<E>) = entities.forEach { update(it) }

    operator fun minusAssign(entity: E) = delete(entity)
    fun delete(entity: E): Unit = deleteById(entity.id)

    fun <T> getValuesOfColumn(prop: KMutableProperty1<E, T>): List<T> = select(prop).getEntities().map(prop)


    inner class Reference<P : Entity?>(
        property: KMutableProperty1<E, P>,
        private val onDelete: Action
    ) :
        Column<P>(property, "integer") {

        init {
            if (!references.any { it.property == property }) {
                referencesAddMethods.add { alter().addForeignKey(property, refTable!!, onDelete) }
                references.add(this)
            }
        }
    }


    open inner class Column<T>(
        val property: KMutableProperty1<E, T>,
        val sqlType: String
    ) {
        @Suppress("UNCHECKED_CAST")
        val refTable by lazy { get((property.returnType.javaType as Class<Entity>).kotlin) }
        val name: String = property.columnName
        private var defaultValue: T? = null
        private var isNotNull = false
        private var isUnique = false
        private var isPrimaryKey = false

        init {
            if (!columns.any { it.property == property }) {
                if (name in database.reservedKeyWords)
                    throw LoggerException("\"$name\" is a reserved SQL keyword!")
                columns.add(this)
                if (!property.returnType.isMarkedNullable)
                    isNotNull = true
            }
        }

        fun notNull() = this.also {
            isNotNull = true
            if (property.returnType.isMarkedNullable)
                Logger.warn { "Nullable value shouldn't be marked as not null! (column: $tableName.$name)" }
        }

        fun unique() = this.also { isUnique = true }
        fun primaryKey() = this.also { isPrimaryKey = true }
        fun default(value: T) = this.also { defaultValue = value }

        protected open fun attributesToSql(): String = "PRIMARY KEY ".ifTrue(isPrimaryKey) +
                "NOT NULL ".ifTrue(isNotNull) +
                "UNIQUE ".ifTrue(isUnique) +
                "DEFAULT $defaultValue".ifTrue(defaultValue != null && defaultValue !is String) +
                "DEFAULT '$defaultValue'".ifTrue(defaultValue != null && defaultValue is String)


        fun toSql(nameLength: Int = name.length): String =
            "${name.padEnd(nameLength)} ${sqlType.uppercase()} ${attributesToSql()}".trim()
    }

    inner class CreateMethods {
        fun defaultEntities(entities: () -> List<E>) {
            defaultEntitiesMethod = entities
        }

        fun <T : Int?> serial(prop: KMutableProperty1<E, T>) = Column(prop, "serial")

        fun <T : BigDecimal?> decimal(prop: KMutableProperty1<E, T>, precision: Int, scale: Int) =
            Column(prop, "decimal($precision, $scale)")

        fun <T : BigDecimal?> numeric(prop: KMutableProperty1<E, T>, precision: Int, scale: Int) =
            Column(prop, "numeric($precision, $scale)")

        fun <T : Long?> bigint(prop: KMutableProperty1<E, T>) = Column(prop, "bigint")
        fun <T : Int?> int(prop: KMutableProperty1<E, T>) = Column(prop, "integer")
        fun <T : Short?> smallint(prop: KMutableProperty1<E, T>) = Column(prop, "smallint")
        fun <T : UByte?> tinyint(prop: KMutableProperty1<E, T>) = Column(prop, "tinyint")

        fun <T : Boolean?> bool(prop: KMutableProperty1<E, T>) = Column(prop, "boolean")

        fun <T : Double?> double(prop: KMutableProperty1<E, T>) = Column(prop, "double precision")
        fun <T : Float?> real(prop: KMutableProperty1<E, T>) = Column(prop, "real")

        fun <T : String?> varchar(prop: KMutableProperty1<E, T>, size: Int = 60) = Column(prop, "varchar($size)")
        fun <T : String?> char(prop: KMutableProperty1<E, T>, size: Int = 60) = Column(prop, "char($size)")
        fun <T : String?> text(prop: KMutableProperty1<E, T>) = Column(prop, "text")

        fun <T : String?> json(prop: KMutableProperty1<E, T>) = Column(prop, "json")
        fun <T : String?> uuid(prop: KMutableProperty1<E, T>) = Column(prop, "uuid")


        fun <T : Date?> date(prop: KMutableProperty1<E, T>) = Column(prop, "date")
        fun <T : Time?> time(prop: KMutableProperty1<E, T>, withTimeZone: Boolean = false) =
            Column(prop, "time" + " with time zone".ifTrue(withTimeZone))

        fun <T : Timestamp?> timestamp(prop: KMutableProperty1<E, T>, withTimeZone: Boolean = false) =
            Column(prop, "timestamp" + " with time zone".ifTrue(withTimeZone))


        fun <T : Entity> reference(prop: KMutableProperty1<E, T?>, onDelete: Action = Action.SetDefault) =
            @Suppress("UNCHECKED_CAST")
            Reference(prop, onDelete)


        fun uniqueColumns(vararg props: KMutableProperty1<E, *>) {
            uniqueColumns.addAll(props.map { it.columnName })
        }
    }

    companion object {
        val tables = HashMap<KClass<*>, Table<*>>()

        @Suppress("UNCHECKED_CAST")
        inline operator fun <reified T : Entity> invoke() = tables[T::class] as Table<T>?
            ?: throw LoggerException("Table for class ${T::class.simpleName} was not initialized!")

        @Suppress("UNCHECKED_CAST")
        internal operator fun <T : Entity> get(kClass: KClass<T>) = tables[kClass] as Table<Entity>?
    }

}


