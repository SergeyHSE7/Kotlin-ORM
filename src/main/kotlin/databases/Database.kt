package databases

import Action
import Column
import Entity
import Reference
import Table
import autoColumn
import column
import org.tinylog.Logger
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.SQLException
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KType

abstract class Database(
    url: String,
    user: String,
    password: String,
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


    abstract val columnTypesMap: HashMap<KType, String>
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

    inline fun <reified E : Entity, T : Boolean?> bool(prop: KMutableProperty1<E, T>) = autoColumn(prop)
    inline fun <reified E : Entity, T : Short?> int2(prop: KMutableProperty1<E, T>) = autoColumn(prop)
    inline fun <reified E : Entity, T : Int?> int4(prop: KMutableProperty1<E, T>) = autoColumn(prop)
    inline fun <reified E : Entity, T : Long?> int8(prop: KMutableProperty1<E, T>) = autoColumn(prop)
    inline fun <reified E : Entity, T : Double?> double(prop: KMutableProperty1<E, T>) = autoColumn(prop)

}


