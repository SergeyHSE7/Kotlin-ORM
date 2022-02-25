package statements

import Entity
import LoggerException
import Table
import column
import database
import databases.SQLite
import sql_type_functions.SqlList
import sql_type_functions.SqlNumber
import sql_type_functions.SqlString
import utils.toSql
import kotlin.reflect.KMutableProperty1

/** Condition that is cast to WHERE-clause expression. */
typealias WhereCondition = WhereStatement.() -> Expression


class Expression(val value: String = "", private val inverseValue: String = "", val isOR: Boolean = false) {
    operator fun not() = Expression(inverseValue, value)
    override fun toString() = value
    override fun equals(other: Any?) = other is Expression && value == other.value
    override fun hashCode() = value.hashCode()
    fun isEmpty() = value.isEmpty()
}

data class SubQuery(private val value: String) {
    override fun toString() = value
}

inline fun <reified E : Entity> KMutableProperty1<E, *>.subQuery(
    func: SelectStatement<E>.(KMutableProperty1<E, *>) -> SelectStatement<E> = { this }
) =
    SubQuery("(" + func(Table<E>().select(this), this).getSql() + ")")


class WhereStatement(conditionBody: WhereCondition = { Expression() }) {
    private var exprAndFlag = false
    private var condition = conditionBody()

    internal fun addCondition(conditionBody: WhereCondition) {
        if (condition.isEmpty()) condition = conditionBody()
        else condition *= conditionBody()
    }

    internal fun getSql(): String =
        if (condition.value.isNotEmpty()) " WHERE ${condition.value.removeSurrounding("(", ")")}" else ""

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

    fun <E : Entity, P : V?, V : Any> KMutableProperty1<E, P>.between(start: V, end: V) = Expression(
        "${toSql()} BETWEEN ${start.toSql()} AND ${end.toSql()}",
        "${toSql()} NOT BETWEEN ${start.toSql()} AND ${end.toSql()}"
    )
    fun <E : Entity, P : V?, V : Any> KMutableProperty1<E, P>.notBetween(start: V, end: V) = !between(start, end)

    infix fun <T : Any?> T.like(str: String) = boolOperator(str, "LIKE", "NOT LIKE")
    infix fun <T : Any?> T.notLike(str: String) = !like(str)

    infix fun <T : Any?> T.startsWith(str: String) = like("$str%")
    infix fun <T : Any?> T.notStartsWith(str: String) = !startsWith(str)

    infix fun <T : Any?> T.endsWith(str: String) = like("%$str")
    infix fun <T : Any?> T.notEndsWith(str: String) = !endsWith(str)

    infix fun <T : Any?> T.contains(str: String) = like("%$str%")
    infix fun <T : Any?> T.notContains(str: String) = !contains(str)

    infix fun <T : Any?> T.match(regex: Regex) = boolOperator(regex.pattern, "~", "!~")
    infix fun <T : Any?> T.notMatch(regex: Regex) = !match(regex)

    infix fun <T : Any?> T.inList(list: List<T>) =
        if (list.isEmpty()) Expression("FALSE", "TRUE") else boolOperator(list, "IN", "NOT IN")

    infix fun <T : Any?> T.notInList(list: List<T>) = !inList(list)

    inline infix fun <T : Any?, reified E : Entity> T.inColumn(prop: KMutableProperty1<E, *>) =
        inColumn(prop.subQuery())

    inline infix fun <T : Any?, reified E : Entity> T.notInColumn(prop: KMutableProperty1<E, *>) = !inColumn(prop)

    infix fun <T : Any?> T.inColumn(subQuery: SubQuery) =
        boolOperator(subQuery, "IN", "NOT IN")

    infix fun <T : Any?> T.notInColumn(subQuery: SubQuery) = !inColumn(subQuery)

    fun all(subQuery: SubQuery) =
        if (database is SQLite) throw LoggerException("SQLite doesn't support ALL syntax") else SubQuery("ALL $subQuery")

    fun any(subQuery: SubQuery) =
        if (database is SQLite) throw LoggerException("SQLite doesn't support ANY syntax") else SubQuery("ANY $subQuery")

    inline fun <reified E : Entity> all(prop: KMutableProperty1<E, *>) = all(prop.subQuery())
    inline fun <reified E : Entity> any(prop: KMutableProperty1<E, *>) = any(prop.subQuery())

    fun exists(subQuery: SubQuery) = Expression("EXISTS$subQuery", "NOT EXISTS$subQuery")
    fun notExists(subQuery: SubQuery) = !exists(subQuery)

    inline fun <reified E : Entity> exists(prop: KMutableProperty1<E, *>) = exists(prop.subQuery())
    inline fun <reified E : Entity> notExists(prop: KMutableProperty1<E, *>) = !exists(prop)

    fun <P : Any, T : KMutableProperty1<*, P?>> T.isNull() =
        Expression(toSql() + " IS NULL", toSql() + " IS NOT NULL")

    fun <P : Any, T : KMutableProperty1<*, P?>> T.isNotNull() = !isNull()


    operator fun Expression.times(expr: Expression): Expression = Expression(
        "${this.value} AND ${expr.value}",
        "(${this.not()} OR ${expr.not()})"
    ).also { exprAndFlag = true }

    operator fun Expression.plus(expr: Expression): Expression = Expression(
        if (exprAndFlag) "${this.value} OR ${expr.value}"
        else if (isOR) "${this.value.dropLast(1)} OR ${expr.value})"
        else "(${this.value} OR ${expr.value})",
        "${this.not()} AND ${expr.not()}",
        isOR = true
    )

    private fun <T : Any?> T.boolOperator(
        obj: Any?,
        strOperator: String,
        inverseStrOperator: String
    ): Expression {
        val left = if (this is String) this else this.toSql()
        val right = obj.toSql()
        return Expression("$left $strOperator $right", "$left $inverseStrOperator $right")
    }


    val <S : String?, T : KMutableProperty1<*, S>> T.sqlString
        get() = SqlString(column.fullName)

    val <N : Number?, T : KMutableProperty1<*, N>> T.sqlInt
        get() = SqlNumber(column.fullName)

    val <L : List<*>?, T : KMutableProperty1<*, L>> T.sqlList
        get() = SqlList(column.fullName)

}
