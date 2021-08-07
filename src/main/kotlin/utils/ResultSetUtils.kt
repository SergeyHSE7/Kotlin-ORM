package utils

import Column
import Entity
import Table
import java.sql.ResultSet
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.createInstance

fun <E : Entity> ResultSet.getEntity(table: Table<E>, lazy: Boolean): E {
    val entity: E = table.entityClass.createInstance()
    table.columns.forEach { setProp(entity, it, lazy) }
    return entity
}

fun <E : Entity, T> ResultSet.setProp(entity: E, column: Column<E, T>, lazy: Boolean) {
    val prop = entity.properties.firstOrNull { it.name == column.property.name }
            as? KMutableProperty1<E, T> ?: return

    kotlin.runCatching {
        if (column.refTable != null) {
            val index = getValue(column) as? Int ?: return
            val obj = if (lazy) column.refTable!!.entityClass.createInstance().apply { id = index }
            else column.refTable!!.findById(index, false)

            prop.set(entity, obj as T)
        } else prop.set(entity, getValue(column))
    }
}

private operator fun Regex.contains(text: CharSequence): Boolean = this.matches(text)

fun <E : Entity, T> ResultSet.getValue(column: Column<E, T>): T =
    when (column.sqlType) {
        in Regex("""decimal\.*"""), in Regex("""numeric\.*""") -> getBigDecimal(column.name)
        in Regex("""varchar\(\d+\)"""), in Regex("""char\(\d+\)"""),
        "text", "json", "uuid" -> getString(column.name)
        "bigint" -> getLong(column.name)
        "integer", "serial" -> getInt(column.name)
        "smallint" -> getShort(column.name)
        "tinyint" -> getShort(column.name).toUByte()
        "double precision" -> getDouble(column.name)
        "real" -> getFloat(column.name)
        "boolean" -> getBoolean(column.name)
        "date" -> getDate(column.name)
        in Regex("""timestamp\.*""") -> getTimestamp(column.name)
        in Regex("""time\.*""") -> getTime(column.name)
        else -> throw java.lang.Exception("Unknown type: ${column.fullName} - ${column.sqlType}")
    } as T


inline fun <T> ResultSet.map(func: ResultSet.() -> T?): List<T> {
    val list = mutableListOf<T?>()
    while (next()) list.add(func(this))
    return list.mapNotNull { it }
}
