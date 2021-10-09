import entities.Address
import entities.User
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.scopes.FreeSpecContainerContext
import io.kotest.matchers.collections.shouldBeSortedWith
import io.kotest.matchers.collections.shouldNotBeSortedWith
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import statements.JoinType
import statements.select
import statements.selectAll
import utils.map
import kotlin.math.min

suspend inline fun FreeSpecContainerContext.selectTests() {
    val usersTable = Table<User>()
    val addressTable = Table<Address>()

    "Load references when adding entity" {
        val user = User(username = "Max", address = Address(1)).save()!!
        user.address!!.country shouldBe "USA"
        user.delete()
    }
    "Not-existing entity equals null" {
        usersTable[100] shouldBe null
    }

    "JOIN" {
        usersTable.select(User::username)
            .joinBy(User::address)
            .where { Address::country eq "USA" }
            .getResultSet().map { getString(1) } shouldBe listOf("Kevin", "Simon")

        shouldNotThrowAny {
            usersTable.select(User::username)
                .joinBy(User::address, JoinType.Inner).size shouldBe min(usersTable.size, addressTable.size)

            usersTable.select(User::username)
                .joinBy(User::address, JoinType.Left).size shouldBeGreaterThanOrEqual usersTable.size
        }
    }

    "CROSS JOIN" {
        usersTable.select(User::username, Address::city)
            .crossJoin<Address>().size shouldBe usersTable.size * addressTable.size
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
