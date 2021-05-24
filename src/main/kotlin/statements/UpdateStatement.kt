package statements

import Entity
import Table
import database
import returnValue
import utils.Case
import utils.transformCase
import kotlin.reflect.KMutableProperty1

fun <E : Entity> Table<E>.update(entity: E, vararg props: KMutableProperty1<E, *>) =
    UpdateStatement(this, entity, props.toList()).where { "id = ${entity.id}" }.execute()

class UpdateStatement<E : Entity>(
    private val table: Table<E>,
    entity: E,
    props: List<KMutableProperty1<E, *>> = listOf()
) {
    private val columnValues: List<Pair<String, String>> = props.ifEmpty { entity.properties }
        .map { prop ->
            prop.name.transformCase(Case.Camel, Case.Snake) to
                    if (prop.returnValue(entity) is String) "'${prop.returnValue(entity)}'"
                    else prop.returnValue(entity).toString()
        }.filter { it.first != "id" }

    private var whereStatement: WhereStatement = WhereStatement()

    fun where(conditionBody: WhereCondition) = this.apply { whereStatement.addCondition(conditionBody) }

    fun execute() {
        database.connection.createStatement().execute(getSql()).also { println(getSql()) }
    }

    fun getSql(): String = "UPDATE ${table.tableName} " +
            "SET " + columnValues.joinToString { it.first + " = " + it.second } +
            whereStatement.getSql()
}
