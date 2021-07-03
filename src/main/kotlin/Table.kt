import org.tinylog.Logger
import statements.*
import utils.*
import java.math.BigDecimal
import java.sql.Date
import java.sql.Time
import java.sql.Timestamp
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1


abstract class Table<E : Entity>(
    val entityClass: KClass<E>,
    refresh: Boolean = false,
    columnsBody: Table<E>.CreateMethods.() -> Unit = {},
    defaultEntities: List<E> = listOf()
) {
    val cache = CacheMap<E>(10)
    var tableName = entityClass.simpleName!!.transformCase(Case.Pascal, Case.Snake, true)
    val columns = mutableListOf<Column<*>>()
    val uniqueColumns = mutableSetOf<String>()

    val size: Int
        get() = database.connection.createStatement().executeQuery("SELECT COUNT(*) FROM $tableName")
            .apply { next() }.getInt(1)

    fun isEmpty() = size == 0

    init {
        with(CreateMethods()) {
            @Suppress("UNCHECKED_CAST")
            serial(entityClass.properties.first { it.name == "id" } as KMutableProperty1<E, Int>).primaryKey()
            columnsBody()
        }

        if (refresh) dropTable()
        createTable()
        add(defaultEntities)
    }

    fun defaultEntities(vararg entities: E) = defaultEntities(entities.toList())
    fun defaultEntities(entities: List<E>) = this.apply { add(entities) }

    fun createTable() = create()
    fun dropTable() = drop()
    fun clearTable() = delete()


    operator fun plusAssign(entity: E) {
        add(entity)
    }

    fun add(entity: E): Int? = insert(entity).getId()?.apply { entity.id = this }
    fun add(vararg entities: E): List<Int> = add(entities.toList())
    fun add(entities: List<E>): List<Int> = insert(entities).getIds()
        .apply { forEachIndexed { index, id -> entities[index].id = id } }


    operator fun get(id: Int): E? = findById(id)
    operator fun contains(entity: E): Boolean = selectAll().apply {
        entity.properties.forEach { prop ->
            if (prop.name != "id")
                where { prop eq prop.returnValue(entity) }
        }
    }.getEntity() != null

    fun all(loadReferences: Boolean = true): List<E> = selectAll().apply { if (!loadReferences) lazy() }.getEntities()
    fun findAll(loadReferences: Boolean = true, condition: WhereCondition): List<E> = selectAll().where(condition)
        .apply { if (!loadReferences) lazy() }.getEntities()

    fun find(loadReferences: Boolean = true, condition: WhereCondition): E? = selectAll().where(condition)
        .apply { if (!loadReferences) lazy() }.getEntity()

    fun findById(id: Int, loadReferences: Boolean = true): E? =
        cache[id, loadReferences] ?: find(loadReferences) { Entity::id eq id }

    fun findIdOf(condition: WhereCondition): Int? = select("id").where(condition).getEntity()?.id

    operator fun set(id: Int, entity: E) = update(entity) { this.id = id }
    inline fun update(entity: E, func: E.() -> Unit) {
        func(entity)
        update(entity)
    }

    fun update(entities: List<E>) = entities.forEach { update(it) }

    operator fun minusAssign(entity: E) = delete(entity)
    fun delete(entity: E) = deleteById(entity.id)

    fun <T> getValuesOfColumn(prop: KMutableProperty1<E, T>): List<T> = select(prop).getEntities().map(prop)

    inner class Reference<R : Entity>(
        property: KMutableProperty1<E, R?>,
        refTable: Table<Entity>,
        private val onDelete: Action
    ) :
        Column<R?>(property, "integer", refTable) {

        override fun attributesToSql(): String =
            super.attributesToSql() + "REFERENCES ${refTable!!.tableName} (id) ON DELETE " +
                    onDelete.name.transformCase(Case.Pascal, Case.Normal).uppercase()

    }

    enum class Action {
        Cascade, SetNull, SetDefault
    }

    open inner class Column<T>(
        val property: KMutableProperty1<E, T>,
        val sqlType: String,
        val refTable: Table<Entity>? = null
    ) {
        val name: String = property.columnName
        private var defaultValue: T? = null
        private var isNotNull = false
        private var isUnique = false
        private var isPrimaryKey = false

        init {
            columns.add(this)
            if (!property.returnType.isMarkedNullable)
                isNotNull = true

            if (property.returnType.isMarkedNullable && isNotNull)
                Logger.warn { "Несоответствие типов!" }
        }

        fun notNull() = this.also { isNotNull = true }
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


        fun <T : Entity> reference(
            prop: KMutableProperty1<E, T?>,
            refTable: Table<T>,
            onDelete: Action = Action.SetDefault
        ) =
            Reference(prop, refTable as Table<Entity>, onDelete)

        fun uniqueColumns(vararg props: KMutableProperty1<E, *>) {
            uniqueColumns.addAll(props.map { it.columnName })
        }
    }

}


