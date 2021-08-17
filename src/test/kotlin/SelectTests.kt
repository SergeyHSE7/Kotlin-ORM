import entities.Address
import entities.User
import io.kotest.core.spec.style.scopes.FreeSpecContainerContext
import io.kotest.matchers.collections.shouldBeSortedWith
import io.kotest.matchers.collections.shouldNotBeSortedWith
import io.kotest.matchers.shouldBe
import statements.selectAll

suspend inline fun FreeSpecContainerContext.selectTests() {
    val usersTable = Table<User>()

    "Load references when adding entity" {
        val user = User(username = "Max", address = Address(1)).save()!!
        user.address!!.country shouldBe "USA"
        user.delete()
    }
    "Not-existing entity equals null" {
        usersTable[100] shouldBe null
    }

    "WHERE" {
        usersTable.getAll { User::enabled eq true }.all { it.enabled } shouldBe true
        usersTable.findIdOf { User::username eq "Marco" } shouldBe 2
        usersTable.first { (User::enabled eq false) and (User::username startsWith "S") }?.username shouldBe "Simon"
    }

    "LIMIT" {
        usersTable.selectAll().limit(3).getEntities().size shouldBe 3
    }
    "ORDER" {
        val comparator = { user1: User, user2: User -> user1.age!! - user2.age!! }

        usersTable.selectAll().orderBy(User::age).getEntities() shouldBeSortedWith comparator
        usersTable.selectAll().orderByDescending(User::age).getEntities() shouldNotBeSortedWith comparator
    }
    "OFFSET" {
        usersTable.selectAll().offset(3).getEntity()?.id shouldBe 4
    }

}
