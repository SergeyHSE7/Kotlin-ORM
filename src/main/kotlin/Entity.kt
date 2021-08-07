import statements.update
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberProperties

abstract class Entity {
    abstract var id: Int
    val properties by lazy { this::class.properties }
    val table: Table<Entity> by lazy {
        Table[this::class] ?: throw LoggerException("Table for entity ${this::class} is not initialized")
    }

    fun compareValuesWith(other: Entity): Boolean =
        properties.all { prop ->
            prop.name == "id" || prop.getter.call(this).run {
                if (this is Entity) id == (prop.getter.call(other) as Entity).id else this == prop.getter.call(other)
            }
        }

    inline fun <reified E : Entity> oneToMany(keyProp: KMutableProperty1<E, *>) =
        ReadOnlyProperty<Any?, List<E>> { thisRef, _ ->
            Table<E>().getAll { keyProp eq (thisRef as Entity).id }
        }

    inline fun <reified K : Entity, V : Entity?> manyToMany(
        keyProp: KMutableProperty1<K, *>,
        valueProp: KMutableProperty1<K, V>
    ) =
        ReadOnlyProperty<Any?, List<V>> { thisRef, _ ->
            Table<K>().getAll { keyProp eq (thisRef as Entity).id }.map { valueProp.get(it) }
        }
}

fun <E : Entity> E.loadReferences(): E? = table.findById(id, loadReferences = true) as E?
internal fun <E : Entity?> E.loadReferencesIf(condition: Boolean): E? =
    if (condition && this != null) this.loadReferences() else this

fun <E : Entity> E.save(): E? = table.add(this) as E?

fun <E : Entity> List<E>.save(): List<E> = firstOrNull()?.let { it.table.add(this) as List<E> } ?: listOf()

inline fun <reified E : Entity> E.update(vararg props: KMutableProperty1<E, *>, func: E.() -> Unit = {}) {
    func(this)
    Table<E>().update(this, props.toList())
}

fun <E : Entity> E.delete(): Unit = table.delete(this)


val KClass<out Entity>.properties
    get() = memberProperties.mapNotNull { it as? KMutableProperty1 }



