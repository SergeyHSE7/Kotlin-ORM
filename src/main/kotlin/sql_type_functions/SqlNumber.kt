package sql_type_functions


class SqlNumber(value: String) : SqlBase(value) {
    operator fun plus(other: Int) = SqlNumber("$other + $value")
    operator fun minus(other: Int) = SqlNumber("$other - $value")
    operator fun times(other: Int) = SqlNumber("$other * $value")

    fun abs() = simpleFunction("ABS")
    fun ceil() = simpleFunction("CEIL")
    fun floor() = simpleFunction("FLOOR")
    fun div(divider: Int) = simpleFunction("DIV", divider)
    fun mod(divider: Int) = simpleFunction("MOD", divider)
    fun power(n: Int) = simpleFunction("POWER", n)
    fun round(decimalPlaces: Int = 0) =
        if (decimalPlaces != 0) simpleFunction("ROUND", decimalPlaces)
        else simpleFunction("ROUND")

    private fun simpleFunction(functionName: String, vararg params: Int) =
        SqlNumber(simpleBaseFunction(functionName, *params))
}
