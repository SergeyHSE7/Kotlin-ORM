import Config.tables

val database: Database
    get() = Config.database ?: throw LoggerException("First you need to set database property in config method!")


fun config(func: Config.() -> Unit) = Config.apply(func).also {
    val tables = tables()
    tables.forEach { it.defaultEntities.save() }
    tables.forEach { it.referencesAddMethods.forEach { it() } }
}

object Config {
    internal var database: Database? = null

    internal var maxCacheSize: Int = 10

    internal var refreshTables: Boolean = false

    internal var tables: () -> List<Table<*>> = { listOf() }

}
