package statements

import Entity
import Table
import database
import returnValue
import utils.Case
import utils.transformCase
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.jvm.internal.impl.resolve.calls.inference.CapturedType

fun <E : Entity> Table<out E>.update(entity: E, vararg props: KMutableProperty1<E, *>) =
    UpdateStatement(this, entity, props.toList()).where { "id = ${entity.id}" }.execute()

class UpdateStatement<E : Entity>(
    private val table: Table<out E>,
    val entity: E,
    private val props: List<KMutableProperty1<E, *>> = listOf()
) {
    private val columnValues: List<Pair<String, String>> = props.ifEmpty { entity.properties }
        .filter { it.returnValue(entity) !is Entity }
        .map { prop ->
            prop.name.transformCase(Case.Camel, Case.Snake) to
                    if (prop.returnValue(entity) is String) "'${prop.returnValue(entity)}'"
                    else prop.returnValue(entity).toString()
        }.filter { it.first != "id" }


    private fun updateReferences() {
        table.columns.filter { it.refTable != null && (props.isEmpty() || props.contains(it.property)) }
            .forEach {
                val refEntity = it.property.returnValue(entity) as Entity
                it.refTable!!.update(refEntity)
            }
    }

    private var whereStatement: WhereStatement = WhereStatement()

    fun where(conditionBody: WhereCondition) = this.apply { whereStatement.addCondition(conditionBody) }

    fun execute() {
        database.connection.createStatement().execute(getSql()).also {
            updateReferences()
            println(getSql())
        }
    }

    fun getSql(): String = "UPDATE ${table.tableName} " +
            "SET " + columnValues.joinToString { it.first + " = " + it.second } +
            whereStatement.getSql()
}
