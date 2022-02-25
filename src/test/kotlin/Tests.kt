import databases.MariaDB
import databases.PostgreSQL
import databases.SQLite
import entities.*
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import java.sql.SQLException

class Tests : FreeSpec({
    val databases = listOf(
        PostgreSQL(url = "jdbc:postgresql://localhost:5432/test_db", user = "user", password = "password"),
        SQLite(url = System.getenv("sqlite_url")),
        MariaDB(url = "jdbc:mariadb://localhost:3306/test_db", user = "root", password = "password")
    )

    with(Config) {
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

            "Table Operations" - {
                val testTable = Table<Test>()
                "Clear table" {
                    testTable.clearTable()
                    testTable.size shouldBe 0
                }
                "Drop table" {
                    testTable.dropTable()
                    shouldThrow<SQLException> {
                        testTable.getAll()
                    }
                }
            }

            Table.tables.clear()
            Column.columns.clear()
        }
    }
})


