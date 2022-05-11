package databases

import Column
import Config
import Entity
import Reference
import Table
import column
import database
import org.tinylog.Logger
import sql_type_functions.SqlDate
import statements.*
import utils.ifTrue
import utils.timestampType
import java.lang.Thread.sleep
import java.sql.*
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KType

/**
 * Base class for databases.
 *
 * @param[url] A database url of the form jdbc:subprotocol:subname.
 * @param[user] The database user on whose behalf the connection is being made.
 * @param[password] The user's password to the database.
 */
sealed class Database(
    url: String,
    user: String? = null,
    password: String? = null,
    driver: String,
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

    internal open val updateStatementSql: UpdateStatement<*>.() -> String = {
        "UPDATE ${table.tableName} " +
                "SET " + columnValues.joinToString { it.first + " = " + it.second } +
                whereStatement.getSql()
    }
    internal abstract val selectStatementSql: SelectStatement<*>.() -> String
    internal open val insertStatementSql: InsertStatement<*>.(preparedEntities: List<*>) -> String = {
        "INSERT INTO ${table.tableName} " +
                "(${table.columns.filter { it.name != "id" }.joinToString { it.name }}) " +
                "VALUES ${it.joinToString { "(${props.joinToString { "?" }})" }} " +
                "ON CONFLICT DO NOTHING " +
                "RETURNING ${if (getEntity) "*" else "id"}"
    }
    internal open val dropStatementSql: DropStatement<*>.() -> String =
        { "DROP TABLE IF EXISTS ${table.tableName}" }
    internal open val deleteStatementSql: DeleteStatement<*>.() -> String =
        { "DELETE FROM ${table.tableName}" + whereStatement.getSql() }
    internal open val alterStatementSql: AlterStatement<*>.(reference: Reference<*, *>) -> String =
        { "ALTER TABLE ${table.tableName} ADD ${it.getForeignKey()}" }
    internal open val createStatementSql: CreateStatement<*>.() -> String = {
        "CREATE TABLE IF NOT EXISTS ${table.tableName}\n(" +
                table.columns.joinToString(",\n") {
                    "\t" + it.toSql(maxLength)
                } +
                table.checkConditions.joinToString("") { ",\nCHECK (${it(WhereStatement())})" } +
                ",\nCONSTRAINT ${table.tableName}_unique_columns UNIQUE (${table.uniqueProps.joinToString { it.column.name }})"
                    .ifTrue(table.uniqueProps.isNotEmpty()) +
                "\n)"
    }

    /** Gets the specified [sqlDate] from database. */
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

    /** Adds specified entities to the table on its creation. */
    inline fun <reified E : Entity> defaultEntities(noinline entities: () -> List<E>) {
        Table<E>().defaultEntitiesMethod = entities
    }

    inline fun <reified E : Entity, T : Entity?> reference(
        prop: KMutableProperty1<E, T>,
        onDelete: Reference.OnDelete = Reference.OnDelete.SetDefault
    ) =
        Reference(Table(), prop, onDelete)


    /** Marks the set of specified columns unique. */
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


