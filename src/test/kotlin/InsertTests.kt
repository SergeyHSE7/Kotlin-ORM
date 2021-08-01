import entities.*
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class InsertTests : FreeSpec({
    config {
        tables = { listOf(UserBooksTable, UsersTable, AddressesTable, BooksTable, TestTable) }
        refreshTables = true
        database = Database(
            url = "jdbc:postgresql://localhost:5432/FinAssistant",
            user = "postgres",
            password = "123456"
        )
        jsonFormat = {
            encodeDefaults = true
            prettyPrint = true
        }
    }
    val newEntity = Test(string = "unknown", int = 5)

    "INSERT check" {
        (newEntity in TestTable) shouldBe false

        newEntity.save()

        (newEntity in TestTable) shouldBe true
    }
    "Shouldn't duplicate objects with unique values" {
        newEntity.save() shouldBe null

        // TestTable -= newEntity
        newEntity.delete()
    }
})
