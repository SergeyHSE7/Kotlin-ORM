package sql_type_functions

import database
import databases.MariaDB
import utils.toSql


class SqlString(value: String) : SqlBase(value) {
    fun lowercase() = simpleFunction("LOWER")
    fun uppercase() = simpleFunction("UPPER")
    fun trim() = simpleFunction("TRIM")

    fun substring(range: IntRange) = substring(range.first, range.last + 1)
    fun substring(startIndex: Int) =
        this.also { value = "SUBSTRING($value from ${startIndex + 1})" }

    fun substring(startIndex: Int, endIndex: Int) =
        this.also { value = "SUBSTRING($value from ${startIndex + 1} for ${endIndex - startIndex})" }

    fun length() = SqlNumber("LENGTH($value)")

    operator fun plus(other: Any?) = this.also {
        value = if (database is MariaDB) "CONCAT($value, ${other.toSql()})"
        else "$value || ${other.toSql()}"
    }

    private fun simpleFunction(functionName: String, vararg params: Int) =
        SqlString(simpleBaseFunction(functionName, *params))
}
