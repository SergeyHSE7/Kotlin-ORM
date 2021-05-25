package statements

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


    infix fun <T1 : KMutableProperty1<*, *>> T1.eq(obj: Any?): String = if (obj == null) isNull() else boolOperator(obj, "=")
    infix fun <T1 : KMutableProperty1<*, *>> T1.neq(obj: Any?): String = if (obj == null) isNotNull() else boolOperator(obj, "!=")
    infix fun <T1 : KMutableProperty1<*, *>> T1.less(obj: Any?): String = boolOperator(obj, "<")
    infix fun <T1 : KMutableProperty1<*, *>> T1.greater(obj: Any?): String = boolOperator(obj, ">")
    infix fun <T1 : KMutableProperty1<*, *>> T1.lessEq(obj: Any?): String = boolOperator(obj, "<=")
    infix fun <T1 : KMutableProperty1<*, *>> T1.greaterEq(obj: Any?): String = boolOperator(obj, ">=")


    infix fun <T1 : KMutableProperty1<*, *>> T1.like(str: String): String = boolOperator(str, "LIKE")
    infix fun <T1 : KMutableProperty1<*, *>> T1.notLike(str: String): String = boolOperator(str, "NOT LIKE")

    infix fun <T1 : KMutableProperty1<*, *>> T1.startsWith(str: String): String = like("$str%")
    infix fun <T1 : KMutableProperty1<*, *>> T1.notStartsWith(str: String): String = notLike("$str%")

    infix fun <T1 : KMutableProperty1<*, *>> T1.endsWith(str: String): String = like("%$str")
    infix fun <T1 : KMutableProperty1<*, *>> T1.notEndsWith(str: String): String = notLike("%$str")

    infix fun <T1 : KMutableProperty1<*, *>> T1.contains(str: String): String = like("%$str%")
    infix fun <T1 : KMutableProperty1<*, *>> T1.notContains(str: String): String = notLike("%$str%")

    infix fun <T1 : KMutableProperty1<*, *>> T1.match(regex: Regex): String = boolOperator(regex.pattern, "~")
    infix fun <T1 : KMutableProperty1<*, *>> T1.notMatch(regex: Regex): String = boolOperator(regex.pattern, "!~")

    infix fun <T1 : KMutableProperty1<*, *>> T1.inList(list: List<Any?>): String = boolOperator(list, "IN")
    infix fun <T1 : KMutableProperty1<*, *>> T1.notInList(list: List<Any?>): String = boolOperator(list, "NOT IN")

    fun <T1 : KMutableProperty1<*, *>> T1.isNull(): String = name.transformCase(Case.Camel, Case.Snake) + " IS NULL"
    fun <T1 : KMutableProperty1<*, *>> T1.isNotNull(): String =
        name.transformCase(Case.Camel, Case.Snake) + " IS NOT NULL"

    infix fun String.and(str: String): String = "$this AND $str"
    infix fun String.or(str: String): String = "$this OR $str"


    private fun <T : KMutableProperty1<*, *>> T.boolOperator(obj: Any?, strOperator: String): String =
        name.transformCase(Case.Camel, Case.Snake) + " $strOperator " +
                when (obj) {
                    is String -> "'$obj'"
                    is List<Any?> -> obj.joinToString(", ", "(", ")") { if (it is String) "'$it'" else it.toString() }
                    is KMutableProperty1<*, *> -> obj.name.transformCase(Case.Camel, Case.Snake).also { columns.add(it) }
                    else -> obj.toString()
                }.also { columns.add(name.transformCase(Case.Camel, Case.Snake)) }
}
