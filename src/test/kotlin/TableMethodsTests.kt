import entities.Address
import entities.User
import io.kotest.core.spec.style.scopes.FreeSpecContainerContext
import io.kotest.matchers.shouldBe

suspend inline fun FreeSpecContainerContext.tableMethodsTests() {
    val usersTable = Table<User>()
    val defaultUsers = usersTable.defaultEntities

    "get" {
        usersTable[1]!!.compareValuesWith(defaultUsers[0]) shouldBe true
    }

    "size" {
        usersTable.size shouldBe defaultUsers.size
    }

    "count" {
        usersTable.count { User::address eq Address(5) } shouldBe 2
        usersTable.count { (User::address eq 5) and (User::age eq 42) } shouldBe 1
        usersTable.count { (User::address eq 5) and (User::age eq 43) and (User::enabled eq false) } shouldBe 0
    }

    "first/last" {
        usersTable.first()!!.compareValuesWith(defaultUsers.first()) shouldBe true
        usersTable.last()!!.compareValuesWith(usersTable.getAll().last()) shouldBe true
        usersTable.last { User::address eq 5 }!!.username shouldBe "Alex"
    }

    "contains" {
        (defaultUsers[2] in usersTable) shouldBe true
        (defaultUsers in usersTable) shouldBe true
        (User(username = "Anonymous") in usersTable) shouldBe false
    }

    "all/any/none check" {
        usersTable.all { User::age greater 10 } shouldBe true
        usersTable.all { User::age greater 50 } shouldBe false

        usersTable.any { User::age greater 50 } shouldBe true
        usersTable.any { User::age greater 150 } shouldBe false

        usersTable.none { User::age greater 150 } shouldBe true
        usersTable.none { User::age greater 50 } shouldBe false
    }

    "getValuesOfColumn" {
        usersTable.getValuesOfColumn(User::username) shouldBe usersTable.getAll().map { it.username }
    }
}
