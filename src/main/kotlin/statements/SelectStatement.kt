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
import sql_type_functions.SqlList
import sql_type_functions.SqlNumber
import utils.getEntity
import utils.ifTrue
import utils.map
import java.sql.ResultSet
import kotlin.reflect.KMutableProperty1


data class OrderColumn(val fullColumnName: String, val isDescending: Boolean = false) {
    constructor(prop: EntityProperty<*>, isDescending: Boolean = false) : this(prop.fullColumnName, isDescending)
}

fun <E : Entity> Table<E>.selectAll(): SelectStatement<E> = SelectStatement(this, selectAll = true)
fun <E : Entity> Table<E>.select(vararg props: KMutableProperty1<*, *>): SelectStatement<E> =
    SelectStatement(this, props.map { prop -> prop.column.fullName })

fun <E : Entity> Table<E>.select(prop: EntityProperty<*>, function: (SqlList) -> SqlNumber): Double =
    SelectStatement(this, listOf(function(SqlList(prop.columnName)).toString()))
        .getResultSet().apply { next() }.getDouble(1)

fun <E : Entity> Table<E>.select(prop: KMutableProperty1<*, *>, function: (SqlList) -> SqlNumber): Double =
    select(EntityProperty(this, prop), function)


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

    fun innerJoin(joinTable: Table<*>, condition: WhereCondition) =
        this.apply { joinTables.add(JoinTable(JoinType.Inner, joinTable, condition)) }

    inline fun <reified T : Entity, R : T?> innerJoinBy(property: KMutableProperty1<E, R>) =
        innerJoin(Table<T>()) { property eq Table<T>().entityProperty("id") }

    fun lazy() = setLazy(true)
    fun setLazy(lazy: Boolean) = this.apply { this.lazy = lazy }

    fun orderBy(vararg props: KMutableProperty1<E, *>) =
        this.apply { orderColumns.addAll(props.map { OrderColumn(it.column.fullName) }) }

    fun orderByDescending(vararg props: KMutableProperty1<E, *>) =
        this.apply {
            if (props.isEmpty()) orderColumns.add(OrderColumn(EntityProperty(table, "id"), true))
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

    fun getSql(): String =
        "SELECT" +
                (if (selectAll) " *"
                else if (columns.size > 0) columns.joinToString(prefix = " ")
                else " id".ifTrue(database !is PostgreSQL)) +
                " FROM ${table.tableName}" +
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


