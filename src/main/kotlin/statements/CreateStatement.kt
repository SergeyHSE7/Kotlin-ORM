package statements

import Entity
import Table
import column
import database
import databases.SQLite
import org.tinylog.Logger
import utils.ifTrue


internal fun <E : Entity> Table<E>.create() = CreateStatement(this).execute()

private class CreateStatement<in E : Entity>(private val table: Table<E>) {
    private val maxLength = table.columns.maxOf { it.name.length }

    private fun getSql(): String =
        "CREATE TABLE IF NOT EXISTS ${table.tableName}\n(" +
                table.columns.joinToString(",\n") {
                    "\t" + it.toSql(maxLength)
                } +
                table.references.joinToString(",\n\t", prefix = ",\n\t") { it.getForeignKey() }
                    .ifTrue(database is SQLite && table.references.isNotEmpty()) +
                table.checkConditions.joinToString("") { ",\nCHECK (${it(WhereStatement())})" } +
                ",\nCONSTRAINT ${table.tableName}_unique_columns UNIQUE (${table.uniqueProps.joinToString { it.column.name }})"
                    .ifTrue(table.uniqueProps.isNotEmpty()) +
                "\n)"

    fun execute() = database.executeSql(getSql().also { Logger.tag("CREATE").info { "\n" + getSql() } })
}
