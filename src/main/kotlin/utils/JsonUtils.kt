package utils

import Entity
import kotlin.reflect.KProperty1
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties


@Target(AnnotationTarget.PROPERTY)
annotation class Hidden

@Target(AnnotationTarget.PROPERTY)
annotation class FetchLazy


fun Entity.toJsonWithout(vararg excludeProps: KProperty1<out Entity, *>): String =
    this::class.memberProperties.excludeHidden().exclude(Entity::properties, *excludeProps)
        .joinToString(", ", "{", "}") { it.toJson(this) }

fun Entity.toJsonOnly(vararg serializeProps: KProperty1<out Entity, *>) = serializeProps.toList()
    .joinToString(", ", "{", "}") { it.toJson(this) }

fun Entity.toJson(all: Boolean = false): String =
    (if (all) this::class.memberProperties else properties.excludeHidden())
        .exclude(Entity::properties)
        .joinToString(", ", "{", "}") { it.toJson(this) }


private fun KProperty1<out Entity, *>.toJson(entity: Entity) = "\"${name}\": " +
        if (hasAnnotation<FetchLazy>() && returnValue(entity) != null)
            "{ \"id\": ${(returnValue(entity) as Entity).id} }"
        else returnValue(entity).toJson()

private fun Any?.toJson(): String = when (this) {
    is String -> "\"$this\""
    is Iterable<Any?> -> joinToString(",", "[", "]") { it.toJson() }
    is Entity -> toJson()
    else -> toString()
}

private fun Iterable<KProperty1<out Entity, *>>.excludeHidden() =
    toMutableList().apply {
        removeAll { prop -> prop.hasAnnotation<Hidden>() }
    }

private fun Iterable<KProperty1<out Entity, *>>.exclude(vararg excludeProps: KProperty1<out Entity, *>) =
    toMutableList().apply {
        removeAll { prop -> excludeProps.any { it.name == prop.name } }
    }
