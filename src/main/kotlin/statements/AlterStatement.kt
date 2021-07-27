package statements

import Action
import Entity
import Table
import database
import org.tinylog.Logger
import utils.Case
import utils.columnName
import utils.transformCase
import kotlin.reflect.KMutableProperty1


fun <E : Entity> Table<E>.alter() = AlterStatement(this)

class AlterStatement<E : Entity>(private val table: Table<E>) {

    fun <R : Entity, P : R?> addForeignKey(
        property: KMutableProperty1<E, P>,
        refTable: Table<R>,
        onDelete: Action
    ) {
        database.executeSql(
            ("ALTER TABLE IF EXISTS ${table.tableName} ADD " +
                    "FOREIGN KEY (${property.columnName}) REFERENCES ${refTable.tableName}(id) " +
                    "ON DELETE ${onDelete.name.transformCase(Case.Pascal, Case.Normal).uppercase()}")
                .apply { Logger.tag("ALTER").info { this } }
        )
    }

}
