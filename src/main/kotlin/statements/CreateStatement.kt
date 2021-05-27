package statements

import Entity
import Table
import database
import utils.ifTrue

fun <E : Entity> Table<E>.create() = CreateStatement(this).execute()

private class CreateStatement<in E : Entity>(private val table: Table<E>) {
    private val maxLength = table.columns.maxOf { it.name.length }

    fun getSql(): String =
        "CREATE TABLE IF NOT EXISTS ${table.tableName}\n(" +
                table.columns.joinToString(",\n") {
                    "\t" + it.toSql(maxLength)
                } +
                ",\nCONSTRAINT unique_columns UNIQUE (${table.uniqueColumns.joinToString()})"
                    .ifTrue(table.uniqueColumns.isNotEmpty()) +
                "\n)"

    fun execute() = database.executeSql(getSql()).also { println(getSql()) }
}
