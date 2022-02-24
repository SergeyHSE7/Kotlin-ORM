package sql_type_functions

import database
import databases.MariaDB
import databases.PostgreSQL
import databases.SQLite

class SqlDate(value: String) : SqlBase(value) {
    fun getFromDB() = database.select(this)

    companion object {
        fun nowWithMs() = SqlDate(
            when (database) {
                is SQLite -> "(STRFTIME('%Y-%m-%d %H:%M:%f', 'now'))"
                is MariaDB -> "now(3)"
                is PostgreSQL -> "(now() at time zone 'utc')"
                else -> "now()"
            }
        )

        fun now() = SqlDate(
            when (database) {
                is SQLite -> "(datetime('now'))"
                is MariaDB -> "now()"
                is PostgreSQL -> "(now() at time zone 'utc')"
                else -> "now()"
            }
        )
    }
}
