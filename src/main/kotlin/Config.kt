import databases.Database
import databases.SQLite
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonBuilder


val database: Database
    get() = Config.database ?: throw LoggerException("First you need to set database property in config method!")

val json: Json by lazy { Json(builderAction = Config.jsonFormat) }

fun config(func: Config.() -> Unit): Unit = Config.apply(func).run {
    val loadRefs = alwaysLoadReferencesWhenAddingEntity
    alwaysLoadReferencesWhenAddingEntity = false
    with(tables()) {
        forEach(Table<*>::createTable)
        forEach { it.defaultEntities.save() }
        forEach { it.referencesAddMethods.forEach { it() } }
    }
    if (database is SQLite) database!!.executeSql("PRAGMA foreign_keys = ON")
    alwaysLoadReferencesWhenAddingEntity = loadRefs
}

object Config {
    var database: Database? = null

    var connectionAttemptsAmount: Int = 3

    var connectionAttemptsDelay: Long = 3000

    var maxCacheSize: Int = 10

    var sequenceWindowSize: Int = 10

    var refreshTables: Boolean = false

    var loadReferencesByDefault: Boolean = true

    var alwaysLoadReferencesWhenAddingEntity: Boolean = true

    var tables: () -> List<Table<*>> = { listOf() }

    fun setTables(vararg tables: () -> Table<*>) {
        this.tables = { tables.map { it() } }
    }

    var jsonFormat: JsonBuilder.() -> Unit = { }

}
