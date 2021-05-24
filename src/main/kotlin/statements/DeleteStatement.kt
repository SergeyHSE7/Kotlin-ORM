package statements

import Entity
import Table
import database

fun <E : Entity> Table<E>.delete(condition: WhereCondition? = null) =
    DeleteStatement(this).where(condition).execute()

class DeleteStatement<E : Entity>(private val table: Table<E>) {
    private var whereStatement: WhereStatement = WhereStatement()

    fun where(conditionBody: WhereCondition?) = this.apply { whereStatement.addCondition(conditionBody) }

    fun execute() {
        database.connection.createStatement().execute(getSql()).also { println(getSql()) }
    }

    fun getSql(): String = "DELETE FROM ${table.tableName}" + whereStatement.getSql()
}
