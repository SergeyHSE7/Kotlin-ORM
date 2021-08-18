import entities.Address
import entities.User
import io.kotest.core.spec.style.scopes.FreeSpecContainerContext
import io.kotest.matchers.shouldBe

suspend inline fun FreeSpecContainerContext.updateTests() {
    val usersTable = Table<User>()
    val addressesTable = Table<Address>()

    "check" {
        val address = addressesTable[1]!!
        address.city = "Chicago"
        addressesTable.update(address, Address::city)
        addressesTable[1]!!.city shouldBe "Chicago"
    }
    "Should update references" {
        val user = usersTable[2]!!
        user.update {
            username = "Josef"
            address = addressesTable[1]!!.copy(city = "Los Angeles")
        }
        println(usersTable[2]!!)
        println(user)
        usersTable[2]!!.compareValuesWith(user) shouldBe true
        addressesTable[1]!!.city shouldBe "Los Angeles"
    }
    "Should create non-existent entities (references)" {
        val user = usersTable[2]!!
        user.address = Address(country = "Germany", city = "Berlin")
        usersTable.update(user, User::address)

        usersTable[2]!!.address?.compareValuesWith(user.address!!) shouldBe true
    }
}
