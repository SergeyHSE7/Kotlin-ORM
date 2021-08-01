package utils

import Entity
import json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.reflect.KProperty1


inline fun <reified T> T.toJson(): String = json.encodeToString(this)


inline fun <reified E : Entity?> Collection<E>.toJsonOnly(vararg serializeProps: KProperty1<out Entity, *>): String {
    val serializeKeys = serializeProps.map { it.name }
    val jsonObjects = map { JsonObject(json.encodeToJsonElement(it).jsonObject.filterKeys { key -> key in serializeKeys }) }
    return json.encodeToString(jsonObjects)
}

inline fun <reified E : Entity?> E.toJsonOnly(vararg serializeProps: KProperty1<out Entity, *>): String {
    val serializeKeys = serializeProps.map { it.name }
    val jsonObject = JsonObject(json.encodeToJsonElement(this).jsonObject.filterKeys { it in serializeKeys })
    return json.encodeToString(jsonObject)
}


inline fun <reified E : Entity?> Collection<E>.toJsonWithout(vararg excludeProps: KProperty1<out Entity, *>): String {
    val excludeKeys = excludeProps.map { it.name }
    val jsonObjects = map { JsonObject(json.encodeToJsonElement(it).jsonObject.filterKeys { key -> key !in excludeKeys }) }
    return json.encodeToString(jsonObjects)
}

inline fun <reified E : Entity?> E.toJsonWithout(vararg excludeProps: KProperty1<out Entity, *>): String {
    val excludeKeys = excludeProps.map { it.name }
    val jsonObject = JsonObject(json.encodeToJsonElement(this).jsonObject.filterKeys { it !in excludeKeys })
    return json.encodeToString(jsonObject)
}
