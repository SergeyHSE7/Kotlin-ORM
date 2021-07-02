import entities.Address
import entities.AddressesTable
import entities.User
import entities.UsersTable
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import statements.*

class UpdateTests : FreeSpec({
    "UPDATE check" {
        val address = AddressesTable[1]!!
        address.city = "Chicago"
        AddressesTable.update(address, Address::city)
        AddressesTable[1]!!.city shouldBe "Chicago"
    }
    "Should update references" {
        val user = UsersTable[2]!!
        UsersTable.update(user) {
            username = "Josef"
            address = AddressesTable[1]!!.copy(city = "Los Angeles")
        }
        println(UsersTable[2]!!)
        println(user)
        UsersTable[2]!!.compareValuesWith(user) shouldBe true
        AddressesTable[1]!!.city shouldBe "Los Angeles"
    }
    "Should create non-existent entities (references)" {
        val user = UsersTable[2]!!
        user.address = Address(country = "Germany", city = "Berlin")
        UsersTable.update(user, User::address)

        UsersTable[2]!!.address?.compareValuesWith(user.address!!) shouldBe true
    }
})
