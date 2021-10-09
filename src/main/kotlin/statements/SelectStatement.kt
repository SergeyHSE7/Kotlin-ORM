package statements

import Config
import Entity
import Table
import column
import database
import databases.MariaDB
import databases.PostgreSQL
import databases.SQLite
import org.tinylog.Logger
import sql_type_functions.SqlNumber
import utils.getEntity
import utils.ifTrue
import utils.map
import java.sql.ResultSet
import kotlin.reflect.KMutableProperty1

enum class JoinType {
    Inner, Left
}

data class OrderColumn(val fullColumnName: String, val isDescending: Boolean = false)

fun <E : Entity> Table<E>.selectAll(): SelectStatement<E> = SelectStatement(this, selectAll = true)
fun <E : Entity> Table<E>.select(vararg props: KMutableProperty1<*, *>): SelectStatement<E> =
    SelectStatement(this, props.map { prop -> prop.column.fullName })


class SelectStatement<E : Entity>(
    private val table: Table<E>,
    columns: List<String> = listOf(),
    private val selectAll: Boolean = false
) {
    private var lazy: Boolean = !Config.loadReferencesByDefault
    private var limit: Int? = null
    private var offset: Int? = null
    private val joinTables = mutableListOf<String>()
    private val columns = columns.toMutableSet()
    private val orderColumns = mutableSetOf<OrderColumn>()
    private var whereStatement: WhereStatement = WhereStatement()

    fun where(conditionBody: WhereCondition?) =
        this.apply { if (conditionBody != null) whereStatement = WhereStatement(conditionBody) }

    fun join(joinTable: Table<*>, joinType: JoinType = JoinType.Inner, condition: WhereCondition) =
        this.apply { joinTables.add(" ${joinType.name.uppercase()} JOIN ${joinTable.tableName} ON ${WhereStatement().condition()}") }

    inline fun <reified E : Entity> join(joinType: JoinType = JoinType.Inner, noinline condition: WhereCondition) =
        join(Table<E>(), joinType, condition)

    inline fun <reified T : Entity, R : T?> joinBy(
        property: KMutableProperty1<E, R>,
        joinType: JoinType = JoinType.Inner
    ) =
        join(Table<T>(), joinType) { "${Table<T>().tableName}.id" eq property }

    fun crossJoin(joinTable: Table<*>) = this.apply { joinTables.add(" CROSS JOIN ${joinTable.tableName}") }
    inline fun <reified E : Entity> crossJoin() = crossJoin(Table<E>())

    internal fun aggregateColumn(aggregation: SqlNumber) = this.apply { columns.add(aggregation.toString()) }

    fun lazy() = setLazy(true)
    fun setLazy(lazy: Boolean) = this.apply { this.lazy = lazy }

    fun orderBy(vararg props: KMutableProperty1<E, *>) =
        this.apply { orderColumns.addAll(props.map { OrderColumn(it.column.fullName) }) }

    fun orderByDescending(vararg props: KMutableProperty1<E, *>) =
        this.apply {
            if (props.isEmpty()) orderColumns.add(OrderColumn("${table.tableName}.id", true))
            else orderColumns.addAll(props.map { OrderColumn(it.column.fullName, true) })
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

    private fun getSelectValues() = (if (selectAll) " *"
    else if (columns.size > 0) columns.joinToString(prefix = " ")
    else " id".ifTrue(database !is PostgreSQL))

    fun getSql(): String =
        "SELECT${getSelectValues()} FROM ${table.tableName}" +
                joinTables.joinToString("") +
                whereStatement.getSql() +
                (" ORDER BY " + orderColumns.joinToString
                { it.fullColumnName + if (it.isDescending) " DESC" else " ASC" })
                    .ifTrue(orderColumns.isNotEmpty()) +
                when (database) {
                    is PostgreSQL ->
                        " LIMIT $limit".ifTrue(limit != null) + " OFFSET $offset".ifTrue(offset != null)
                    is SQLite ->
                        if (offset != null) " LIMIT ${limit ?: -1} OFFSET $offset"
                        else " LIMIT $limit".ifTrue(limit != null)
                    is MariaDB ->
                        if (offset != null) " LIMIT ${limit ?: (table.size - offset!!)} OFFSET $offset"
                        else " LIMIT $limit".ifTrue(limit != null)
                    else -> ""
                }


}


