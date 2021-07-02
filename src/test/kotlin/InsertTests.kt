import entities.Test
import entities.TestTable
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class InsertTests : FreeSpec({
    val newEntity = Test(string = "unknown", int = 5)

    "INSERT check" {
        (newEntity in TestTable) shouldBe false

        val id = TestTable.add(newEntity)
        newEntity.id shouldBe id

        (newEntity in TestTable) shouldBe true
    }
    "Shouldn't duplicate objects with unique values" {
        TestTable.add(newEntity) shouldBe null

        TestTable -= newEntity
    }
})
