package sql_type_functions


class SqlNumber(value: String) : SqlBase(value) {
    fun abs() = simpleFunction("ABS")
    fun ceil() = simpleFunction("CEIL")
    fun floor() = simpleFunction("FLOOR")
    fun div(divider: Int) = simpleFunction("DIV", divider)
    fun mod(divider: Int) = simpleFunction("MOD", divider)
    fun power(n: Int) = simpleFunction("POWER", n)
    fun round(decimalPlaces: Int = 0) =
        if (decimalPlaces != 0) simpleFunction("ROUND", decimalPlaces)
        else simpleFunction("ROUND")
}
