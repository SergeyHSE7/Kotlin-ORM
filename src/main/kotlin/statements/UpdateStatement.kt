package statements

import Entity
import Table
import column
import database
import org.tinylog.Logger
import utils.toSql
import kotlin.reflect.KMutableProperty1

/**
 * Update the data of specified [entity] in database.
 *
 * @param[entity] The entity to update.
 * @param[props] The properties of entity to update.
 * (if none are specified then all properties of the entity are updated).
 */
fun <E : Entity> Table<out E>.update(entity: E, props: List<KMutableProperty1<E, *>>) =
    UpdateStatement(this, entity, props).where { Expression("id = ${entity.id}") }.execute()
        .also { cache.remove(entity.id) }

internal class UpdateStatement<out E : Entity>(
    val table: Table<out E>,
    private val entity: E,
    private val props: List<KMutableProperty1<E, *>> = listOf()
) {
    val columnValues: MutableList<Pair<String, String>> = props.ifEmpty { entity.properties }
        .filter { it.column.name != "id" }
        .map { prop -> prop.column.name to prop.getter.call(entity).toSql()
        }.toMutableList()


    private fun updateReferences() {
        table.columns.filter { it.refTable != null && (props.isEmpty() || props.contains(it.property)) }
            .forEach {
                val refEntity = it.property.getter.call(entity) as Entity
                if (refEntity.id != 0 && it.refTable?.get(refEntity.id) != null) it.refTable!!.update(refEntity)
                else {
                    it.refTable?.add(refEntity)
                    val index = columnValues.indexOfFirst { pair -> pair.first == it.name }
                    columnValues[index] = Pair(it.name, refEntity.id.toString())
                }
            }
    }

    var whereStatement: WhereStatement = WhereStatement()

    fun where(conditionBody: WhereCondition?) = this.apply { if (conditionBody != null) whereStatement = WhereStatement(conditionBody) }

    fun execute() = with(database) {
        updateReferences()
        executeSql(updateStatementSql().also { Logger.tag("UPDATE").info { it } })
    }

}
