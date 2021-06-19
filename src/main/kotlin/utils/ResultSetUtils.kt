package utils

import Entity
import Table
import org.postgresql.util.PSQLException
import java.sql.Date
import java.sql.ResultSet
import java.sql.Time
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.hasAnnotation

fun <E : Entity> ResultSet.getEntity(table: Table<E>, lazy: Boolean): E {
    val entity: E = table.entityClass.createInstance()
    table.columns.forEach { setProp(entity, it, lazy) }
    return entity
}

fun <E : Entity, T> ResultSet.setProp(entity: E, column: Table<E>.Column<T>, lazy: Boolean) {
    val prop = entity.properties.firstOrNull { it.name == column.property.name }
            as? KMutableProperty1<E, T> ?: return

    try {
        if (column.refTable != null) {
            val index = (getValue(column) as? String?)?.toIntOrNull() ?: return
            val obj = if (lazy || column.property.hasAnnotation<FetchLazy>())
                column.refTable.entityClass.createInstance().apply { id = index }
            else column.refTable.findById(index, false)

            prop.set(entity, obj as T)
        } else prop.set(entity, getValue(column))

    } catch (ex: PSQLException) {
        if (ex.message?.contains("не найдено") == false)
            throw ex
    }
}

fun <E : Entity, T> ResultSet.getValue(column: Table<E>.Column<T>): T =
    when (column.property.get(column.entityClass.createInstance())) {
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
