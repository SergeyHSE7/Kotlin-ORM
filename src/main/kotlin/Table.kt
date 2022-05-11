@file:Suppress("UNCHECKED_CAST")

import databases.Database
import sql_type_functions.SqlList
import sql_type_functions.SqlNumber
import statements.*
import utils.CacheMap
import utils.Case
import utils.map
import utils.transformCase
import kotlin.collections.set
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.createInstance


/**
 * Create the table for specified entity.
 *
 * @param[columnsBody] A function applied to the table to set constraints and/or additional rules for columns.
 */
inline fun <reified E : Entity, DB : Database> table(
    tableName: String? = null,
    noinline columnsBody: DB.() -> Unit = {}
): Table<E> =
    Table(tableName, E::class, columnsBody as Database.() -> Unit)


/** Represents a table for specified entity class. */
class Table<E : Entity>(
    tableName: String?,
    val entityClass: KClass<E>,
    private val columnsBody: Database.() -> Unit
) {
    /** Name of the created table. */
    val tableName = tableName ?: entityClass.simpleName!!.transformCase(Case.Pascal, Case.Snake, true)

    /** Entities that are always added after the table creation. */
    val defaultEntities by lazy { defaultEntitiesMethod() }
    var defaultEntitiesMethod: () -> List<E> = { listOf() }
    val uniqueProps = mutableSetOf<KMutableProperty1<E, *>>()
    val checkConditions = mutableListOf<WhereCondition>()

    internal val cache = CacheMap<E>(Config.maxCacheSize)
    internal val columns = mutableSetOf<Column<E, *>>()
    internal val references = mutableSetOf<Reference<E, *>>()
    internal val referencesAddMethods: MutableSet<() -> Unit> = mutableSetOf()
    internal val defaultEntity: E = entityClass.createInstance()

    /** Amount of entries in table. */
    val size: Int
        get() = select().aggregateColumn(SqlList("*").count()).getSingleValue()

    /** Check if table has no entries. */
    val isEmpty
        get() = size == 0

    /**
     * Getting table entries as sequence.
     *
     * Batch retrieval of entries from a huge table (with huge amount of entries).
     *
     * @param[windowSize] The number of entities receiving per request.
     * @return Sequence of entities.
     */
    fun asSequence(windowSize: Int = Config.sequenceWindowSize): Sequence<E> = sequence {
        var iterator: Iterator<E> = take(windowSize).iterator()
        var count = 0

        while (iterator.hasNext()) {
            yieldAll(iterator)
            iterator = selectAll().limit(windowSize).offset(++count * windowSize).getEntities().iterator()
        }
    }

    // -------------------------------- Table operations ---------------------------------------------------------------

    internal fun initTable() {
        if (tableName in database.reservedKeyWords)
            throw LoggerException("\"$tableName\" is a reserved SQL keyword!")

        tables[entityClass] = this
        database.columnsBody()
        entityClass.properties.forEach { column(it as KMutableProperty1<E, Any?>) }
    }

    internal fun dropTable() = drop().also {
        tables.remove(entityClass)
        columns.clear()
        uniqueProps.clear()
        checkConditions.clear()
        references.clear()
        referencesAddMethods.clear()
        cache.clear()
    }

    /** Deletes all entries in the database table. */
    fun clearTable() = delete()


    // -------------------------------- 'Add' methods (INSERT) ---------------------------------------------------------

    /** Inserts [entity] to the database table. */
    operator fun plusAssign(entity: E) {
        add(entity)
    }

    /** Inserts [entity] to the database table. */
    fun add(entity: E): E? = insert(entity).getId()?.let { id -> entity.apply { this.id = id } }
        .loadReferencesIf(Config.alwaysLoadReferencesWhenAddingEntity)

    /** Inserts [entities] to the database table. */
    fun add(vararg entities: E): List<E> = add(entities.toList())

    /** Inserts [entities] to the database table. */
    fun add(entities: List<E>): List<E> = if (entities.isEmpty()) listOf() else insert(entities).getIds()
        .let { idList -> entities.apply { idList.forEachIndexed { index, id -> entities[index].id = id } } }
        .mapNotNull { it.loadReferencesIf(Config.alwaysLoadReferencesWhenAddingEntity) }


    // -------------------------------- 'Get' methods (SELECT) ---------------------------------------------------------

    /** Gets entity by the specified [id]. */
    operator fun get(id: Int): E? = findById(id)

    /** Gets values of column associated with specified [property]. */
    operator fun <T : Any?> get(property: KMutableProperty1<E, T>): List<T> = getColumn(property)

    /** Gets values of column associated with specified [property] that satisfy the [condition] (optional). */
    fun <T : Any, P : T?> getColumn(property: KMutableProperty1<E, P>, condition: WhereCondition? = null): List<T> =
        select(property).where(condition).getEntities().mapNotNull(property)

    /** Gets all entities that satisfy the [condition] (optional). */
    fun getAll(loadReferences: Boolean = Config.loadReferencesByDefault, condition: WhereCondition? = null): List<E> =
        selectAll().where(condition).setLazy(!loadReferences).getEntities()

    /** Gets entity by the specified [id]. */
    fun findById(id: Int, loadReferences: Boolean = Config.loadReferencesByDefault): E? =
        cache[id, loadReferences] ?: first(loadReferences) { "$tableName.id" eq id }

    /** Gets id of the first entity that satisfy the [condition]. */
    fun findIdOf(condition: WhereCondition): Int? = SelectStatement(this, listOf("id")).where(condition).getEntity()?.id

    /** Gets the first entity that satisfy the [condition] (optional). If none is found then returns null. */
    fun first(loadReferences: Boolean = Config.loadReferencesByDefault, condition: WhereCondition? = null): E? =
        selectAll().where(condition).limit(1).setLazy(!loadReferences).getEntity()

    /** Gets the first entity that satisfy the [condition] (optional). If none is found then returns specified [defaultValue]. */
    fun firstOrDefault(
        defaultValue: E = entityClass.createInstance(),
        loadReferences: Boolean = Config.loadReferencesByDefault,
        condition: WhereCondition? = null
    ): E = first(loadReferences, condition) ?: defaultValue

    /** Gets the last entity that satisfy the [condition] (optional). If none is found then returns null. */
    fun last(loadReferences: Boolean = Config.loadReferencesByDefault, condition: WhereCondition? = null): E? =
        selectAll().where(condition).orderByDescending().limit(1).setLazy(!loadReferences).getEntity()

    /** Gets the last entity that satisfy the [condition] (optional). If none is found then returns specified [defaultValue]. */
    fun lastOrDefault(
        defaultValue: E = entityClass.createInstance(),
        loadReferences: Boolean = Config.loadReferencesByDefault,
        condition: WhereCondition? = null
    ): E = last(loadReferences, condition) ?: defaultValue

    /** Gets the entity with the highest value of specified numeric [property]. */
    fun <P : Number?> maxBy(
        property: KMutableProperty1<E, P>,
        loadReferences: Boolean = Config.loadReferencesByDefault
    ): E? =
        selectAll().orderByDescending(property).limit(1).setLazy(!loadReferences).getEntity()

    /** Gets the entity with the lowest value of specified numeric [property]. */
    fun <P : Number?> minBy(
        property: KMutableProperty1<E, P>,
        loadReferences: Boolean = Config.loadReferencesByDefault
    ): E? =
        selectAll().orderBy(property).limit(1).setLazy(!loadReferences).getEntity()

    /** Gets the first [n] entities that satisfy the [condition] (optional). */
    fun take(n: Int, loadReferences: Boolean = Config.loadReferencesByDefault, condition: WhereCondition? = null) =
        selectAll().where(condition).limit(n).setLazy(!loadReferences).getEntities()

    /** Gets the last [n] entities that satisfy the [condition] (optional). */
    fun takeLast(n: Int, loadReferences: Boolean = Config.loadReferencesByDefault, condition: WhereCondition? = null) =
        selectAll().where(condition).orderByDescending().limit(n).setLazy(!loadReferences).getEntities()

    /** Gets the amount of entities that satisfy the [condition]. */
    fun count(condition: WhereCondition): Int = select().aggregateColumn(SqlList("*").count()).where(condition)
        .getSingleValue()

    /** Gets the amount of entities which specified [property] is not equal null. */
    fun countNotNull(property: KMutableProperty1<E, *>): Int =
        select().aggregateColumn(SqlList(property.column.fullName).count()).getSingleValue()


    /** Checks if the table contains the specified [entity] (all fields are compared except id). */
    operator fun contains(entity: E): Boolean = select().apply {
        entity.properties.forEach { property ->
            if (property.name != "id")
                where { property eq property.getter.call(entity) }
        }
    }.getResultSet().next()

    /** Checks if the table contains all the specified [entities] (all fields are compared except id). */
    operator fun contains(entities: Iterable<E>): Boolean = containsAll(entities)

    /** Checks if the table contains all the specified [entities] (all fields are compared except id). */
    fun containsAll(entities: Iterable<E>): Boolean = entities.all(::contains)

    /** Checks if the table contains any of the specified [entities] (all fields are compared except id). */
    fun containsAny(entities: Iterable<E>): Boolean = entities.any(::contains)

    /** Checks if all table entries are satisfied to specified [condition]. */
    fun all(condition: WhereCondition): Boolean = !any { !condition(this) }

    /** Checks if any table entry is satisfied to specified [condition]. */
    fun any(condition: WhereCondition): Boolean = select().where(condition).limit(1).lazy().getResultSet().next()

    /** Checks if none of the table entries is satisfied to specified [condition]. */
    fun none(condition: WhereCondition): Boolean = !any(condition)


    /** Aggregates the values of column associated with the specified numeric [property] via [func]. */
    fun <T : Number?> aggregateBy(property: KMutableProperty1<E, T>, func: SqlList.() -> SqlNumber): Int =
        select().aggregateBy(property, func).getSingleValue()

    /** Returns the number of entries for each unique value of column associated with specified property.
     * Also, you can specify parameter [filter] to filter entries at the level of grouped data. */
    fun <G : Any?> groupCounts(
        groupBy: KMutableProperty1<E, G>,
        filter: (WhereStatement.(SqlNumber) -> Expression)? = null
    ): Map<G, Int> {
        val map = mutableMapOf<G, Int>()
        select().groupAggregate(groupBy, SqlList("*").count(), filter)
            .getResultSet().map { map[groupBy.column.getValue(this, 2) as G] = getInt(1) }
        return map
    }

    /**
     * Returns the map where keys are unique values of specified property and
     * values are results of aggregation another property for each group.
     *
     * @param[groupBy] The property associated with the column by which the entries are grouped.
     * @param[aggregateBy] The numeric property which values will be aggregated.
     * @param[aggregateFunc] Function via which the values will be aggregated.
     * @param[filter] Function to filter entries at the level of grouped data.
     */
    fun <T : Number?, G : Any?> groupAggregate(
        groupBy: KMutableProperty1<E, G>,
        aggregateBy: KMutableProperty1<E, T>,
        aggregateFunc: SqlList.() -> SqlNumber,
        filter: (WhereStatement.(SqlNumber) -> Expression)? = null
    ): Map<G, Int> {
        val map = mutableMapOf<G, Int>()
        select().groupAggregate(groupBy, aggregateBy, aggregateFunc, filter)
            .getResultSet().map { map[groupBy.column.getValue(this, 2) as G] = getInt(1) }
        return map
    }


    // -------------------------------- 'Update' methods (UPDATE) ------------------------------------------------------

    /** Update data of the [entity] by specified [id]. */
    operator fun set(id: Int, entity: E) = update(entity) { this.id = id }

    /**
     * Update the data of specified [entity] in database.
     *
     * @param[entity] The entity to update.
     * @param[props] The properties of entity to update.
     * (if none are specified then all properties of the entity are updated).
     * @param[func] Lambda function where you can edit your entity's properties before updating them.
     */
    inline fun update(entity: E, vararg props: KMutableProperty1<E, *>, func: E.() -> Unit = {}) {
        func(entity)
        update(entity, props.toList())
    }

    /** Update the data of specified [entities] in database. */
    fun update(entities: List<E>) = entities.forEach { update(it) }


    // -------------------------------- 'Delete' methods (DELETE) ------------------------------------------------------

    /** Deletes the specified entity by its id. */
    operator fun minusAssign(entity: E) = delete(entity)

    /** Deletes the specified entity by its id. */
    fun delete(entity: E): Unit = deleteById(entity.id)


    companion object {
        /** Map of entity classes and table instances. */
        val tables = HashMap<KClass<*>, Table<*>>()

        /** Returns the table instance for specified entity class. */
        inline operator fun <reified T : Entity> invoke() = tables[T::class] as Table<T>?
            ?: throw LoggerException("Table for class ${T::class.simpleName} was not initialized!")

        internal operator fun <T : Entity> get(kClass: KClass<T>) = tables[kClass] as Table<Entity>?
    }

}


