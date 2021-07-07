import entities.Test
import entities.TestTable
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class InsertTests : FreeSpec({
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
