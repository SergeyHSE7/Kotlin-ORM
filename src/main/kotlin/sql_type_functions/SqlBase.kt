package sql_type_functions

import utils.ifTrue

abstract class SqlBase(protected var value: String) {
    protected fun simpleFunction(functionName: String, vararg params: Int) = this
        .also {
            value = "$functionName($value" +
                    params.joinToString(", ", ", ").ifTrue(params.isNotEmpty()) +
                    ")"
        }

    override fun toString(): String = value
}
