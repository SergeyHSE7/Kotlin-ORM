import entities.Test
import entities.TestTable
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import org.postgresql.util.PSQLException


class TableOperationsTests : FreeSpec({
    "Create table" {
        TestTable.tableName shouldBe "tests"
        TestTable.columns.size shouldBe Test().properties.size
        TestTable.size shouldBe TestTable.defaultEntities.size
    }
    "Clear table" {
        TestTable.clearTable()
        TestTable.size shouldBe 0
    }
    "Drop table" {
        TestTable.dropTable()
        shouldThrowExactly<PSQLException> {
            TestTable.getAll()
        }
    }
})
