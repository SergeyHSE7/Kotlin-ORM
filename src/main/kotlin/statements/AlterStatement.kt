package statements

import Entity
import Reference
import Table
import database
import databases.MariaDB
import org.tinylog.Logger
import utils.ifTrue


fun <E : Entity> Table<E>.alter() = AlterStatement(this)

class AlterStatement<E : Entity>(private val table: Table<E>) {

    fun addForeignKey(reference: Reference<E, *>) {
        database.executeSql(
            ("ALTER TABLE ${table.tableName} ADD " + "CONSTRAINT ".ifTrue(database is MariaDB) + reference.getForeignKey())
                .apply { Logger.tag("ALTER").info { this } }
        )
    }

}
