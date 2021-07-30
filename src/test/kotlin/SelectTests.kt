import entities.*
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeSortedWith
import io.kotest.matchers.collections.shouldNotBeSortedWith
import io.kotest.matchers.shouldBe
import statements.selectAll

class SelectTests : FreeSpec({
    val defaultUsers = UsersTable.defaultEntities

    "SELECT COUNT(*)" {
        UsersTable.size shouldBe defaultUsers.size
    }
    "Not-existing entity equals null" {
        UsersTable[100] shouldBe null
    }
    "Comparing values" {
        println(UsersTable[2])
        println(defaultUsers[1])
        UsersTable[2]?.compareValuesWith(defaultUsers[1]) shouldBe true
    }

    "WHERE check" {
        UsersTable.findIdOf { User::username eq "Marco" } shouldBe 2
        UsersTable.find { (User::enabled eq false) and (User::username startsWith "S") }?.username shouldBe "Simon"

        println()
        UsersTable.getValuesOfColumn(User::username).forEach { println(it) }
    }

    "LIMIT check" {
        UsersTable.selectAll().limit(3).getEntities().size shouldBe 3
    }
    "ORDER check" {
        val comparator = { user1: User, user2: User -> user1.age!! - user2.age!! }

        UsersTable.selectAll().orderBy(User::age).getEntities() shouldBeSortedWith comparator
        UsersTable.selectAll().orderByDescending(User::age).getEntities() shouldNotBeSortedWith comparator
    }
    "OFFSET check" {
        UsersTable.selectAll().offset(3).getEntity()?.id shouldBe 4
    }

})
