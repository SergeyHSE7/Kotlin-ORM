import kotlin.properties.ReadWriteProperty
import kotlin.reflect.*
import kotlin.reflect.full.memberProperties

abstract class Entity {
    abstract var id: Int
    val properties = this::class.properties

    fun toJson(): String = "{\n" +
                properties.joinToString(",\n") {
                "\t\"${it.name}\": ${if (it.returnValue(this) is String) "${it.returnValue(this)}" else it.returnValue(this)}"
            } +
            "\n}"

    fun compareValuesWith(other: Entity): Boolean =
        properties.all { it.name == "id" || it.returnValue(this) == it.returnValue(other) }
}

fun <T, V> KMutableProperty1<T, V>.returnValue(receiver: Any): Any? = getter.call(receiver)

val KClass<out Entity>.properties
    get() = memberProperties
        .filter { it.getter.visibility == KVisibility.PUBLIC && !listOf("properties", "table").contains(it.name) }
        .map { it as KMutableProperty1 }

