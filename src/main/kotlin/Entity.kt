import statements.select
import utils.transformCase
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.*
import kotlin.reflect.full.memberProperties

abstract class Entity {
    abstract var id: Int
    val properties = this::class.properties

    fun toJson(): String = "{\n" +
            properties.joinToString(",\n") {
                "\t\"${it.name}\": ${
                    if (it.returnValue(this) is String) "${it.returnValue(this)}"
                    else it.returnValue(this)
                }"
            } + "\n}"

    fun compareValuesWith(other: Entity): Boolean =
        properties.all { it.name == "id" || it.returnValue(this) == it.returnValue(other) }

    fun <E : Entity> oneToMany(refTable: Table<E>?, keyProp: KMutableProperty1<E, *>) =
        ReadOnlyProperty<Any?, List<E>> { thisRef, _ ->
            refTable?.all { keyProp eq (thisRef as Entity).id } ?: listOf()
        }

    fun <K : Entity, V : Entity?> manyToMany(
        refKeyTable: Table<K>?,
        keyProp: KMutableProperty1<K, *>,
        valueProp: KMutableProperty1<K, V>
    ) =
        ReadOnlyProperty<Any?, List<V>> { thisRef, _ ->
            refKeyTable?.select(valueProp)
                ?.where { keyProp eq (thisRef as Entity).id }
                ?.getEntities()?.map { valueProp.get(it) }
                ?: listOf()
        }
}

fun <T, V> KMutableProperty1<T, V>.returnValue(receiver: Any): Any? = getter.call(receiver)

val KClass<out Entity>.properties
    get() = memberProperties
        .filter { it.getter.visibility == KVisibility.PUBLIC && !listOf("properties", "table").contains(it.name) }
        .mapNotNull { it as? KMutableProperty1 }

