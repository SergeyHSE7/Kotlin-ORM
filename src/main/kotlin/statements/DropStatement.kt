package statements

import Entity
import Table
import database

fun <E: Entity> Table<E>.drop() = DropStatement(this).execute()

class DropStatement<E : Entity>(private val table: Table<E>) {

    fun getSql(): String =
        "DROP TABLE IF EXISTS ${table.tableName}"

    fun execute() = database.connection.createStatement().execute(getSql()).also { println(getSql()) }
}
