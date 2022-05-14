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

            "Date Types" - {
                val tests = Table<Test>()
                val test = tests.first()!!

                println("\njava.sql:")
                println(test.timestampValue)
                println(test.dateValue)
                println(test.timeValue)

                println("\njava.util:")
                println(test.calendarValue.time)
                println(test.instantValue)
                println(test.localDateTimeValue)
                println(test.localDateValue)
                println(test.localTimeValue)

                println("\nkotlinx.datetime:")
                println(test.ktInstantValue)
                println(test.ktLocalDateTimeValue)
                println(test.ktLocalDateValue)
            }
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


