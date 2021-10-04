package statements

import Entity
import Table
import column
import sql_type_functions.SqlNumber
import sql_type_functions.SqlString
import utils.toSql
import kotlin.reflect.KMutableProperty1

typealias WhereCondition = WhereStatement.() -> String

class EntityProperty<T : Entity>(table: Table<T>, val columnName: String) {
    val fullColumnName = "${table.tableName}.$columnName"

    constructor(table: Table<T>, prop: KMutableProperty1<*, *>) : this(table, prop.column.name)
}

class WhereStatement(conditionBody: WhereStatement.() -> String? = { null }) {
    private val conditions = listOf(conditionBody()).mapNotNull { it }.toMutableList()

    fun addCondition(conditionBody: WhereCondition?) {
        if (conditionBody != null)
            conditions.add(conditionBody())
    }

    fun getSql(): String = if (conditions.isEmpty()) ""
    else " WHERE " + conditions.joinToString(" AND ")


    fun <E : Entity> Table<E>.entityProperty(prop: KMutableProperty1<*, *>) = EntityProperty(this, prop)
    fun <E : Entity> Table<E>.entityProperty(columnName: String) = EntityProperty(this, columnName)

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

    infix fun <T : Any?> T.inList(list: List<T>): String = if (list.isEmpty()) "" else boolOperator(list, "IN")
    infix fun <T : Any?> T.notInList(list: List<T>): String = if (list.isEmpty()) "" else boolOperator(list, "NOT IN")

    fun <P : Any, T : KMutableProperty1<*, P?>> T.isNull(): String = toSql() + " IS NULL"
    fun <P : Any, T : KMutableProperty1<*, P?>> T.isNotNull(): String = toSql() + " IS NOT NULL"

    infix fun String.and(str: String): String = "$this AND $str"
    infix fun String.or(str: String): String = "$this OR $str"


    private fun <T : Any?> T.boolOperator(obj: Any?, strOperator: String): String =
        this.toSql() + " $strOperator " + obj.toSql()


    val <S : String?, T : KMutableProperty1<*, S>> T.sqlString
        get() = SqlString(column.fullName)

    val <N : Number?, T : KMutableProperty1<*, N>> T.sqlInt
        get() = SqlNumber(column.fullName)

    val <N : Number?, T : KMutableProperty1<*, List<N>>> T.sqlList
        get() = SqlNumber(column.fullName)

}
