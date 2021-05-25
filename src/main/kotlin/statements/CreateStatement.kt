package statements

import Entity
import Table
import database

fun <E: Entity> Table<E>.create() = CreateStatement(this).execute()

private class CreateStatement<in E : Entity>(private val table: Table<E>) {
    private val maxLength = table.columns.maxOf { it.name.length }

    fun getSql(): String =
        "CREATE TABLE IF NOT EXISTS ${table.tableName}\n" +
                table.columns.joinToString(",\n", "(\n", "\n)") {
                    "\t" + it.toSql(maxLength)
                }

    fun execute() = database.connection.createStatement().execute(getSql()).also { println(getSql()) }
}
