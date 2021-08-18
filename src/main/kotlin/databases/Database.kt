package databases

import Action
import Column
import Entity
import Reference
import Table
import column
import org.tinylog.Logger
import java.sql.*
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KType

abstract class Database(
    url: String,
    user: String? = null,
    password: String? = null,
    driver: String = "org.postgresql.Driver",
) {
    internal val connection: Connection = DriverManager.getConnection(url, user, password)
    internal abstract val reservedKeyWords: List<String>

    init {
        if (driver.isNotBlank())
            Class.forName(driver)
    }

    internal fun executeSql(sql: String) {
        try {
            connection.createStatement().execute(sql)
        } catch (ex: SQLException) {
            Logger.error { ex }
        }
    }

    internal fun executeQuery(sql: String): ResultSet = connection.createStatement().executeQuery(sql)


    abstract val defaultTypesMap: HashMap<KType, SqlType<*>>
    abstract fun <E: Entity> idColumn(table: Table<E>, prop: KMutableProperty1<E, Int>): Column<E, Int>

    inline fun <reified E : Entity> defaultEntities(noinline entities: () -> List<E>) {
        Table<E>().defaultEntitiesMethod = entities
    }

    inline fun <reified E : Entity, T : Entity> reference(
        prop: KMutableProperty1<E, T?>,
        onDelete: Action = Action.SetDefault
    ) =
        Reference(Table(), prop, onDelete)


    inline fun <reified E : Entity> uniqueColumns(vararg props: KMutableProperty1<E, *>) {
        Table<E>().uniqueColumns.addAll(props.map { it.column.name })
    }


    data class SqlType<T>(val name: String,
                       val customGetValue: ((rs: ResultSet) -> T)? = null,
                       val customSetValue: ((ps: PreparedStatement, index: Int, value: Any?) -> Unit)? = null,
    )
}


