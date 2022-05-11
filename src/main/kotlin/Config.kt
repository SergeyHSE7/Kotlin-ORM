import databases.Database
import databases.SQLite
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonBuilder
import statements.create


internal val database: Database
    get() = Config.database ?: throw LoggerException("First you need to set database property in config method!")

/** [Json] instance with parameters applied from the config method. */
val json: Json by lazy { Json(builderAction = Config.jsonFormat) }

/** Function for setting configuration values and generating tables. */
fun config(func: Config.() -> Unit): Unit = Config.apply(func).run {
    val loadRefs = alwaysLoadReferencesWhenAddingEntity
    alwaysLoadReferencesWhenAddingEntity = false
    with(tables()) {
        if (refreshTables) forEach(Table<*>::dropTable)
        forEach(Table<*>::initTable)
        forEach(Table<*>::create)
        forEach { it.defaultEntities.save() }
        forEach { it.referencesAddMethods.forEach { it() } }
    }
    if (database is SQLite) database!!.executeSql("PRAGMA foreign_keys = ON")
    alwaysLoadReferencesWhenAddingEntity = loadRefs
}

/** Object containing all config values */
object Config {
    /** Database instance. */
    var database: Database? = null

    /** Number of attempts to connect to the database. */
    var connectionAttemptsAmount: Int = 3

    /** Delay between attempts of connecting to the database. */
    var connectionAttemptsDelay: Long = 3000

    /** Cache size for storing the latest received entries. */
    var maxCacheSize: Int = 10

    /** Default number of entities receiving per request when creating sequence. */
    var sequenceWindowSize: Int = 10

    /** Should the tables be recreated when the code is restarted */
    var refreshTables: Boolean = false

    /** Should references be loaded by default. */
    var loadReferencesByDefault: Boolean = true

    var alwaysLoadReferencesWhenAddingEntity: Boolean = true

    /** Config values for [JsonBuilder]. */
    var jsonFormat: JsonBuilder.() -> Unit = { }

    /** Specifies all instances of database tables. */
    fun setTables(vararg tables: () -> Table<*>) {
        this.tables = { tables.map { it() } }
    }
    internal var tables: () -> List<Table<*>> = { listOf() }
}
