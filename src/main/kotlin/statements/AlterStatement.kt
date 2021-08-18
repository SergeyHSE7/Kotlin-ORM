package statements

import Entity
import Reference
import Table
import database
import org.tinylog.Logger


fun <E : Entity> Table<E>.alter() = AlterStatement(this)

class AlterStatement<E : Entity>(private val table: Table<E>) {

    fun addForeignKey(reference: Reference<E, *>) {
        database.executeSql(
            ("ALTER TABLE ${table.tableName} ADD " + reference.getForeignKey())
                .apply { Logger.tag("ALTER").info { this } }
        )
    }

}
