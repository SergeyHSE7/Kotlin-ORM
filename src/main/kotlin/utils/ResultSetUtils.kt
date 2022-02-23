package utils

import Column
import Entity
import Table
import java.sql.ResultSet
import kotlin.reflect.full.createInstance

fun <E : Entity> ResultSet.getEntity(table: Table<E>, lazy: Boolean): E {
    val entity: E = table.entityClass.createInstance()
    val columnNames = table.columns.map { it.name }.toHashSet()
    with(metaData) {
        (1..columnCount)
            .filter { getTableName(it) == table.tableName && getColumnName(it) in columnNames }
            .forEach { colIndex ->
                setProp(entity, table.columns.first { it.name == getColumnName(colIndex) }, colIndex, lazy)
            }
    }

    return entity
}

fun <E : Entity, T> ResultSet.setProp(entity: E, column: Column<E, T>, columnIndex: Int, lazy: Boolean) {
    val prop = column.property

    if (column.refTable != null) {
        val index = column.getValue(this, columnIndex) as? Int ?: return

        val obj = if (lazy) column.refTable!!.entityClass.createInstance().apply { id = index }
        else getEntity(column.refTable!!, false).also { column.refTable!!.cache.add(it, true) }

        @Suppress("UNCHECKED_CAST")
        prop.set(entity, obj as T)
    } else prop.set(entity, column.getValue(this, columnIndex))
}


inline fun <T> ResultSet.map(func: ResultSet.() -> T?): List<T> {
    val list = mutableListOf<T?>()
    while (next()) list.add(func(this))
    return list.mapNotNull { it }
}
