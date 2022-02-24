package databases

import Action
import Column
import Config
import Entity
import Reference
import Table
import database
import org.tinylog.Logger
import sql_type_functions.SqlDate
import statements.Expression
import statements.WhereCondition
import statements.WhereStatement
import utils.timestampType
import java.lang.Thread.sleep
import java.sql.*
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KType

sealed class Database(
    url: String,
    user: String? = null,
    password: String? = null,
    driver: String = "org.postgresql.Driver",
) {
    internal val connection: Connection = run {
        var count = 0
        while (count++ < Config.connectionAttemptsAmount) {
            runCatching { DriverManager.getConnection(url, user, password) }.onSuccess { return@run it }
            sleep(Config.connectionAttemptsDelay)
        }
        throw Exception("Can't get connection  after $count attempts")
    }

    private val openedStatements = mutableListOf<Statement>()
    private val newStatement: Statement
        get() = connection.createStatement().also { openedStatements.add(it) }

    internal fun closeAllStatements() {
        openedStatements.forEach { it.close() }
        openedStatements.clear()
    }

    internal abstract val reservedKeyWords: List<String>

    fun select(sqlDate: SqlDate): Timestamp {
        val rs = database.executeQuery("SELECT $sqlDate").apply { next() }
        Logger.tag("SELECT").info { "SELECT $sqlDate" }
        return (defaultTypesMap[timestampType]!!.customGetValue?.invoke(rs, 1)
            ?: rs.getObject(1)) as Timestamp
    }

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


    internal abstract val defaultTypesMap: HashMap<KType, SqlType<*>>
    internal abstract fun <E : Entity> idColumn(table: Table<E>, prop: KMutableProperty1<E, Int>): Column<E, Int>

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


    inline fun <reified E : Entity, P : KMutableProperty1<E, *>> check(
        property: P,
        crossinline condition: WhereStatement.(P) -> Expression
    ) =
        this.also { Table<E>().checkConditions.add { condition(property) } }

    inline fun <reified E : Entity> check(crossinline condition: WhereCondition) =
        this.also { Table<E>().checkConditions.add { condition() } }


    data class SqlType<T>(
        val name: String,
        val customGetValue: ((rs: ResultSet, index: Int) -> T)? = null,
        val customSetValue: ((ps: PreparedStatement, index: Int, value: T) -> Unit)? = null,
    )
}


