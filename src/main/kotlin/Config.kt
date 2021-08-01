
val database: Database
    get() = Config.database ?: throw LoggerException("First you need to set database property in config method!")


fun config(func: Config.() -> Unit): Unit = Config.apply(func).run {
    val loadRefs = alwaysLoadReferencesWhenAddingEntity
    alwaysLoadReferencesWhenAddingEntity = false
    with (tables()) {
        forEach { it.defaultEntities.save() }
        forEach { it.referencesAddMethods.forEach { it() } }
    }
    alwaysLoadReferencesWhenAddingEntity = loadRefs
}

object Config {
    internal var database: Database? = null

    internal var maxCacheSize: Int = 10

    internal var refreshTables: Boolean = false

    internal var loadReferencesByDefault: Boolean = true

    internal var alwaysLoadReferencesWhenAddingEntity: Boolean = true

    internal var tables: () -> List<Table<*>> = { listOf() }

}