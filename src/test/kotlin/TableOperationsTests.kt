import entities.Test
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.scopes.FreeSpecContainerContext
import io.kotest.matchers.shouldBe
import java.sql.SQLException


suspend inline fun FreeSpecContainerContext.tableOperationsTests() {
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
