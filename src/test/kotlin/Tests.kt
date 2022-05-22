import databases.MariaDB
import databases.PostgreSQL
import databases.SQLite
import entities.*
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import statements.delete
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
                    setTables(::UserBooksTable, ::UsersTable, ::AddressesTable,
                        ::BooksTable, ::TestTable, ::customTable)
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


            "Custom names" {
                customTable.tableName shouldBe "table_with_custom_name"
                customTable.columns.firstOrNull { it.name == "column_with_custom_name" }.shouldNotBeNull()

                shouldNotThrowAny {
                    val entity = customTable[1]!!
                    entity.field shouldBe "value"
                    entity.update {
                        field = "new_value"
                    }
                    customTable.delete { CustomTable::field eq "new_value" }
                    customTable.size shouldBe 0
                }
            }

            "Types" - { typeTests() }

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


