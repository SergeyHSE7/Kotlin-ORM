package utils

import Entity
import json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.reflect.KProperty1

/** Encodes object to json. */
inline fun <reified T> T.toJson(): String = json.encodeToString(this)

/** Encodes entities with only specified properties to json. */
inline fun <reified E : Entity?> Collection<E>.toJsonOnly(vararg serializeProps: KProperty1<out Entity, *>): String {
    val serializeKeys = serializeProps.map { it.name }
    val jsonObjects = map { JsonObject(json.encodeToJsonElement(it).jsonObject.filterKeys { key -> key in serializeKeys }) }
    return json.encodeToString(jsonObjects)
}

/** Encodes entity with only specified properties to json. */
inline fun <reified E : Entity?> E.toJsonOnly(vararg serializeProps: KProperty1<out Entity, *>): String {
    val serializeKeys = serializeProps.map { it.name }
    val jsonObject = JsonObject(json.encodeToJsonElement(this).jsonObject.filterKeys { it in serializeKeys })
    return json.encodeToString(jsonObject)
}

/** Encodes entities without specified properties to json. */
inline fun <reified E : Entity?> Collection<E>.toJsonWithout(vararg excludeProps: KProperty1<out Entity, *>): String {
    val excludeKeys = excludeProps.map { it.name }
    val jsonObjects = map { JsonObject(json.encodeToJsonElement(it).jsonObject.filterKeys { key -> key !in excludeKeys }) }
    return json.encodeToString(jsonObjects)
}

/** Encodes entity without specified properties to json. */
inline fun <reified E : Entity?> E.toJsonWithout(vararg excludeProps: KProperty1<out Entity, *>): String {
    val excludeKeys = excludeProps.map { it.name }
    val jsonObject = JsonObject(json.encodeToJsonElement(this).jsonObject.filterKeys { it !in excludeKeys })
    return json.encodeToString(jsonObject)
}
