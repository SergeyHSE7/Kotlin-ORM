package entities

import Entity
import Table

data class Address(
    override var id: Int = 0,
    var country: String = "",
    var city: String = "",
) : Entity() {
    val users: List<User?> by oneToMany(UsersTable, User::address)
}

val defaultAddresses = listOf(
    Address(country = "USA", city = "New York"),
    Address(country = "France", city = "Paris"),
    Address(country = "Italy", city = "Rome"),
    Address(country = "USA", city = "Seattle"),
    Address(country = "Russia", city = "Moscow"),
)

object AddressesTable : Table<Address>(Address::class, true, {
    varchar(Address::country, 30)
    varchar(Address::city, 30)
}, defaultAddresses)
