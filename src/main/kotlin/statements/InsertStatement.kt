package statements

import Entity
import Table
import database
import utils.getEntity
import utils.map
import utils.set
import java.sql.PreparedStatement

fun <E: Entity> Table<E>.insert(vararg insertEntities: E) = insert(insertEntities.toList())
fun <E: Entity> Table<E>.insert(insertEntities: List<E>) = InsertStatement(this, insertEntities)

class InsertStatement<E : Entity>(private val table: Table<E>, insertEntities: List<E> = listOf()) {
    private val props = table.columns.map { it.property }.filter { it.name != "id" }
    private var getEntity = false
    private val entities: MutableList<E> = insertEntities.toMutableList()

    fun add(objects: List<E>) = this.apply { entities.addAll(objects) }
    fun add(vararg objects: E) = this.apply { entities.addAll(objects) }

    fun getId(): Int? = getIds().firstOrNull()
    fun getIds(): List<Int> = getPreparedStatement().executeQuery().map { getInt("id") }
    private fun getEntity(): E? {
        getEntity = true
        return getPreparedStatement(listOf(entities.removeAt(0))).executeQuery()
            .getEntity(table)
    }

    fun getEntities(): List<E> {
        getEntity = true
        return try {
            getPreparedStatement().executeQuery().map { getEntity(table) }
        } catch (ex: Exception) {
            val list = mutableListOf<E?>()
            kotlin.runCatching { list.add(getEntity()) }
            list.mapNotNull { it }
        }
    }

    fun getSql(preparedEntities: List<E> = entities): String =
        "INSERT INTO ${table.tableName} (${table.columns.filter { it.name != "id" }.joinToString { it.name }}) " +
                "VALUES ${preparedEntities.joinToString { "(${props.joinToString { "?" }})" }} " +
                "ON CONFLICT DO NOTHING " +
                "RETURNING ${if (getEntity) "*" else "id"}"

    private fun getPreparedStatement(preparedEntities: List<E> = entities): PreparedStatement =
        database.connection.prepareStatement(getSql(preparedEntities))
            .apply {
                set(entities.flatMap { entity -> props.map {
                    val value = it.get(entity)
                    if (value is Entity)
                        value.id
                    else value
                } })
                println(toString())
            }
}
