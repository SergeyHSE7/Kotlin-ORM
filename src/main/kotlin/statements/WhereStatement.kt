package statements

import utils.columnName
import utils.fullColumnName
import utils.Case
import utils.transformCase
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


    infix fun <T : KMutableProperty1<*, *>> T.eq(obj: Any?): String =
        if (obj == null) isNull() else boolOperator(obj, "=")

    infix fun <T : KMutableProperty1<*, *>> T.neq(obj: Any?): String =
        if (obj == null) isNotNull() else boolOperator(obj, "!=")

    infix fun <T : KMutableProperty1<*, *>> T.less(obj: Any?): String = boolOperator(obj, "<")
    infix fun <T : KMutableProperty1<*, *>> T.greater(obj: Any?): String = boolOperator(obj, ">")
    infix fun <T : KMutableProperty1<*, *>> T.lessEq(obj: Any?): String = boolOperator(obj, "<=")
    infix fun <T : KMutableProperty1<*, *>> T.greaterEq(obj: Any?): String = boolOperator(obj, ">=")


    infix fun <T : KMutableProperty1<*, *>> T.like(str: String): String = boolOperator(str, "LIKE")
    infix fun <T : KMutableProperty1<*, *>> T.notLike(str: String): String = boolOperator(str, "NOT LIKE")

    infix fun <T : KMutableProperty1<*, *>> T.startsWith(str: String): String = like("$str%")
    infix fun <T : KMutableProperty1<*, *>> T.notStartsWith(str: String): String = notLike("$str%")

    infix fun <T : KMutableProperty1<*, *>> T.endsWith(str: String): String = like("%$str")
    infix fun <T : KMutableProperty1<*, *>> T.notEndsWith(str: String): String = notLike("%$str")

    infix fun <T : KMutableProperty1<*, *>> T.contains(str: String): String = like("%$str%")
    infix fun <T : KMutableProperty1<*, *>> T.notContains(str: String): String = notLike("%$str%")

    infix fun <T : KMutableProperty1<*, *>> T.match(regex: Regex): String = boolOperator(regex.pattern, "~")
    infix fun <T : KMutableProperty1<*, *>> T.notMatch(regex: Regex): String = boolOperator(regex.pattern, "!~")

    infix fun <T : KMutableProperty1<*, *>> T.inList(list: List<Any?>): String = boolOperator(list, "IN")
    infix fun <T : KMutableProperty1<*, *>> T.notInList(list: List<Any?>): String = boolOperator(list, "NOT IN")

    fun <T : KMutableProperty1<*, *>> T.isNull(): String = name.transformCase(Case.Camel, Case.Snake) + " IS NULL"
    fun <T : KMutableProperty1<*, *>> T.isNotNull(): String = "$columnName IS NOT NULL"

    infix fun String.and(str: String): String = "$this AND $str"
    infix fun String.or(str: String): String = "$this OR $str"


    private fun <T : KMutableProperty1<*, *>> T.boolOperator(obj: Any?, strOperator: String): String =
        "$fullColumnName $strOperator " +
                when (obj) {
                    is String -> "'$obj'"
                    is List<Any?> -> obj.joinToString(", ", "(", ")") { if (it is String) "'$it'" else it.toString() }
                    is KMutableProperty1<*, *> -> obj.fullColumnName
                        .also { columns.add(it) }
                    else -> obj.toString()
                }.also { columns.add(columnName) }
}
