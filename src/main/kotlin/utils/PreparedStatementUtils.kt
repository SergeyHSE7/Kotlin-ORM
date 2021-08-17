package utils

import LoggerException
import java.math.BigDecimal
import java.sql.Date
import java.sql.PreparedStatement
import java.sql.Time
import java.sql.Timestamp

fun PreparedStatement.set(array: List<Any?>) = array.forEachIndexed { index, value -> set(index + 1, value) }

fun PreparedStatement.set(index: Int, value: Any?) = when (value) {
    is String -> setString(index, value)
    is BigDecimal -> setBigDecimal(index, value)
    is Long -> setLong(index, value)
    is Int -> setInt(index, value)
    is Short -> setShort(index, value)
    is UByte -> setShort(index, value.toShort())
    is Boolean -> setBoolean(index, value)
    is Float -> setFloat(index, value)
    is Double -> setDouble(index, value)
    is Date -> setDate(index, value)
    is Time -> setTime(index, value)
    is Timestamp -> setTimestamp(index, value)
    null -> setNull(index, 4)
    else -> throw LoggerException("Unknown type: $value")
}
