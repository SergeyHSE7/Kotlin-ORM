import utils.Case
import utils.transformCase
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.*
import kotlin.reflect.full.memberProperties

abstract class Entity {
    abstract var id: Int
    val properties by lazy { this::class.properties }

    fun compareValuesWith(other: Entity): Boolean =
        properties.all { prop ->
            prop.name == "id" || prop.returnValue(this).run {
                if (this is Entity) id == (prop.returnValue(other) as Entity).id else this == prop.returnValue(other)
            }
        }

    fun <E : Entity> oneToMany(refTable: Table<E>?, keyProp: KMutableProperty1<E, *>) =
        ReadOnlyProperty<Any?, List<E>> { thisRef, _ ->
            refTable?.findAll { keyProp eq (thisRef as Entity).id } ?: listOf()
        }

    fun <K : Entity, V : Entity?> manyToMany(
        refKeyTable: Table<K>?,
        keyProp: KMutableProperty1<K, *>,
        valueProp: KMutableProperty1<K, V>
    ) =
        ReadOnlyProperty<Any?, List<V>> { thisRef, _ ->
            refKeyTable?.findAll { keyProp eq (thisRef as Entity).id }?.map { valueProp.get(it) } ?: listOf()
        }
}


fun KProperty1<out Entity, *>.returnValue(receiver: Any): Any? = getter.call(receiver)
val KProperty1<*, *>.columnName: String
    get() = name.transformCase(Case.Camel, Case.Snake)

val KClass<out Entity>.properties
    get() = memberProperties.mapNotNull { it as? KMutableProperty1 }



