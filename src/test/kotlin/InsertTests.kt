import entities.*
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class InsertTests : FreeSpec({
    config {
        database = Database(
            url = "jdbc:postgresql://localhost:5432/FinAssistant",
            user = "postgres",
            password = "123456"
        )
        refreshTables = true
        tables = listOf(UserBooksTable, UsersTable, AddressesTable, BooksTable, TestTable)
    }
    val newEntity = Test(string = "unknown", int = 5)

    "INSERT check" {
        (newEntity in TestTable) shouldBe false

        val id = newEntity.save()
        newEntity.id shouldBe id

        (newEntity in TestTable) shouldBe true
    }
    "Shouldn't duplicate objects with unique values" {
        newEntity.save() shouldBe null

        // TestTable -= newEntity
        newEntity.delete()
    }
})
