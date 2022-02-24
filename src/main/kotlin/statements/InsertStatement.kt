package statements

import Entity
import Table
import column
import database
import databases.MariaDB
import org.tinylog.Logger
import utils.getEntity
import utils.ifTrue
import utils.map
import java.sql.PreparedStatement

fun <E : Entity> Table<E>.insert(vararg insertEntities: E) = insert(insertEntities.toList())
fun <E : Entity> Table<E>.insert(insertEntities: List<E>) = InsertStatement(this, insertEntities)

class InsertStatement<E : Entity>(private val table: Table<E>, insertEntities: List<E> = listOf()) {
    private val props = table.columns.map { it.property }.filter { it.name != "id" }
    private var getEntity = false
    private val entities: MutableList<E> = insertEntities.toMutableList()

    fun add(objects: List<E>) = this.apply { entities.addAll(objects) }
    fun add(vararg objects: E) = this.apply { entities.addAll(objects) }

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

    fun getSql(preparedEntities: List<E> = entities): String =
        "INSERT ${"IGNORE ".ifTrue(database is MariaDB)}INTO ${table.tableName} " +
                "(${table.columns.filter { it.name != "id" }.joinToString { it.name }}) " +
                "VALUES ${preparedEntities.joinToString { "(${props.joinToString { "?" }})" }} " +
                "ON CONFLICT DO NOTHING ".ifTrue(database !is MariaDB) +
                "RETURNING ${if (getEntity) "*" else "id"}"

    private fun getPreparedStatement(preparedEntities: List<E> = entities): PreparedStatement =
        database.connection.prepareStatement(getSql(preparedEntities))
            .apply {
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
