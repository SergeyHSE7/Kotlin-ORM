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
            url = "jdbc:postgresql://localhost:5432/FinAssistant",
            user = "postgres",
            password = "123456"
        ),
        SQLite(url = "jdbc:sqlite:C:\\SQLite3\\test_db.sqlite"),
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
            "UPDATE" - { updateTests() }
            "References" - { referenceTests() }

            "Json Print" - { jsonPrintTests() }

            "Table Operations" - { tableOperationsTests() }

            Table.tables.clear()
            Column.columns.clear()
        }
    }
})


