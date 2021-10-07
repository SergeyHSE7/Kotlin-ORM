package statements

import Entity
import Table
import column
import sql_type_functions.SqlList
import sql_type_functions.SqlNumber
import sql_type_functions.SqlString
import utils.ifTrue
import utils.toSql
import kotlin.reflect.KMutableProperty1

typealias WhereCondition = WhereStatement.() -> Expression

class EntityProperty<T : Entity>(table: Table<T>, val columnName: String) {
    val fullColumnName = "${table.tableName}.$columnName"

    constructor(table: Table<T>, prop: KMutableProperty1<*, *>) : this(table, prop.column.name)
}

class Expression(val value: String = "", private val inverseValue: String = "") {
    fun inverse() = Expression(inverseValue, value)
    operator fun not() = inverse()

    override fun toString() = value
    override fun equals(other: Any?) = other is Expression && value == other.value
    override fun hashCode() = value.hashCode()
}


class WhereStatement(conditionBody: WhereCondition = { Expression() }) {
    private var exprAndFlag = false
    private val condition = conditionBody()

    fun getSql(): String = " WHERE $condition".ifTrue(condition.value.isNotEmpty())

    fun <E : Entity> Table<E>.entityProperty(prop: KMutableProperty1<*, *>) = EntityProperty(this, prop)
    fun <E : Entity> Table<E>.entityProperty(columnName: String) = EntityProperty(this, columnName)

    infix fun <T : Any?> T.eq(obj: Any?) =
        if (obj == null) Expression(toSql() + " IS NULL", toSql() + " IS NOT NULL")
        else boolOperator(obj, "=", "!=")

    infix fun <T : Any?> T.neq(obj: Any?) =
        if (obj == null) Expression(toSql() + " IS NOT NULL")
        else boolOperator(obj, "!=", "=")

    infix fun <T : Any?> T.less(obj: Any?) = boolOperator(obj, "<", ">=")
    infix fun <T : Any?> T.greater(obj: Any?) = boolOperator(obj, ">", "<=")
    infix fun <T : Any?> T.lessEq(obj: Any?) = boolOperator(obj, "<=", ">")
    infix fun <T : Any?> T.greaterEq(obj: Any?) = boolOperator(obj, ">=", "<")


    infix fun <T : Any?> T.like(str: String) = boolOperator(str, "LIKE", "NOT LIKE")
    infix fun <T : Any?> T.notLike(str: String) = boolOperator(str, "NOT LIKE", "LIKE")

    infix fun <T : Any?> T.startsWith(str: String) = like("$str%")
    infix fun <T : Any?> T.notStartsWith(str: String) = notLike("$str%")

    infix fun <T : Any?> T.endsWith(str: String) = like("%$str")
    infix fun <T : Any?> T.notEndsWith(str: String) = notLike("%$str")

    infix fun <T : Any?> T.contains(str: String) = like("%$str%")
    infix fun <T : Any?> T.notContains(str: String) = notLike("%$str%")

    infix fun <T : Any?> T.match(regex: Regex) = boolOperator(regex.pattern, "~", "!~")
    infix fun <T : Any?> T.notMatch(regex: Regex) = boolOperator(regex.pattern, "!~", "~")

    infix fun <T : Any?> T.inList(list: List<T>) =
        if (list.isEmpty()) Expression() else boolOperator(list, "IN", "NOT IN")

    infix fun <T : Any?> T.notInList(list: List<T>) =
        if (list.isEmpty()) Expression() else boolOperator(list, "NOT IN", "IN")

    fun <P : Any, T : KMutableProperty1<*, P?>> T.isNull() =
        Expression(toSql() + " IS NULL", toSql() + " IS NOT NULL")

    fun <P : Any, T : KMutableProperty1<*, P?>> T.isNotNull() =
        Expression(toSql() + " IS NOT NULL", toSql() + " IS NULL")


    operator fun Expression.times(expr: Expression): Expression = Expression("${this.value} AND ${expr.value}",
        "(${this.inverse()} OR ${expr.inverse()})").also { exprAndFlag = true }

    operator fun Expression.plus(expr: Expression): Expression = Expression(
        if (exprAndFlag) "${this.value} OR ${expr.value}"
        else "(${this.value} OR ${expr.value})",
        "${this.inverse()} AND ${expr.inverse()}"
    )

    private fun <T : Any?> T.boolOperator(
        obj: Any?,
        strOperator: String,
        inverseStrOperator: String
    ): Expression = Expression(this.toSql() + " $strOperator " + obj.toSql(),
        this.toSql() + " $inverseStrOperator " + obj.toSql())


    val <S : String?, T : KMutableProperty1<*, S>> T.sqlString
        get() = SqlString(column.fullName)

    val <N : Number?, T : KMutableProperty1<*, N>> T.sqlInt
        get() = SqlNumber(column.fullName)

    val <L : List<*>?, T : KMutableProperty1<*, L>> T.sqlList
        get() = SqlList(column.fullName)

}
