import org.tinylog.Logger
import statements.alter
import utils.Case
import utils.ifTrue
import utils.transformCase
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.jvm.javaType

val KMutableProperty1<*, *>.column
    get() = Column[this]

class Reference<E : Entity, P : Entity?>(
    table: Table<E>,
    property: KMutableProperty1<E, P>,
    private val onDelete: Action
) :
    Column<E, P>(table, property, "integer") {

    init {
        with(table) {
            if (!references.any { it.property == property }) {
                referencesAddMethods.add { alter().addForeignKey(property, refTable!!, onDelete) }
                references.add(this@Reference)
            }
        }
    }
}

open class Column<E : Entity, T>(
    table: Table<E>,
    val property: KMutableProperty1<E, T>,
    val sqlType: String
) {
    @Suppress("UNCHECKED_CAST")
    val refTable by lazy { Table[(property.returnType.javaType as Class<Entity>).kotlin] }
    val name: String = property.name.transformCase(Case.Camel, Case.Snake)
    val fullName: String = table.tableName + "." + name
    private var defaultValue: T? = null
    private var isNotNull = false
    private var isUnique = false
    private var isPrimaryKey = false

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
    fun primaryKey() = this.also { isPrimaryKey = true }
    fun default(value: T) = this.also { defaultValue = value }

    protected open fun attributesToSql(): String = "PRIMARY KEY ".ifTrue(isPrimaryKey) +
            "NOT NULL ".ifTrue(isNotNull) +
            "UNIQUE ".ifTrue(isUnique) +
            "DEFAULT $defaultValue".ifTrue(defaultValue != null && defaultValue !is String) +
            "DEFAULT '$defaultValue'".ifTrue(defaultValue != null && defaultValue is String)


    fun toSql(nameLength: Int = name.length): String =
        "${name.padEnd(nameLength)} ${sqlType.uppercase()} ${attributesToSql()}".trim()

    companion object {
        val columns = HashMap<KMutableProperty1<*, *>, Column<*, *>>()

        internal operator fun get(prop: KMutableProperty1<*, *>) = columns[prop]!!
    }
}
