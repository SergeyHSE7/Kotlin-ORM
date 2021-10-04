package sql_type_functions

import utils.ifTrue

sealed class SqlBase(protected var value: String) {
    protected open fun simpleBaseFunction(functionName: String, vararg params: Int): String =
        "$functionName($value" + params.joinToString(", ", ", ").ifTrue(params.isNotEmpty()) + ")"

    override fun toString(): String = value
}
