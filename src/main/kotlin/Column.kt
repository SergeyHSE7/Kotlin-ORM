import databases.Database
import databases.PostgreSQL
import org.tinylog.Logger
import statements.alter
import utils.*
import java.math.BigDecimal
import java.sql.*
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.jvm.javaType

val KMutableProperty1<*, *>.column
    get() = Column.columns[this]!!

@Suppress("UNCHECKED_CAST")
class Reference<E : Entity, P : Entity?>(
    table: Table<E>,
    property: KMutableProperty1<E, P>,
    private val onDelete: Action
) :
Column<E, P>(table, property, database.defaultTypesMap[int4Type] as Database.SqlType<P>) {

    init {
        with(table) {
            if (!references.any { it.property == property }) {
                if (database is PostgreSQL) referencesAddMethods.add { alter().addForeignKey(this@Reference) }
                references.add(this@Reference)
            }
        }
    }

    fun getForeignKey(): String = "FOREIGN KEY (${property.column.name}) REFERENCES " +
            "${(property.returnType.javaType as Class<Entity>).kotlin.simpleName!!.transformCase(Case.Pascal, Case.Snake, true)}(id) " +
            "ON DELETE ${onDelete.name.transformCase(Case.Pascal, Case.Normal).uppercase()}"
}

inline fun <reified E : Entity, T> column(prop: KMutableProperty1<E, T>, sqlTypeName: String) =
    Column(Table(), prop, Database.SqlType(sqlTypeName))

inline fun <reified E : Entity, T> column(prop: KMutableProperty1<E, T>, sqlType: Database.SqlType<T>) =
    Column(Table(), prop, sqlType)

inline fun <reified E : Entity, T> column(prop: KMutableProperty1<E, T>): Column<E, *> = Table<E>().column(prop)

@Suppress("UNCHECKED_CAST")
fun <E: Entity, T> Table<E>.column(prop: KMutableProperty1<E, T>): Column<E, *> {
    if (prop.name == "id") return database.idColumn(this, prop as KMutableProperty1<E, Int>)

    val sqlType = database.defaultTypesMap[prop.type] as Database.SqlType<T>?

    return if (sqlType != null) Column(this, prop, sqlType)
    else Reference(this, prop as KMutableProperty1<E, Entity?>, Action.SetDefault)
}

open class Column<E : Entity, T>(
    table: Table<E>,
    val property: KMutableProperty1<E, T>,
    private val sqlType: Database.SqlType<T>
) {
    @Suppress("UNCHECKED_CAST")
    val refTable by lazy { Table[(property.returnType.javaType as Class<Entity>).kotlin] }
    val name: String = property.name.transformCase(Case.Camel, Case.Snake)
    val fullName: String = table.tableName + "." + name
    private var defaultValue: T? = null
    private var isNotNull = false
    private var isUnique = false
    private var isPrimaryKey = false

    internal val getValue: (rs: ResultSet, name: String) -> T = sqlType.customGetValue ?: ::defaultGetValue
    @Suppress("UNCHECKED_CAST")
    internal val setValue: (ps: PreparedStatement, index: Int, value: Any?) -> Unit =
        (sqlType.customSetValue ?: ::defaultSetValue) as (PreparedStatement, Int, Any?) -> Unit

    init {
        if (!table.columns.any { it.property == property }) {
            if (name in database.reservedKeyWords)
                throw LoggerException("\"$name\" is a reserved SQL keyword!")
            table.columns.add(this)
            columns[property] = this
            if (!property.returnType.isMarkedNullable)
                isNotNull = true
        }
    }

    fun notNull() = this.also {
        isNotNull = true
        if (property.returnType.isMarkedNullable)
            Logger.warn { "Nullable value shouldn't be marked as not null! (column: $name)" }
    }

    fun unique() = this.also { isUnique = true }
    internal fun primaryKey() = this.also { isPrimaryKey = true }
    fun default(value: T) = this.also { defaultValue = value }

    protected open fun attributesToSql(): String = "PRIMARY KEY ".ifTrue(isPrimaryKey) +
            "NOT NULL ".ifTrue(isNotNull) +
            "UNIQUE ".ifTrue(isUnique) +
            "DEFAULT $defaultValue".ifTrue(defaultValue != null && defaultValue !is String) +
            "DEFAULT '$defaultValue'".ifTrue(defaultValue != null && defaultValue is String)


    fun toSql(nameLength: Int = name.length): String =
        "${name.padEnd(nameLength)} ${sqlType.name.uppercase()} ${attributesToSql()}".trim()

    companion object {
        internal val columns = HashMap<KMutableProperty1<*, *>, Column<*, *>>()
    }
}

@Suppress("UNCHECKED_CAST")
private fun <E : Entity, T> Column<E, T>.defaultGetValue(rs: ResultSet, name: String): T =
    if (refTable != null) rs.getInt(name) as T
    else when (property.type) {
        decimalType -> rs.getBigDecimal(name)
        stringType -> rs.getString(name)
        int8Type -> rs.getLong(name)
        int4Type -> rs.getInt(name)
        int2Type -> rs.getShort(name)
        doubleType -> rs.getDouble(name)
        floatType -> rs.getFloat(name)
        boolType -> rs.getBoolean(name)
        dateType -> rs.getDate(name)
        timestampType -> rs.getTimestamp(name)
        timeType -> rs.getTime(name)
        else -> throw LoggerException("Unknown type: $fullName - ${property.type}")
    } as T


private fun defaultSetValue(ps: PreparedStatement, index: Int, value: Any?) =
    when (value) {
        is String -> ps.setString(index, value)
        is BigDecimal -> ps.setBigDecimal(index, value)
        is Long -> ps.setLong(index, value)
        is Int -> ps.setInt(index, value)
        is Short -> ps.setShort(index, value)
        is Boolean -> ps.setBoolean(index, value)
        is Float -> ps.setFloat(index, value)
        is Double -> ps.setDouble(index, value)
        is Date -> ps.setDate(index, value)
        is Time -> ps.setTime(index, value)
        is Timestamp -> ps.setTimestamp(index, value)
        null -> ps.setNull(index, 4)
        else -> throw LoggerException("Unknown type: $value")
    }
