import sql_type_functions.SqlList
import sql_type_functions.SqlNumber
import statements.WhereCondition
import statements.WhereStatement
import kotlin.reflect.KMutableProperty1

class SubQuery {
    private var value: String? = null
    private var prop: KMutableProperty1<*, *>? = null
    private var condition: WhereCondition? = null
    private var aggregateFunc: (SqlList.() -> SqlNumber)? = null

    private constructor()

    constructor(prop: KMutableProperty1<*, *>) : this() {
        this.prop = prop
    }

    constructor(value: String) : this() {
        this.value = value
    }

    fun where(condition: WhereCondition) = this.also { this.condition = condition }
    fun aggregate(func: SqlList.() -> SqlNumber) = this.also { this.aggregateFunc = func }

    override fun toString(): String = value
        ?: "(SELECT ${aggregateFunc?.let { it(SqlList(prop!!.column.fullName)) } ?: prop!!.column.fullName} FROM ${prop!!.column.tableName}${
            if (condition != null) WhereStatement(condition!!).getSql() else ""
        })"
}
