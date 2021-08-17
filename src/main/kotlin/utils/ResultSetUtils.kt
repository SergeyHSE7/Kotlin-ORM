package utils

import Column
import Entity
import LoggerException
import Table
import java.sql.ResultSet
import kotlin.reflect.full.createInstance

fun <E : Entity> ResultSet.getEntity(table: Table<E>, lazy: Boolean): E {
    val entity: E = table.entityClass.createInstance()
    val availableColumns = with(metaData) { (1..columnCount).map { getColumnName(it) } }
    table.columns.filter { it.name in availableColumns }.forEach { setProp(entity, it, lazy) }
    return entity
}

fun <E : Entity, T> ResultSet.setProp(entity: E, column: Column<E, T>, lazy: Boolean) {
    val prop = column.property

    if (column.refTable != null) {
        val index = getValue(column) as? Int ?: return

        val obj = if (lazy) column.refTable!!.entityClass.createInstance().apply { id = index }
        else column.refTable!!.findById(index, false)

        @Suppress("UNCHECKED_CAST")
        prop.set(entity, obj as T)
    } else prop.set(entity, getValue(column))
}

private operator fun Regex.contains(text: CharSequence): Boolean = this.matches(text)

@Suppress("UNCHECKED_CAST")
fun <E : Entity, T> ResultSet.getValue(column: Column<E, T>): T =
    if (column.refTable != null) getInt(column.name) as T
    else when (column.property.type) {
        decimalType -> getBigDecimal(column.name)
        stringType -> getString(column.name)
        int8Type -> getLong(column.name)
        int4Type -> getInt(column.name)
        int2Type -> getShort(column.name)
        int1Type -> getShort(column.name).toUByte()
        doubleType -> getDouble(column.name)
        floatType -> getFloat(column.name)
        boolType -> getBoolean(column.name)
        dateType -> getDate(column.name)
        timestampType -> getTimestamp(column.name)
        timeType -> getTime(column.name)
        else -> throw LoggerException("Unknown type: ${column.fullName} - ${column.property.type}")
    } as T


inline fun <T> ResultSet.map(func: ResultSet.() -> T?): List<T> {
    val list = mutableListOf<T?>()
    while (next()) list.add(func(this))
    return list.mapNotNull { it }
}
