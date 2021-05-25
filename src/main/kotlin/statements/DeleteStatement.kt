package statements

import Entity
import Table
import database

fun <E : Entity> Table<E>.delete(condition: WhereCondition? = null) =
    DeleteStatement(this).where(condition).execute().also { cache.clear() }

fun <E : Entity> Table<E>.deleteById(id: Int) =
    DeleteStatement(this).where { Entity::id eq id }.execute()
        .also { cache.remove(id) }


private class DeleteStatement<E : Entity>(private val table: Table<E>) {
    private var whereStatement: WhereStatement = WhereStatement()

    fun where(conditionBody: WhereCondition?) = this.apply { whereStatement.addCondition(conditionBody) }

    fun execute() {
        database.connection.createStatement().execute(getSql()).also { println(getSql()) }
    }

    fun getSql(): String = "DELETE FROM ${table.tableName}" + whereStatement.getSql()
}
