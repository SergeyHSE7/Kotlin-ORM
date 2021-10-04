package databases

import Action
import Column
import Entity
import Reference
import Table
import org.tinylog.Logger
import java.sql.*
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KType

sealed class Database(
    url: String,
    user: String? = null,
    password: String? = null,
    driver: String = "org.postgresql.Driver",
) {
    internal val connection: Connection = DriverManager.getConnection(url, user, password)
    private val openedStatements = mutableListOf<Statement>()
    private val newStatement: Statement
        get() = connection.createStatement().also { openedStatements.add(it) }

    internal fun closeAllStatements() {
        openedStatements.forEach { it.close() }
        openedStatements.clear()
    }

    internal abstract val reservedKeyWords: List<String>

    init {
        if (driver.isNotBlank())
            Class.forName(driver)
    }

    internal fun executeSql(sql: String) {
        try {
            newStatement.execute(sql)
        } catch (ex: SQLException) {
            Logger.error { ex }
        }
    }

    internal fun executeQuery(sql: String): ResultSet = newStatement.executeQuery(sql)


    abstract val defaultTypesMap: HashMap<KType, SqlType<*>>
    abstract fun <E : Entity> idColumn(table: Table<E>, prop: KMutableProperty1<E, Int>): Column<E, Int>

    inline fun <reified E : Entity> defaultEntities(noinline entities: () -> List<E>) {
        Table<E>().defaultEntitiesMethod = entities
    }

    inline fun <reified E : Entity, T : Entity?> reference(
        prop: KMutableProperty1<E, T>,
        onDelete: Action = Action.SetDefault
    ) =
        Reference(Table(), prop, onDelete)


    inline fun <reified E : Entity> uniqueColumns(vararg props: KMutableProperty1<E, *>) {
        Table<E>().uniqueProps.addAll(props)
    }


    data class SqlType<T>(
        val name: String,
        val customGetValue: ((rs: ResultSet, name: String) -> T)? = null,
        val customSetValue: ((ps: PreparedStatement, index: Int, value: T) -> Unit)? = null,
    )
}


