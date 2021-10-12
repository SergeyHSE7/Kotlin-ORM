package sql_type_functions

import utils.ifTrue


class SqlList(value: String) : SqlBase(value) {
    fun average(distinct: Boolean = false) = listToIntFunction("AVG", distinct)
    fun min(distinct: Boolean = false) = listToIntFunction("MIN", distinct)
    fun max(distinct: Boolean = false) = listToIntFunction("MAX", distinct)
    fun sum(distinct: Boolean = false) = listToIntFunction("SUM", distinct)
    fun count(distinct: Boolean = false) = listToIntFunction("COUNT", distinct)

    private fun listToIntFunction(functionName: String, distinct: Boolean) =
        SqlNumber("$functionName(" + "DISTINCT ".ifTrue(distinct) + "$value)")
}
