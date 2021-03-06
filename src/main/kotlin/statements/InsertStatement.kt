package statements

import Entity
import Table
import column
import database
import org.tinylog.Logger
import utils.getEntity
import utils.map
import java.sql.PreparedStatement

fun <E : Entity> Table<E>.insert(vararg insertEntities: E) = insert(insertEntities.toList())
fun <E : Entity> Table<E>.insert(insertEntities: List<E>) = InsertStatement(this, insertEntities)

class InsertStatement<E : Entity>(val table: Table<E>, insertEntities: List<E> = listOf()) {
    val props = table.columns.map { it.property }.filter { it.name != "id" }
    var getEntity = false
    private val entities: MutableList<E> = insertEntities.toMutableList()

    fun getId(): Int? = getIds().firstOrNull()
    fun getIds(): List<Int> = getPreparedStatement().executeQuery().map { getInt("id").also { table.cache.remove(it) } }

    fun getEntity(): E {
        getEntity = true
        return getPreparedStatement(listOf(entities.removeAt(0))).executeQuery()
            .getEntity(table, lazy = true).also { table.cache.add(it, withReferences = false) }
    }

    fun getEntities(): List<E> {
        getEntity = true
        return try {
            getPreparedStatement().executeQuery().map { getEntity(table, lazy = true) }
        } catch (ex: Exception) {
            val list = mutableListOf<E?>()
            kotlin.runCatching { list.add(getEntity()) }
            list.mapNotNull { it }
        }.also { table.cache.addAll(it, withReferences = false) }
    }

    private fun getPreparedStatement(preparedEntities: List<E> = entities): PreparedStatement = with(database) {
        connection.prepareStatement(insertStatementSql(preparedEntities)).apply {
                entities.flatMap { entity ->
                    props.map { prop ->
                        prop.column to (when (val value = prop.get(entity)) {
                            is Entity -> value.id
                            else -> value
                        })
                    }
                }.forEachIndexed { index, (column, value) -> column.setValue(this, index + 1, value) }
                Logger.tag("INSERT").info { toString() }
            }
    }
}
