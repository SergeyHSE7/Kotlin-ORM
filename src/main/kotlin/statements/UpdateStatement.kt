package statements

import Entity
import Table
import column
import database
import org.tinylog.Logger
import kotlin.reflect.KMutableProperty1

fun <E : Entity> Table<out E>.update(entity: E, props: List<KMutableProperty1<E, *>>) =
    UpdateStatement(this, entity, props).where { Expression("id = ${entity.id}") }.execute()
        .also { cache.remove(entity.id) }

private class UpdateStatement<out E : Entity>(
    private val table: Table<out E>,
    private val entity: E,
    private val props: List<KMutableProperty1<E, *>> = listOf()
) {
    private val columnValues: MutableList<Pair<String, String>> = props.ifEmpty { entity.properties }
        .filter { it.column.name != "id" }
        .map { prop ->
            prop.column.name to when (val value = prop.getter.call(entity)) {
                is String -> "'$value'"
                is Entity -> value.id
                else -> value
            }.toString()
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

    private var whereStatement: WhereStatement = WhereStatement()

    fun where(conditionBody: WhereCondition?) = this.apply { if (conditionBody != null) whereStatement = WhereStatement(conditionBody) }

    fun execute() {
        updateReferences()
        database.executeSql(getSql().also { Logger.tag("UPDATE").info { it } })
    }

    private fun getSql(): String = "UPDATE ${table.tableName} " +
            "SET " + columnValues.joinToString { it.first + " = " + it.second } +
            whereStatement.getSql()
}
