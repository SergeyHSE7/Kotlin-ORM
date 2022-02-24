package statements

import Entity
import Table
import database
import databases.PostgreSQL
import org.tinylog.Logger
import utils.ifTrue

fun <E : Entity> Table<E>.drop() = DropStatement(this).execute().also { cache.clear() }

private class DropStatement<in E : Entity>(private val table: Table<E>) {

    private fun getSql(): String =
        "DROP TABLE IF EXISTS ${table.tableName}" + " CASCADE".ifTrue(database is PostgreSQL)

    fun execute() = database.apply { closeAllStatements() }
        .executeSql(getSql().also { Logger.tag("DROP").info { it } })
}
