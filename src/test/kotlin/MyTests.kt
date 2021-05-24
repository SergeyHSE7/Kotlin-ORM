import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.*
import org.atteo.evo.inflector.English
import org.postgresql.util.PSQLException
import statements.delete
import statements.update
import utils.Case
import utils.getEntity
import utils.transformCase

/*val database: Database = Database(
    url = "jdbc:postgresql://localhost:5432/FinAssistant",
    user = "postgres",
    password = "123456"
)*/

class MyTests : FreeSpec({
    "string transform utils check" - {
        "plural library check" {
            English.plural("word") shouldBe "words"
            English.plural("box") shouldBe "boxes"
            English.plural("entity") shouldBe "entities"
            English.plural("mouse") shouldBe "mice"
            English.plural("elf") shouldBe "elves"
        }
        "splitByCase check" {
            "PascalCase".transformCase(Case.Pascal, Case.Normal) shouldBe "Pascal Case"
            "camelCase".transformCase(Case.Camel, Case.Normal) shouldBe "Camel Case"
            "snake_case".transformCase(Case.Snake, Case.Normal) shouldBe "Snake Case"
            "kebab-case".transformCase(Case.Kebab, Case.Normal) shouldBe "Kebab Case"
        }
        "joinWithCase check" {
            "Pascal Case".transformCase(Case.Normal, Case.Pascal) shouldBe "PascalCase"
            "Camel Case".transformCase(Case.Normal, Case.Camel) shouldBe "camelCase"
            "Snake Case".transformCase(Case.Normal, Case.Snake) shouldBe "snake_case"
            "Kebab Case".transformCase(Case.Normal, Case.Kebab) shouldBe "kebab-case"
        }
        "transform to plural forms" {
            "Long Word".transformCase(Case.Normal, Case.Snake, true) shouldBe "long_words"
        }
    }

    "check table operations" - {
        "create table" {
            TestTable.tableName shouldBe "test_entities"
            TestTable.entityClass.simpleName shouldBe "TestEntity"
            TestTable.columns.size shouldBe TestEntity().properties.size
            TestTable.all().size shouldBe defaultTestEntities.size
        }
        "clear table" {
            TestTable.clearTable()
            TestTable.all().size shouldBe 0
        }
        "drop table" {
            TestTable.dropTable()
            shouldThrowExactly<PSQLException> {
                TestTable.all()
            }
            TestTable.createTable()
            TestTable.defaultEntities(defaultTestEntities)
        }
    }

    "statements check" - {
        val newEntity = TestEntity(id = 1, string = "new1")


        "SELECT check" {
            TestTable.all().forEach(::println)
            TestTable.all().size shouldBe defaultTestEntities.size

            TestTable.findById(2) shouldBe defaultTestEntities[1].apply { id = 2 }
            TestTable.findById(2)?.compareValuesWith(defaultTestEntities[1]) shouldBe true
            TestTable.findById(100) shouldBe null

            TestTable.findIdOf { TestEntity::string eq "str2" } shouldBe 2
            TestTable.find { TestEntity::int greater 1 and (TestEntity::string startsWith "str") }?.string shouldBe "str2"
        }
        "INSERT check" {
            TestTable.add(newEntity) shouldBe defaultTestEntities.size + 1
            TestTable.add(newEntity) shouldBe null
        }
        "DELETE check" {
            TestTable.delete { TestEntity::string eq newEntity.string }
            TestTable.find { TestEntity::string eq newEntity.string } shouldBe null
        }
        "UPDATE check" {
            var entity = TestTable.findById(1)!!
            entity.string = "updateStr1"
            TestTable.update(entity, TestEntity::string)
            TestTable.findById(1)!!.string shouldBe "updateStr1"

            entity = TestTable.findById(2)!!
            TestTable.update(entity) {
                int = -2
                string = "updateStr2"
            }
            TestTable.findById(2)!! shouldBe entity

            entity.id = 5
            TestTable.update(entity)
            TestTable.findById(2)!! shouldBe entity.copy(id = 2)
        }
    }
})
