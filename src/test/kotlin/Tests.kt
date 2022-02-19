import databases.MariaDB
import databases.PostgreSQL
import databases.SQLite
import entities.*
import io.kotest.core.spec.style.FreeSpec

class Tests : FreeSpec({
    val databases = listOf(
        PostgreSQL(url = "jdbc:postgresql://localhost:5432/test_db", user = "user", password = "password"),
        SQLite(url = System.getenv("sqlite_url")),
        MariaDB(url = "jdbc:mariadb://localhost:3306/test_db", user = "root", password = "password")
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
                    setTables(::UserBooksTable, ::UsersTable, ::AddressesTable, ::BooksTable, ::TestTable)
                }
            }
            "SQL Functions" - { sqlFunctionsTests() }

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


