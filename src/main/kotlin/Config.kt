import java.util.*

val database: Database
    get() = Config.database ?: throw LoggerException("First you need to set database property in config method!")


fun config(func: Config.() -> Unit) {
    func(Config)
}

object Config {
    var database: Database? = null
        internal set

    var maxCacheSize: Int = 10
        internal set

    var refreshTables: Boolean = false
        internal set

    internal var tables: List<Table<*>> = listOf()
        set(value) {
            field = value
            field.forEach { it.defaultEntities.save() }
            field.forEach { it.referencesAddMethods.forEach { it() } }
        }
}
