import entities.Address
import entities.User
import entities.UsersTable
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class TableMethodsTests: FreeSpec({
    val defaultUsers = UsersTable.defaultEntities

    "get" {
        UsersTable[1]!!.compareValuesWith(defaultUsers[0]) shouldBe true
    }

    "size" {
        UsersTable.size shouldBe defaultUsers.size
    }

    "count" {
        UsersTable.count { User::address eq Address(5) } shouldBe 2
        UsersTable.count { (User::address eq 5) and (User::age eq 42) } shouldBe 1
        UsersTable.count { (User::address eq 5) and (User::age eq 43) and (User::enabled eq false) } shouldBe 0
    }

    "first/last" {
        UsersTable.first()!!.compareValuesWith(defaultUsers.first()) shouldBe true
        UsersTable.last()!!.compareValuesWith(UsersTable.getAll().last()) shouldBe true
        UsersTable.last { User::address eq 5 }!!.username shouldBe "Alex"
    }

    "contains" {
        (defaultUsers[2] in UsersTable) shouldBe true
        (defaultUsers in UsersTable) shouldBe true
        (User(username = "Anonymous") in UsersTable) shouldBe false
    }

    "all/any/none check" {
        UsersTable.all { User::age greater 10 } shouldBe true
        UsersTable.all { User::age greater 50 } shouldBe false

        UsersTable.any { User::age greater 50 } shouldBe true
        UsersTable.any { User::age greater 150 } shouldBe false

        UsersTable.none { User::age greater 150 } shouldBe true
        UsersTable.none { User::age greater 50 } shouldBe false
    }

    "getValuesOfColumn" {
        UsersTable.getValuesOfColumn(User::username) shouldBe UsersTable.getAll().map { it.username }
    }
})
