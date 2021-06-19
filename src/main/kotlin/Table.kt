import statements.*
import utils.CacheMap
import utils.Case
import utils.ifTrue
import utils.transformCase
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1

/*inline fun <reified E : Entity> table(
    refresh: Boolean = false,
    noinline columnsBody: Table<E>.() -> Unit = {}
): Table<E> = object : Table<E>(E::class, refresh, columnsBody) {}*/

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
            serial(entityClass.properties.first { it.name == "id" } as KMutableProperty1<E, Int>).primaryKey()
            columnsBody()
        }


        if (refresh) dropTable()
        createTable()
        addAll(defaultEntities)
    }

    fun defaultEntities(vararg entities: E) = defaultEntities(entities.toList())
    fun defaultEntities(entities: List<E>) = this.apply { addAll(entities) }

    fun createTable() = create()
    fun dropTable() = drop()
    fun clearTable() = delete()


    operator fun plusAssign(entity: E) {
        add(entity)
    }

    fun add(entity: E): Int? = insert(entity).getId()//?.apply { cache[this] = entity }
    fun addAll(entities: List<E>): List<Int> = insert(entities).getIds()//.apply {forEach { cache[this] = entity }
    fun addAll(vararg entities: E): List<Int> = addAll(entities.toList())

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
    fun findById(id: Int, loadReferences: Boolean = true): E? = cache[id, loadReferences] ?: find(loadReferences) { Entity::id eq id }
    fun findIdOf(condition: WhereCondition): Int? = select("id").where(condition).getEntity()?.id

    operator fun set(id: Int, entity: E) = update(entity) { this.id = id }
    fun update(entity: E, func: E.() -> Unit) {
        func(entity)
        update(entity)
    }

    operator fun minusAssign(entity: E) = delete(entity)
    fun delete(entity: E) = deleteById(entity.id)

    fun <T> getValuesOfColumn(prop: KMutableProperty1<E, T>): List<T> = select(prop).getEntities().map(prop)

    inner class Reference<R : Entity>(property: KMutableProperty1<E, R?>, refTable: Table<out Entity>, val onDelete: Action) :
        Column<R?>(property, "integer", refTable) {

        override fun attributesToSql(): String =
            super.attributesToSql() + " REFERENCES ${refTable!!.tableName} (id) ON DELETE " +
                    onDelete.name.transformCase(Case.Pascal, Case.Normal).uppercase()

    }

    enum class Action {
        Cascade, SetNull, SetDefault
    }

    open inner class Column<T>(
        val property: KMutableProperty1<E, T>,
        private val sqlType: String,
        val refTable: Table<out Entity>? = null
    ) {
        var name: String = property.columnName
        val entityClass = this@Table.entityClass
        private var defaultValue: T? = null
        private var isNotNull = false
        private var isUnique = false
        private var isPrimaryKey = false

        init {
            columns.add(this)
            println("$name - ${property.returnType}")
            if (!property.returnType.isMarkedNullable)
                isNotNull = true

            if (property.returnType.isMarkedNullable == isNotNull)
                println("Несоответствие типов!")
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
        fun <T> serial(prop: KMutableProperty1<E, T>) = Column(prop, "serial")
        fun <T> integer(prop: KMutableProperty1<E, T>) = Column(prop, "integer")
        fun <T> real(prop: KMutableProperty1<E, T>) = Column(prop, "real")
        fun <T> varchar(prop: KMutableProperty1<E, T>, size: Int = 60) = Column(prop, "varchar($size)")

        fun <T : Entity> reference(prop: KMutableProperty1<E, T?>, refTable: Table<T>, onDelete: Action = Action.SetDefault) =
            Reference(prop, refTable, onDelete)

        fun uniqueColumns(vararg props: KMutableProperty1<E, *>) {
            uniqueColumns.addAll(props.map { it.columnName })
        }
    }

}


