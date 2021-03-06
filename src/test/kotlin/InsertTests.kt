import entities.Test
import io.kotest.core.spec.style.scopes.FreeSpecContainerContext
import io.kotest.matchers.shouldBe
import java.util.*

suspend inline fun FreeSpecContainerContext.insertTests() {

    val newEntity = Test(uniqueValue = 5, calendarValue = Calendar.getInstance())

    "INSERT check" {
        (newEntity in Table<Test>()) shouldBe false

        newEntity.save()

        (newEntity in Table<Test>()) shouldBe true
    }
    "Shouldn't duplicate objects with unique values" {
        newEntity.save() shouldBe null

        // TestTable -= newEntity
        newEntity.delete()
    }
}
