package utils

import Entity
import returnValue
import kotlin.reflect.KProperty1
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.memberProperties


@Target(AnnotationTarget.PROPERTY)
annotation class LazyProp


fun Entity.toJsonWithout(vararg excludeProps: KProperty1<out Entity, *>): String =
    this::class.memberProperties.excludeLazy().exclude(Entity::properties, *excludeProps)
        .joinToString(", ", "{", "}") { it.toJson(this) }

fun Entity.toJsonOnly(vararg serializeProps: KProperty1<out Entity, *>) = serializeProps.toList()
    .joinToString(", ", "{", "}") { it.toJson(this) }

fun Entity.toJson(all: Boolean = false): String = (if (all) this::class.memberProperties else properties.excludeLazy())
    .exclude(Entity::properties)
    .joinToString(", ", "{", "}") { it.toJson(this) }


private fun KProperty1<out Entity, *>.toJson(entity: Entity) = "\"${name}\": ${returnValue(entity).toJson()}"

private fun Any?.toJson(): String = when (this) {
    is String -> "\"$this\""
    is Iterable<Any?> -> joinToString(",", "[", "]") { it.toJson() }
    is Entity -> toJson()
    else -> toString()
}

private fun Iterable<KProperty1<out Entity, *>>.excludeLazy() =
    toMutableList().apply {
        removeAll { prop -> prop.hasAnnotation<LazyProp>() }
    }

private fun Iterable<KProperty1<out Entity, *>>.exclude(vararg excludeProps: KProperty1<out Entity, *>) =
    toMutableList().apply {
        removeAll { prop -> excludeProps.any { it.name == prop.name } }
    }
