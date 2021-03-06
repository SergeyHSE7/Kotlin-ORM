package statements

import Entity
import Table
import database
import org.tinylog.Logger

/** Deletes all entities that satisfy the specified [condition] (optional). */
fun <E : Entity> Table<E>.delete(condition: WhereCondition? = null) =
    DeleteStatement(this).where(condition).execute().also { cache.clear() }

/** Deletes entity by the specified [id]. */
fun <E : Entity> Table<E>.deleteById(id: Int) =
    DeleteStatement(this).where { "id" eq id }.execute()
        .also { cache.remove(id) }


internal class DeleteStatement<E : Entity>(val table: Table<E>) {
    var whereStatement: WhereStatement = WhereStatement()

    fun where(conditionBody: WhereCondition?) = this.apply { if (conditionBody != null) whereStatement = WhereStatement(conditionBody) }

    fun execute() = with(database) {
        executeSql(deleteStatementSql().also { Logger.tag("DELETE").info { it } })
    }
}
