package statements

import Entity
import Table
import database
import utils.*
import kotlin.reflect.KMutableProperty1

fun <E : Entity> Table<E>.selectAll(): SelectStatement<E> = SelectStatement(this, selectAll = true)
fun <E : Entity> Table<E>.select(vararg propsNames: String): SelectStatement<E> =
    SelectStatement(this, propsNames.toList())

fun <E : Entity> Table<E>.select(vararg props: KMutableProperty1<E, *>): SelectStatement<E> =
    SelectStatement(this, props.map { prop -> prop.name.transformCase(Case.Camel, Case.Snake) })

class SelectStatement<E : Entity>(
    private val table: Table<E>,
    columns: List<String> = listOf(),
    private val selectAll: Boolean = false
) {
    private val columns = columns.toMutableSet()
    private var whereStatement: WhereStatement = WhereStatement()

    fun where(conditionBody: WhereCondition?) = this.apply { whereStatement.addCondition(conditionBody) }

    fun getEntity(): E? = database.connection.createStatement().executeQuery(getSql())
        .apply { next() }.getEntity(table)
        .also {
            if (selectAll && it != null) table.cache += it
            println(getSql())
        }

    fun getEntities(): List<E> = database.connection.createStatement().executeQuery(getSql())
        .map { getEntity(table) }
        .also {
            if (selectAll) table.cache.addAll(it)
            println(getSql())
        }

    fun getSql(): String =
        "SELECT ${if (selectAll) "*" else columns.apply { addAll(whereStatement.columns) }.joinToString { it }} " +
                "FROM ${table.tableName}" +
                whereStatement.getSql()


}


