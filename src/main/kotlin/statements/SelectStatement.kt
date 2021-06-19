package statements

import Entity
import Table
import columnName
import database
import utils.*
import kotlin.reflect.KMutableProperty1

data class OrderColumn(val column: String, val isDescending: Boolean = false)

fun <E : Entity> Table<E>.selectAll(): SelectStatement<E> = SelectStatement(this, selectAll = true)
fun <E : Entity> Table<E>.select(vararg propsNames: String): SelectStatement<E> =
    SelectStatement(this, propsNames.toList())

fun <E : Entity> Table<E>.select(vararg props: KMutableProperty1<E, *>): SelectStatement<E> =
    SelectStatement(this, props.map { prop -> prop.columnName })

class SelectStatement<E : Entity>(
    private val table: Table<E>,
    columns: List<String> = listOf(),
    private val selectAll: Boolean = false
) {
    private var lazy: Boolean = false
    private var limit: Int? = null
    private var offset: Int? = null
    private val columns = columns.toMutableSet()
    private val orderColumns = mutableSetOf<OrderColumn>()
    private var whereStatement: WhereStatement = WhereStatement()

    fun where(conditionBody: WhereCondition?) = this.apply { whereStatement.addCondition(conditionBody) }

    fun lazy() = this.apply { lazy = true }

    fun orderBy(vararg props: KMutableProperty1<E, *>) =
        this.apply { orderColumns.addAll(props.map { OrderColumn(it.columnName) }) }

    fun orderByDescending(vararg props: KMutableProperty1<E, *>) =
        this.apply { orderColumns.addAll(props.map { OrderColumn(it.columnName, true) }) }

    fun limit(limit: Int) = this.apply { this.limit = limit }
    fun offset(offset: Int) = this.apply { this.offset = offset }

    fun getEntity(): E? = database.executeQuery(getSql())
        .run { if (next()) getEntity(table, lazy) else null }
        .also {
            if (selectAll && it != null) table.cache.add(it, !lazy)
            println(getSql())
        }

    fun getEntities(): List<E> = database.executeQuery(getSql())
        .map { getEntity(table, lazy) }
        .also {
            if (selectAll) table.cache.addAll(it, !lazy)
            println(getSql())
        }

    fun getSql(): String =
        "SELECT ${if (selectAll) "*" else columns.apply { addAll(whereStatement.columns) }.joinToString { it }} " +
                "FROM ${table.tableName}" +
                whereStatement.getSql() +
                (" ORDER BY " + orderColumns.joinToString { it.column + if (it.isDescending) " DESC" else " ASC" })
                    .ifTrue(orderColumns.isNotEmpty()) +
                " LIMIT $limit".ifTrue(limit != null) +
                " OFFSET $offset".ifTrue(offset != null)


}


