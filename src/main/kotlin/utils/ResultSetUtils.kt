package utils

import Entity
import Table
import org.postgresql.util.PSQLException
import java.sql.Date
import java.sql.ResultSet
import java.sql.Time
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.createInstance

fun <E : Entity> ResultSet.getEntity(table: Table<E>): E? = try {
    val entity: E = table.entityClass.createInstance()
    table.columns.forEach { setProp(entity, it) }
    entity
} catch (ex: PSQLException) {
    if (ex.message?.contains("next") == true && next()) {
        println("next")
        getEntity(table)
    }
    null
}

fun <E: Entity, T> ResultSet.setProp(entity: E, column: Table<E>.Column<T>) {
    val prop = entity.properties.firstOrNull { it.name == column.property.name }
            as? KMutableProperty1<E, T> ?: return

    try {
        if (column.refTable != null) {
            // println(prop.name + " " + getValue(column))
            val index = (getValue(column) as? String?)?.toIntOrNull() ?: return
            prop.set(entity, column.refTable.findById(index) as T)
        }
        else {
            // println(prop.returnType)
            prop.set(entity, getValue(column))
        }
    } catch (ex: PSQLException) {
        if (ex.message?.contains("не найдено") == false)
            throw ex
    }
}

fun <E: Entity, T> ResultSet.getValue(column: Table<E>.Column<T>): T = when (column.property.get(column.entityClass.createInstance())) {
    is String? -> getString(column.name)
    is Int? -> getInt(column.name)
    is Boolean? -> getBoolean(column.name)
    is Date? -> getDate(column.name)
    is Float? -> getFloat(column.name)
    is Double? -> getDouble(column.name)
    is Time? -> getTime(column.name)
    is Entity? -> getInt(column.name)
    else -> throw java.lang.Exception("Unknown type: ${column.property.name}")
} as T


fun <T> ResultSet.map(func: ResultSet.() -> T?): List<T> {
    val list = mutableListOf<T?>()
    while (next()) list.add(func(this))
    return list.mapNotNull { it }
}
