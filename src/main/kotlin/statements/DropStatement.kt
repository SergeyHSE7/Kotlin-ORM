package statements

import Entity
import Table
import database
import org.tinylog.Logger

internal fun <E : Entity> Table<E>.drop() = DropStatement(this).execute().also { cache.clear() }

internal class DropStatement<E : Entity>(val table: Table<E>) {
    fun execute() = with(database) {
        closeAllStatements()
        executeSql(dropStatementSql().also { Logger.tag("DROP").info { it } })
    }
}
