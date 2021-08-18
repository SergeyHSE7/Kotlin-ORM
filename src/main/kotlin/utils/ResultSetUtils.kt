package utils

import Column
import Entity
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
        val index = column.getValue(this, column.name) as? Int ?: return

        val obj = if (lazy) column.refTable!!.entityClass.createInstance().apply { id = index }
        else column.refTable!!.findById(index, false)

        @Suppress("UNCHECKED_CAST")
        prop.set(entity, obj as T)
    } else prop.set(entity, column.getValue(this, column.name))
}


inline fun <T> ResultSet.map(func: ResultSet.() -> T?): List<T> {
    val list = mutableListOf<T?>()
    while (next()) list.add(func(this))
    return list.mapNotNull { it }
}
