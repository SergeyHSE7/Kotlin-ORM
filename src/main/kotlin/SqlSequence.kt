import databases.MariaDB
import databases.PostgreSQL
import databases.SQLite
import org.tinylog.Logger
import utils.ifTrue
import utils.map

data class SqlSequence(
    val name: String,
    private val start: Long = 0,
    private val increment: Long = 1,
    private val minValue: Long? = null,
    private val maxValue: Long? = null,
    private val isCycle: Boolean = false
) {

    init {
        if (database is SQLite)
            throw LoggerException("Sequences for SqlLite are not implemented yet!")
        if (Config.refreshTables) drop()
        database.executeSql(createSql().also { Logger.tag("CREATE").info { it } })
    }

    private fun createSql() = "CREATE SEQUENCE IF NOT EXISTS $name INCREMENT $increment" +
            " MINVALUE $minValue".ifTrue(minValue != null) +
            " MAXVALUE $maxValue".ifTrue(maxValue != null) +
            " START $start" + " CYCLE".ifTrue(isCycle)

    private fun dropSql() = "DROP SEQUENCE IF EXISTS $name" + " CASCADE".ifTrue(database is PostgreSQL)
    fun drop() = database.executeSql(dropSql().also { Logger.tag("DROP").info { it } })

    private fun nextvalSql() = "SELECT nextval" + when (database) {
        is PostgreSQL -> "('$name')"
        is MariaDB -> "($name)"
        else -> ""
    }

    fun nextValue(): Int = database.executeQuery(nextvalSql().also { Logger.tag("SELECT").info { it } })
        .map {
            when (database) {
                is PostgreSQL -> getInt("nextval")
                is MariaDB -> getInt("nextval($name)")
                else -> 0
            }
        }.first()


    private fun lastvalSql() = "" + when (database) {
        is PostgreSQL -> "SELECT last_value FROM $name"
        is MariaDB -> "SELECT lastval($name)"
        else -> ""
    }

    fun lastValue(): Int = database.executeQuery(lastvalSql().also { Logger.tag("SELECT").info { it } })
        .map {
            when (database) {
                is PostgreSQL -> getInt("last_value")
                is MariaDB -> getInt("lastval($name)")
                else -> 0
            }
        }.first()


    fun restart(restartValue: Long = start) {
        if (minValue != null && restartValue < minValue || maxValue != null && restartValue > maxValue)
            throw IllegalArgumentException("Restart value must be in allowed range (minValue <= restartValue <= maxValue)")
        database.executeSql("ALTER SEQUENCE IF EXISTS $name RESTART $restartValue"
            .also { Logger.tag("ALTER").info { it } })
    }
}
