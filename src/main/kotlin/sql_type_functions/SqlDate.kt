package sql_type_functions

import database
import databases.MariaDB
import databases.SQLite

class SqlDate(value: String) : SqlBase(value) {

    companion object {
        fun nowWithMs() = SqlDate(when (database) {
                is SQLite -> "STRFTIME('%Y-%m-%d %H:%M:%f', 'now', 'localtime')"
                is MariaDB -> "now(3)"
                else -> "now()"
        })
        fun now() = SqlDate(if (database is SQLite) "datetime('now', 'localtime')" else "now()")
    }
}
