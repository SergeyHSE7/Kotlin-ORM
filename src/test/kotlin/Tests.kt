import databases.Database
import databases.PostgreSQL
import databases.SQLite
import entities.*
import io.kotest.core.spec.style.FreeSpec

class Tests : FreeSpec({
    val databases = mapOf<Database, () -> List<Table<*>>>(
        PostgreSQL(
            url = "jdbc:postgresql://localhost:5432/FinAssistant",
            user = "postgres",
            password = "123456"
        ) to { listOf(UserBooksTable, UsersTable, AddressesTable, BooksTable, TestTable) },
    )

    config {
        refreshTables = true
        jsonFormat = {
            encodeDefaults = true
        }
    }

    include(utilsTests())

    val db = SQLite("jdbc:sqlite:C:\\SQLite3\\test_db.sqlite")
    db.executeSql("CREATE TABLE test_table(id INTEGER)")

    databases.forEach { (database, tables) ->
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


