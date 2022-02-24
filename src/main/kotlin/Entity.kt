import statements.subQuery
import statements.update
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberProperties

abstract class Entity {
    abstract var id: Int
    internal val properties by lazy { this::class.properties }
    internal val table: Table<Entity> by lazy {
        Table[this::class] ?: throw LoggerException("Table for entity ${this::class} is not initialized")
    }

    fun compareValuesWith(other: Entity): Boolean =
        properties.all { prop ->
            prop.name == "id" || prop.getter.call(this).run {
                if (this is Entity) id == (prop.getter.call(other) as Entity).id else this == prop.getter.call(other)
            }
        }

    inline fun <reified E : Entity> oneToMany(keyProp: KMutableProperty1<E, *>) =
        ReadOnlyProperty<Any?, List<E>> { _, _ -> Table<E>().getAll { keyProp eq id } }

    inline fun <reified K : Entity, reified V : Entity, P : V?> manyToMany(
        keyProp: KMutableProperty1<K, *>,
        valueProp: KMutableProperty1<K, P>
    ) =
        ReadOnlyProperty<Any?, List<V>> { _, _ ->
            Table<V>().run { getAll { "$tableName.id" inColumn valueProp.subQuery { where { keyProp eq id } } } }
        }
}

@Suppress("UNCHECKED_CAST")
fun <E : Entity?> E.loadReferences(): E? = if (this == null) null else table.findById(id, loadReferences = true) as E?
internal fun <E : Entity?> E.loadReferencesIf(condition: Boolean): E? =
    if (condition) this.loadReferences() else this

@Suppress("UNCHECKED_CAST")
fun <E : Entity?> E.save(): E? = if (this == null) null else table.add(this) as E?

@Suppress("UNCHECKED_CAST")
fun <E : Entity?> List<E>.save(): List<E> =
    firstOrNull()?.let { it.table.add(this.filterNotNull()) as List<E> } ?: listOf()

inline fun <U : E?, reified E : Entity> U.update(vararg props: KMutableProperty1<E, *>, func: E.() -> Unit = {}) {
    if (this == null) return
    func(this)
    Table<E>().update(this, props.toList())
}

fun <E : Entity?> E.delete() {
    if (this != null) table.delete(this)
}

val KClass<out Entity>.properties
    get() = memberProperties.mapNotNull { it as? KMutableProperty1 }



