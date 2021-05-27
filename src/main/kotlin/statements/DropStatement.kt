package statements

import Entity
import Table
import database

fun <E : Entity> Table<E>.drop() = DropStatement(this).execute().also { cache.clear() }

private class DropStatement<in E : Entity>(private val table: Table<E>) {

    fun getSql(): String =
        "DROP TABLE IF EXISTS ${table.tableName} CASCADE"

    fun execute() = database.executeSql(getSql()).also { println(getSql()) }
}
