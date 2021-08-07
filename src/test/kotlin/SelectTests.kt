import entities.Address
import entities.User
import entities.UsersTable
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldBeSortedWith
import io.kotest.matchers.collections.shouldNotBeSortedWith
import io.kotest.matchers.shouldBe
import statements.selectAll

class SelectTests : FreeSpec({
    "Load references when adding entity" {
        val user = User(username = "Max", address = Address(1)).save()!!
        user.address!!.country shouldBe "USA"
        user.delete()
    }
    "Not-existing entity equals null" {
        UsersTable[100] shouldBe null
    }

    "WHERE" {
        UsersTable.getAll { User::enabled eq true }.all { it.enabled } shouldBe true
        UsersTable.findIdOf { User::username eq "Marco" } shouldBe 2
        UsersTable.first { (User::enabled eq false) and (User::username startsWith "S") }?.username shouldBe "Simon"
    }

    "LIMIT" {
        UsersTable.selectAll().limit(3).getEntities().size shouldBe 3
    }
    "ORDER" {
        val comparator = { user1: User, user2: User -> user1.age!! - user2.age!! }

        UsersTable.selectAll().orderBy(User::age).getEntities() shouldBeSortedWith comparator
        UsersTable.selectAll().orderByDescending(User::age).getEntities() shouldNotBeSortedWith comparator
    }
    "OFFSET" {
        UsersTable.selectAll().offset(3).getEntity()?.id shouldBe 4
    }

})
