package utils

import java.sql.Date
import java.sql.PreparedStatement
import java.sql.Time

fun PreparedStatement.set(array: List<Any?>) = array.forEachIndexed { index, value -> set(index + 1, value) }

fun PreparedStatement.set(index: Int, value: Any?) = when (value) {
    is String -> setString(index, value)
    is Int -> setInt(index, value)
    is Boolean -> setBoolean(index, value)
    is Date -> setDate(index, value)
    is Float -> setFloat(index, value)
    is Double -> setDouble(index, value)
    is Time -> setTime(index, value)
    else -> throw Exception("Unknown type: ${value?.javaClass?.name ?: "null"}")
}
