package statements

import Entity
import Reference
import Table
import database
import org.tinylog.Logger


internal fun <E : Entity> Table<E>.alter() = AlterStatement(this)

internal class AlterStatement<E : Entity>(val table: Table<E>) {

    internal fun addForeignKey(reference: Reference<E, *>) = with(database) {
        executeSql(alterStatementSql(reference).apply { Logger.tag("ALTER").info { this } })
    }

}
