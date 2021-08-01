import org.tinylog.Logger
import statements.update
import utils.returnValue
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.memberProperties

abstract class Entity {
    abstract var id: Int
    val properties by lazy { this::class.properties }
    val table: Table<Entity>?
        get() = Table[this::class] ?: Logger.error { "Table for entity ${this::class} is not initialized" }.let { null }

    fun compareValuesWith(other: Entity): Boolean =
        properties.all { prop ->
            prop.name == "id" || prop.returnValue(this).run {
                if (this is Entity) id == (prop.returnValue(other) as Entity).id else this == prop.returnValue(other)
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

fun <E : Entity> E.loadReferences(): E? = table?.findById(id, loadReferences = true) as E?
internal fun <E : Entity?> E.loadReferencesIf(condition: Boolean): E? =
    if (condition && this != null) table?.get(id) as E? else this

fun <E : Entity> E.save(): E? = table?.add(this) as E?

fun <E : Entity> List<E>.save(): List<E> = firstOrNull()?.table?.let { it.add(this) as List<E> } ?: listOf()

inline fun <reified E : Entity> E.update(vararg props: KMutableProperty1<E, *>, func: E.() -> Unit = {}) {
    func(this)
    Table<E>().update(this, props.toList())
}

fun <E : Entity> E.delete(): Unit = table?.delete(this) ?: Unit


val KClass<out Entity>.properties
    get() = memberProperties.mapNotNull { it as? KMutableProperty1 }



