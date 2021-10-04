import databases.Database
import sql_type_functions.SqlList
import sql_type_functions.SqlNumber
import statements.*
import utils.CacheMap
import utils.Case
import utils.transformCase
import kotlin.collections.set
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.createInstance


@Suppress("UNCHECKED_CAST")
inline fun <reified E : Entity, DB : Database> table(noinline columnsBody: DB.() -> Unit = {}): Table<E> =
    Table(E::class, columnsBody as Database.() -> Unit)


enum class Action {
    Cascade, SetNull, SetDefault
}

class Table<E : Entity>(
    val entityClass: KClass<E>,
    private val columnsBody: Database.() -> Unit
) {
    val cache = CacheMap<E>(Config.maxCacheSize)
    var tableName = entityClass.simpleName!!.transformCase(Case.Pascal, Case.Snake, true)
    val defaultEntities by lazy { defaultEntitiesMethod() }
    var defaultEntitiesMethod: () -> List<E> = { listOf() }

    internal val columns = mutableListOf<Column<E, *>>()
    internal val references = mutableListOf<Reference<E, *>>()
    val uniqueProps = mutableSetOf<KMutableProperty1<E, *>>()
    internal val referencesAddMethods: MutableSet<() -> Unit> = mutableSetOf()
    internal val defaultEntity: E = entityClass.createInstance()
    val checkConditions = mutableListOf<WhereCondition>()


    val size: Int
        get() = select().aggregateColumn(SqlList("*").count()).getResultSet().apply { next() }.getInt(1)

    fun isEmpty() = size == 0

    fun createTable() {
        if (tableName in database.reservedKeyWords)
            throw LoggerException("\"$tableName\" is a reserved SQL keyword!")

        if (Config.refreshTables) dropTable()
        tables[entityClass] = this

        database.columnsBody()
        @Suppress("UNCHECKED_CAST")
        entityClass.properties.forEach { column(it as KMutableProperty1<E, Any?>) }

        create()
    }

    fun dropTable() = drop().also {
        tables.remove(entityClass)
        columns.clear()
        uniqueProps.clear()
        checkConditions.clear()
        references.clear()
        referencesAddMethods.clear()
        cache.clear()
    }

    fun clearTable() = delete()


    operator fun plusAssign(entity: E) {
        add(entity)
    }

    fun add(entity: E): E? = insert(entity).getId()?.let { id -> entity.apply { this.id = id } }
        .loadReferencesIf(Config.alwaysLoadReferencesWhenAddingEntity)

    fun add(vararg entities: E): List<E> = add(entities.toList())
    fun add(entities: List<E>): List<E> = if (entities.isEmpty()) listOf() else insert(entities).getIds()
        .let { idList -> entities.apply { idList.forEachIndexed { index, id -> entities[index].id = id } } }
        .mapNotNull { it.loadReferencesIf(Config.alwaysLoadReferencesWhenAddingEntity) }


    operator fun get(id: Int): E? = findById(id)
    operator fun contains(entity: E): Boolean = select().apply {
        entity.properties.forEach { prop ->
            if (prop.name != "id")
                where { this@Table.entityProperty(prop) eq prop.getter.call(entity) }
        }
    }.getResultSet().next()

    fun containsAll(entities: Iterable<E>): Boolean = entities.all(::contains)
    fun containsAny(entities: Iterable<E>): Boolean = entities.any(::contains)
    operator fun contains(entities: Iterable<E>): Boolean = containsAll(entities)

    fun getAll(loadReferences: Boolean = Config.loadReferencesByDefault, condition: WhereCondition? = null): List<E> =
        selectAll().where(condition).setLazy(!loadReferences).getEntities()

    fun findById(id: Int, loadReferences: Boolean = Config.loadReferencesByDefault): E? =
        cache[id, loadReferences] ?: first(loadReferences) { this@Table.entityProperty("id") eq id }

    fun findIdOf(condition: WhereCondition): Int? = SelectStatement(this, listOf("id")).where(condition).getEntity()?.id

    fun first(loadReferences: Boolean = Config.loadReferencesByDefault, condition: WhereCondition? = null): E? =
        selectAll().where(condition).limit(1).setLazy(!loadReferences).getEntity()

    fun last(loadReferences: Boolean = Config.loadReferencesByDefault, condition: WhereCondition? = null): E? =
        selectAll().where(condition).orderByDescending().limit(1).setLazy(!loadReferences).getEntity()

    fun take(n: Int, loadReferences: Boolean = Config.loadReferencesByDefault) =
        selectAll().limit(n).setLazy(!loadReferences).getEntities()

    fun takeLast(n: Int, loadReferences: Boolean = Config.loadReferencesByDefault) =
        selectAll().orderByDescending().limit(n).setLazy(!loadReferences).getEntities()

    fun count(condition: WhereCondition): Int = select().where(condition).size


    operator fun set(id: Int, entity: E) = update(entity) { this.id = id }
    inline fun update(entity: E, vararg props: KMutableProperty1<E, *>, func: E.() -> Unit = {}) {
        func(entity)
        update(entity, props.toList())
    }

    fun update(entities: List<E>) = entities.forEach { update(it) }

    operator fun minusAssign(entity: E) = delete(entity)
    fun delete(entity: E): Unit = deleteById(entity.id)

    fun <T> getColumn(prop: KMutableProperty1<E, T>): List<T> = select(prop).getEntities().map(prop)

    fun <T : Number?> aggregateBy(prop: KMutableProperty1<E, T>, func: SqlList.() -> SqlNumber): Int =
        select().aggregateColumn(func(SqlList(prop.column.fullName))).getResultSet().apply { next() }.getInt(1)

    fun all(condition: WhereCondition): Boolean = select().where(condition).lazy().size == size
    fun any(condition: WhereCondition): Boolean = select().where(condition).limit(1).lazy().getResultSet().next()
    fun none(condition: WhereCondition): Boolean = !any(condition)


    companion object {
        val tables = HashMap<KClass<*>, Table<*>>()

        @Suppress("UNCHECKED_CAST")
        inline operator fun <reified T : Entity> invoke() = tables[T::class] as Table<T>?
            ?: throw LoggerException("Table for class ${T::class.simpleName} was not initialized!")

        @Suppress("UNCHECKED_CAST")
        internal operator fun <T : Entity> get(kClass: KClass<T>) = tables[kClass] as Table<Entity>?
    }

}


