import statements.*
import utils.Case
import utils.ifTrue
import utils.transformCase
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1

inline fun <reified E : Entity> table(
    refresh: Boolean = false,
    noinline columnsBody: Table<E>.() -> Unit = {}
): Table<E> = Table(E::class, refresh, columnsBody)

open class Table<E : Entity>(
    val entityClass: KClass<E>,
    refresh: Boolean = false,
    columnsBody: Table<E>.() -> Unit = {},
    defaultEntities: List<E> = listOf()
) {
    var tableName = entityClass.simpleName!!.transformCase(Case.Pascal, Case.Snake, true)
    val columns = mutableListOf<Column<*>>()

    init {
        serial(entityClass.properties.first { it.name == "id" } as KMutableProperty1<E, Int>).primaryKey()
        columnsBody()


        if (refresh) dropTable()
        createTable()
        addAll(defaultEntities)
    }

    fun defaultEntities(vararg entities: E) = defaultEntities(entities.toList())
    fun defaultEntities(entities: List<E>) = this.apply { addAll(entities) }

    fun createTable() = create()
    fun dropTable() = drop()
    fun clearTable() = delete()


    fun add(entity: E): Int? = insert(entity).getId()
    fun addAll(entities: List<E>): List<Int> = insert(entities).getIds()
    fun addAll(vararg entities: E): List<Int> = insert(entities.toList()).getIds()

    fun all(condition: WhereCondition? = null): List<E> = selectAll().where(condition).getEntities()
    fun find(condition: WhereCondition): E? = selectAll().where(condition).getEntity()
    fun findById(id: Int): E? = find { Entity::id eq id }
    fun findIdOf(condition: WhereCondition): Int? = select("id").where(condition).getEntity()?.id

    fun update(entity: E, func: E.() -> Unit) {
        func(entity)
        update(entity)
    }

    fun deleteById(id: Int) = delete { Entity::id eq id }
    fun delete(entity: E) = deleteById(entity.id)


    fun <T> getValuesOfColumn(prop: KMutableProperty1<E, T>): List<T> = select(prop).getEntities().map(prop)

    inner class Column<T>(val property: KMutableProperty1<E, T>, private val sqlType: String, val refTable: Table<out Entity>? = null) {
        var name: String = property.name.transformCase(Case.Camel, Case.Snake)
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

        private fun attributesToSql(): String = "PRIMARY KEY ".ifTrue(isPrimaryKey) +
                "NOT NULL ".ifTrue(isNotNull) +
                "UNIQUE ".ifTrue(isUnique) +
                "DEFAULT $defaultValue".ifTrue(defaultValue != null && defaultValue !is String) +
                "DEFAULT '$defaultValue'".ifTrue(defaultValue != null && defaultValue is String)


        fun toSql(nameLength: Int = name.length): String =
            "${name.padEnd(nameLength)} ${sqlType.uppercase()} ${attributesToSql()}".trim()
    }

    private fun <T> serial(prop: KMutableProperty1<E, T>) = Column(prop, "serial")
    fun <T> integer(prop: KMutableProperty1<E, T>) = Column(prop, "integer")
    fun <T> real(prop: KMutableProperty1<E, T>) = Column(prop, "real")
    fun <T> varchar(prop: KMutableProperty1<E, T>, size: Int = 60) = Column(prop, "varchar($size)")
    fun <T: Entity> reference(prop: KMutableProperty1<E, T?>, refTable: Table<T>) = Column(prop, "integer", refTable)

}


