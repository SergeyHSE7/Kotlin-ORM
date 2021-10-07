import databases.MariaDB
import databases.PostgreSQL
import databases.SQLite
import entities.*
import io.kotest.core.spec.style.FreeSpec

class Tests : FreeSpec({
    val tables = {
        listOf(
            UserBooksTable,
            UsersTable,
            AddressesTable,
            BooksTable,
            TestTable
        )
    }

    val databases = listOf(
        PostgreSQL(
            url = System.getenv("postgresql_url"),
            user = System.getenv("postgresql_user"),
            password = System.getenv("postgresql_password")
        ),
        SQLite(url = System.getenv("sqlite_url")),
        MariaDB(
            url = System.getenv("mariadb_url"),
            user = System.getenv("mariadb_user"),
            password = System.getenv("mariadb_password")
        )
    )

    config {
        refreshTables = true
        jsonFormat = {
            encodeDefaults = true
        }
    }

    include(utilsTests())

    databases.forEach { database ->
        "${database::class.simpleName} Tests" - {
            "Generate tables" {
                config {
                    this.database = database
                    this.tables = tables
                }
            }
            "Table Methods" - { tableMethodsTests() }

            "INSERT" - { insertTests() }
            "SELECT" - { selectTests() }
            "WHERE" - { whereTests() }
            "UPDATE" - { updateTests() }
            "References" - { referenceTests() }

            if (database !is SQLite) "Sequences" - { sequenceTests() }

            "Json Print" - { jsonPrintTests() }

            "Table Operations" - { tableOperationsTests() }

            Table.tables.clear()
            Column.columns.clear()
        }
    }
})


