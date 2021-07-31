package statements

import Entity
import Table
import utils.columnName
import database
import org.tinylog.Logger
import sql_type_functions.SqlNumber
import sql_type_functions.SqlList
import utils.*
import java.sql.ResultSet
import kotlin.reflect.KMutableProperty1

data class OrderColumn(val column: String, val isDescending: Boolean = false)

fun <E : Entity> Table<E>.selectAll(): SelectStatement<E> = SelectStatement(this, selectAll = true)
fun <E : Entity> Table<E>.select(vararg props: KMutableProperty1<*, *>): SelectStatement<E> =
    SelectStatement(this, props.map { prop -> prop.columnName })

fun <E : Entity> Table<E>.select(prop: KMutableProperty1<*, *>, function: (SqlList) -> SqlNumber): Double =
    SelectStatement(this, listOf(function(SqlList(prop.columnName)).toString()))
        .getResultSet().apply { next() }.getDouble(1)


class SelectStatement<E : Entity>(
    private val table: Table<E>,
    columns: List<String> = listOf(),
    private val selectAll: Boolean = false
) {
    enum class JoinType {
        Inner, Left, Right, Full, Cross
    }

    data class JoinTable(val joinType: JoinType, val table: Table<*>, val condition: WhereCondition) {
        override fun toString(): String = if (joinType == JoinType.Cross) " CROSS JOIN ${table.tableName}"
        else " ${joinType.name.uppercase()} JOIN ${table.tableName} ON ${WhereStatement().condition()}"
    }

    private var lazy: Boolean = !Config.loadReferencesByDefault
    private var limit: Int? = null
    private var offset: Int? = null
    private val joinTables = mutableListOf<JoinTable>()
    private val columns = columns.toMutableSet()
    private val orderColumns = mutableSetOf<OrderColumn>()
    private var whereStatement: WhereStatement = WhereStatement()

    fun where(conditionBody: WhereCondition?) = this.apply { whereStatement.addCondition(conditionBody) }
//    fun where(vararg values: Pair<KMutableProperty1<E, *>, Any?>) =
//        this.apply { values.forEach { where { it.first eq it.second } } }

    fun innerJoin(joinTable: Table<*>, condition: WhereCondition) =
        this.apply { joinTables.add(JoinTable(JoinType.Inner, joinTable, condition)) }
    inline fun <reified T: Entity> innerJoinBy(property: KMutableProperty1<E, T>) =
        innerJoin(Table<T>()) { property eq "${Table<T>().tableName}.id" }

    fun lazy() = setLazy(true)
    fun setLazy(lazy: Boolean) = this.apply { this.lazy = lazy }

    fun orderBy(vararg props: KMutableProperty1<E, *>) =
        this.apply { orderColumns.addAll(props.map { OrderColumn(it.columnName) }) }

    fun orderByDescending(vararg props: KMutableProperty1<E, *>) =
        this.apply {
            if (props.isEmpty()) orderColumns.add(OrderColumn("id", true))
            else orderColumns.addAll(props.map { OrderColumn(it.columnName, true) })
        }

    fun limit(limit: Int) = this.apply { this.limit = limit }
    fun offset(offset: Int) = this.apply { this.offset = offset }

    fun getResultSet(): ResultSet = database.executeQuery(getSql().also { Logger.tag("SELECT").info { it } })

    val size: Int
        get() = getResultSet().map {}.size

    fun getEntity(): E? = getResultSet()
        .run { if (next()) getEntity(table, lazy) else null }
        .also {
            if (selectAll && it != null) table.cache.add(it, !lazy)
        }

    fun getEntities(): List<E> = getResultSet()
        .map { getEntity(table, lazy) }
        .also {
            if (selectAll) table.cache.addAll(it, !lazy)
        }

    fun getSql(): String =
        "SELECT ${if (selectAll) "*" else columns.apply { addAll(whereStatement.columns) }.joinToString { it }} " +
                "FROM ${table.tableName}" +
                joinTables.joinToString("") +
                whereStatement.getSql() +
                (" ORDER BY " + orderColumns.joinToString { it.column + if (it.isDescending) " DESC" else " ASC" })
                    .ifTrue(orderColumns.isNotEmpty()) +
                " LIMIT $limit".ifTrue(limit != null) +
                " OFFSET $offset".ifTrue(offset != null)

}


