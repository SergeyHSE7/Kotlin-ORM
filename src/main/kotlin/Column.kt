@file:Suppress("UNCHECKED_CAST")

import databases.Database
import databases.MariaDB
import databases.PostgreSQL
import org.tinylog.Logger
import statements.Expression
import statements.WhereStatement
import statements.alter
import utils.*
import java.sql.PreparedStatement
import java.sql.ResultSet
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.jvm.javaType

val KMutableProperty1<*, *>.column
    get() = Column.columns[this]!!

class Reference<E : Entity, P : Entity?>(
    table: Table<E>,
    property: KMutableProperty1<E, P>,
    private val onDelete: Action
) :
    Column<E, P>(table, property, database.defaultTypesMap[int4Type] as Database.SqlType<P>) {

    init {
        with(table) {
            if (!references.any { it.property == property }) {
                if (database is PostgreSQL || database is MariaDB)
                    referencesAddMethods.add { alter().addForeignKey(this@Reference) }
                references.add(this@Reference)
            }
        }
    }

    fun getForeignKey(): String = "FOREIGN KEY (${property.column.name}) REFERENCES " +
            "${
                (property.returnType.javaType as Class<Entity>).kotlin.simpleName!!
                    .transformCase(Case.Pascal, Case.Snake, true)
            }(id) " +
            "ON DELETE ${onDelete.name.transformCase(Case.Pascal, Case.Normal).uppercase()}"
}

inline fun <reified E : Entity, T> column(prop: KMutableProperty1<E, T>, sqlTypeName: String) =
    column(prop, Database.SqlType(sqlTypeName))

inline fun <reified E : Entity, T> column(prop: KMutableProperty1<E, T>, sqlType: Database.SqlType<T>) =
    Column(Table(), prop, sqlType)

inline fun <reified E : Entity, T> column(prop: KMutableProperty1<E, T>): Column<E, *> = Table<E>().column(prop)

fun <E : Entity, T> Table<E>.column(prop: KMutableProperty1<E, T>): Column<E, *> {
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
    val refTable by lazy { Table[(property.returnType.javaType as Class<Entity>).kotlin] }
    val name: String = property.name.transformCase(Case.Camel, Case.Snake)
    val tableName: String = table.tableName
    val fullName: String = "$tableName.$name"
    private var defaultValue: T? = property.get(table.defaultEntity)
    private var isNotNull = false
    private var isUnique = false
    private var isPrimaryKey = false
    private val checkConditions = table.checkConditions

    internal val getValue: (rs: ResultSet, index: Int) -> T = sqlType.customGetValue ?: { rs, index ->
        when {
            property.isTypeOf(floatType) -> rs.getFloat(index)
            property.isTypeOf(int2Type) -> rs.getShort(index)
            else -> rs.getObject(index)
        } as T }

    internal val setValue: (ps: PreparedStatement, index: Int, value: Any?) -> Unit =
        (sqlType.customSetValue ?: { ps, index, value ->
            ps.setObject(index, value)
        }) as (PreparedStatement, Int, Any?) -> Unit

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
    fun check(condition: WhereStatement.(KMutableProperty1<E, T>) -> Expression) =
        this.also { checkConditions.add { condition(property) } }

    protected open fun attributesToSql(): String = "PRIMARY KEY ".ifTrue(isPrimaryKey) +
            "NOT NULL ".ifTrue(isNotNull) +
            "UNIQUE ".ifTrue(isUnique) +
            "DEFAULT ${defaultValue.toSql()}".ifTrue(!isPrimaryKey && defaultValue != null)


    fun toSql(nameLength: Int = name.length): String =
        "${name.padEnd(nameLength)} ${sqlType.name.uppercase()} ${attributesToSql()}".trim()

    companion object {
        internal val columns = HashMap<KMutableProperty1<*, *>, Column<*, *>>()
    }
}
