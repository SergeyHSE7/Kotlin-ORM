package statements

import Entity
import sql_type_functions.*
import utils.fullColumnName
import kotlin.reflect.KMutableProperty1

typealias WhereCondition = WhereStatement.() -> String

class WhereStatement(conditionBody: WhereStatement.() -> String? = { null }) {
    private val conditions = listOf(conditionBody()).mapNotNull { it }.toMutableList()

    val columns = mutableSetOf<String>()

    fun addCondition(conditionBody: WhereCondition?) {
        if (conditionBody != null)
            conditions.add(conditionBody())
    }

    fun getSql(): String = if (conditions.isEmpty()) ""
    else " WHERE " + conditions.joinToString(" AND ")


    infix fun <T : Any?> T.eq(obj: Any?): String =
        if (obj == null) toSql() + " IS NULL" else boolOperator(obj, "=")

    infix fun <T : Any?> T.neq(obj: Any?): String =
        if (obj == null) toSql() + " IS NOT NULL" else boolOperator(obj, "!=")

    infix fun <T : Any?> T.less(obj: Any?): String = boolOperator(obj, "<")
    infix fun <T : Any?> T.greater(obj: Any?): String = boolOperator(obj, ">")
    infix fun <T : Any?> T.lessEq(obj: Any?): String = boolOperator(obj, "<=")
    infix fun <T : Any?> T.greaterEq(obj: Any?): String = boolOperator(obj, ">=")


    infix fun <T : Any?> T.like(str: String): String = boolOperator(str, "LIKE")
    infix fun <T : Any?> T.notLike(str: String): String = boolOperator(str, "NOT LIKE")

    infix fun <T : Any?> T.startsWith(str: String): String = like("$str%")
    infix fun <T : Any?> T.notStartsWith(str: String): String = notLike("$str%")

    infix fun <T : Any?> T.endsWith(str: String): String = like("%$str")
    infix fun <T : Any?> T.notEndsWith(str: String): String = notLike("%$str")

    infix fun <T : Any?> T.contains(str: String): String = like("%$str%")
    infix fun <T : Any?> T.notContains(str: String): String = notLike("%$str%")

    infix fun <T : Any?> T.match(regex: Regex): String = boolOperator(regex.pattern, "~")
    infix fun <T : Any?> T.notMatch(regex: Regex): String = boolOperator(regex.pattern, "!~")

    infix fun <T : Any?> T.inList(list: List<T>): String = boolOperator(list, "IN")
    infix fun <T : Any?> T.notInList(list: List<T>): String = boolOperator(list, "NOT IN")

    fun <P : Any, T : KMutableProperty1<*, P?>> T.isNull(): String = toSql() + " IS NULL"
    fun <P : Any, T : KMutableProperty1<*, P?>> T.isNotNull(): String = toSql() + " IS NOT NULL"

    infix fun String.and(str: String): String = "$this AND $str"
    infix fun String.or(str: String): String = "$this OR $str"


    private fun <T : Any?> T.boolOperator(obj: Any?, strOperator: String): String =
        (if (this is KMutableProperty1<*, *>) fullColumnName else this.toString()) +
                " $strOperator " + obj.toSql()

    private fun <T : Any?> T.toSql() = when (this) {
        is String -> "'$this'"
        is Entity -> id.toString()
        is List<Any?> -> joinToString(", ", "(", ")") { if (it is String) "'$it'" else it.toString() }
        is KMutableProperty1<*, *> -> fullColumnName
            .also { columns.add(it) }
        else -> this.toString()
    }


    val <S : String?, T : KMutableProperty1<*, S>> T.sqlString
        get() = SqlString(fullColumnName).also { columns.add(fullColumnName) }

    val <N : Number?, T : KMutableProperty1<*, N>> T.sqlInt
        get() = SqlNumber(fullColumnName).also { columns.add(fullColumnName) }

    val <N : Number?, T : KMutableProperty1<*, List<N>>> T.sqlList
        get() = SqlNumber(fullColumnName).also { columns.add(fullColumnName) }

}
