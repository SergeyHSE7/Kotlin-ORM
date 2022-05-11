package statements

import Entity
import Table
import database
import org.tinylog.Logger


internal fun <E : Entity> Table<E>.create() = CreateStatement(this).execute()

internal class CreateStatement<E : Entity>(val table: Table<E>) {
    val maxLength = table.columns.maxOf { it.name.length }

    fun execute() = with(database) { executeSql(createStatementSql().also { Logger.tag("CREATE").info { "\n" + it } }) }
}
