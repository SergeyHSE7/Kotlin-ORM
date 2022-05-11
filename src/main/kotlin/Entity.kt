@file:Suppress("UNCHECKED_CAST")

import statements.subQuery
import statements.update
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberProperties

/** Base class for all entities. */
abstract class Entity {
    /** Unique entity identification. */
    abstract var id: Int
    internal val properties by lazy { this::class.properties }
    internal val table: Table<Entity> by lazy {
        Table[this::class] ?: throw LoggerException("Table for entity ${this::class} is not initialized")
    }

    /** Compares all values (except id) with [other] entity. */
    fun compareValuesWith(other: Entity): Boolean =
        properties.all { prop ->
            prop.name == "id" || prop.getter.call(this).run {
                if (this is Entity) id == (prop.getter.call(other) as Entity).id else this == prop.getter.call(other)
            }
        }

    /** Property delegate for One-To-One connection via specified key-property. */
    inline fun <reified E:Entity> oneToOne(keyProp: KMutableProperty1<E, *>) =
        ReadOnlyProperty<Any?, E?> { _, _ -> Table<E>().first { keyProp eq id } }

    /** Property delegate for One-To-Many connection via specified key-property. */
    inline fun <reified E : Entity> oneToMany(keyProp: KMutableProperty1<E, *>) =
        ReadOnlyProperty<Any?, List<E>> { _, _ -> Table<E>().getAll { keyProp eq id } }

    /** Property delegate for Many-To-Many connection via specified key and value properties of the linking table. */
    inline fun <reified K : Entity, reified V : Entity, P : V?> manyToMany(
        keyProp: KMutableProperty1<K, *>,
        valueProp: KMutableProperty1<K, P>
    ) =
        ReadOnlyProperty<Any?, List<V>> { _, _ ->
            Table<V>().run { getAll { "$tableName.id" inColumn valueProp.subQuery { where { keyProp eq id } } } }
        }
}

/** Loads all references for current entity. */
fun <E : Entity?> E.loadReferences(): E? = if (this == null) null else table.findById(id, loadReferences = true) as E?
internal fun <E : Entity?> E.loadReferencesIf(condition: Boolean): E? =
    if (condition) this.loadReferences() else this

/** Adds the current entity to database. */
fun <E : Entity?> E.save(): E? = if (this == null) null else table.add(this) as E?

/** Adds current entities to database by its ids. */
fun <E : Entity?> List<E>.save(): List<E> =
    firstOrNull()?.let { it.table.add(this.filterNotNull()) as List<E> } ?: listOf()

/**
 * Updates the current entity in database by its id.
 *
 * @param[props] The properties of entity to update.
 * (if none are specified then all properties of the entity are updated).
 */
inline fun <U : E?, reified E : Entity> U.update(vararg props: KMutableProperty1<E, *>, func: E.() -> Unit = {}) {
    if (this == null) return
    func(this)
    Table<E>().update(this, props.toList())
}

/** Deletes the current entity in database by its id. */
fun <E : Entity?> E.delete() {
    if (this != null) table.delete(this)
}

internal val KClass<out Entity>.properties
    get() = memberProperties.mapNotNull { it as? KMutableProperty1 }



