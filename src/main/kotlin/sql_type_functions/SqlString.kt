package sql_type_functions


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

    operator fun plus(other: Any?) = this.also { value = "$value || ${other.toString()}" }
}
