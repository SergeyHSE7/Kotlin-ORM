package sql_type_functions

import database
import databases.SQLite

class SqlDate(value: String) : SqlBase(value) {

    companion object {
        fun now() = SqlDate(if (database is SQLite) "datetime('now', 'localtime')" else "now()")
    }
}
