import io.kotest.assertions.throwables.shouldThrowExactly
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.*
import io.kotest.matchers.ints.shouldBeGreaterThan
import org.atteo.evo.inflector.English
import org.postgresql.util.PSQLException
import statements.*
import utils.Case
import utils.*
import kotlin.system.measureNanoTime


fun <T> printMeasureTime(proccessName: String = "", func: () -> T): T {
    var value: T
    println("$proccessName - ".ifTrue(proccessName.isNotBlank()) + measureNanoTime {
        value = func()
    } / 1000000.0 + "ms")
    return value
}


class MyTests : FreeSpec({
    "!string transform utils check" - {
        "plural library check" {
            printMeasureTime {
                English.plural("word") shouldBe "words"
                English.plural("box") shouldBe "boxes"
                English.plural("entity") shouldBe "entities"
                English.plural("mouse") shouldBe "mice"
                English.plural("elf") shouldBe "elves"
            }
        }
        "splitByCase check" {
            printMeasureTime {
                "PascalCase".transformCase(Case.Pascal, Case.Normal) shouldBe "Pascal Case"
                "camelCase".transformCase(Case.Camel, Case.Normal) shouldBe "Camel Case"
                "snake_case".transformCase(Case.Snake, Case.Normal) shouldBe "Snake Case"
                "kebab-case".transformCase(Case.Kebab, Case.Normal) shouldBe "Kebab Case"
            }
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
            TestTable.size shouldBe defaultTestEntities.size

            TestTable[2]?.compareValuesWith(defaultTestEntities[1]) shouldBe true
            TestTable[100] shouldBe null

            TestTable.findIdOf { TestEntity::string eq "str2" } shouldBe 2
            TestTable.find { TestEntity::int greater 1 and (TestEntity::string startsWith "str") }?.string shouldBe "str2"

            println()
            TestTable.getValuesOfColumn(TestEntity::string).forEach { println(it) }

            TestTable.selectAll().limit(1).getEntities().size shouldBe 1
            TestTable.selectAll().orderByDescending(TestEntity::int).getEntity()?.int shouldBe 2
            TestTable.selectAll().offset(1).getEntity()?.int shouldBe 2
        }
        "INSERT check" {
            (newEntity in TestTable) shouldBe false
            TestTable.add(newEntity) shouldBe defaultTestEntities.size + 1
            (newEntity in TestTable) shouldBe true

            TestTable.add(newEntity) shouldBe null
            TestTable.add(TestEntity(int = 1, int1 = 1)) shouldBe null
        }
        "DELETE check" {
            TestTable.delete { TestEntity::string eq newEntity.string }
            TestTable.find { TestEntity::string eq newEntity.string } shouldBe null
        }
        "UPDATE check" {
            var entity = TestTable[1]!!
            entity.string = "updateStr1"
            TestTable.update(entity, TestEntity::string)
            TestTable[1]!!.string shouldBe "updateStr1"

            entity = TestTable[2]!!
            TestTable.update(entity) {
                int = -2
                string = "updateStr2"
                human = human?.copy(age = 40)
            }
            TestTable[2]!!.compareValuesWith(entity) shouldBe true
            HumanTable[1]!!.age shouldBe 40

            TestTable[5] = entity
            TestTable[2]!!.compareValuesWith(entity) shouldBe true
        }
        "One To Many check" {
            val human = HumanTable[1]!!
            println(human)
            human.tests.forEach(::println)
        }
        "Many To Many check" {
            val human = HumanTable[1]!!
            human.followers.size shouldBeGreaterThan 0
            human.followers.first()?.id shouldBe 2
        }
        "Lazy" {
            println(FollowerToFollowersTable.findById(1, false)!!.toJson())
            FollowerToFollowersTable.cache.clear()
            println(FollowerToFollowersTable.findById(1, false)!!.toJson())
            println(FollowerToFollowersTable[1]!!.toJson())
        }
        "References check" {
            HumanTable.deleteById(2)
            FollowerToFollowersTable.isEmpty() shouldBe true
        }
        "Json print" {
            val human = HumanTable[1]!!
            println("toJson() - " + human.toJson())
            println("toJson(all = true) - " + human.toJson(true))
            println("toJsonOnly() - " + human.toJsonOnly(Human::name, Human::followers, Human::tests))
            println("toJsonWithout() - " + human.toJsonWithout(Human::name, Human::followers))
        }
        "Join" {
            println(TestTable[2]!!.human)
            TestTable.select(Human::name, Human::age).innerJoin(HumanTable) { TestEntity::human eq Human::id }.getResultSet()
                .map { getString("name") + " - " + getInt("age") }.forEach(::println)
        }
    }
})
