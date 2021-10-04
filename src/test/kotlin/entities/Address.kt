package entities

import Entity
import databases.Database
import kotlinx.serialization.Serializable
import table

@Serializable
data class Address(
    override var id: Int = 0,
    var country: String = "",
    var city: String = "",
) : Entity() {
    val users: List<User?> by oneToMany(User::address)
}

private val defaultEntities = { listOf(
    Address(country = "USA", city = "New York"),
    Address(country = "France", city = "Paris"),
    Address(country = "Italy", city = "Rome"),
    Address(country = "USA", city = "Seattle"),
    Address(country = "Russia", city = "Moscow"),
)}

val AddressesTable = table<Address, Database> {
    defaultEntities(defaultEntities)
    uniqueColumns(Address::city, Address::country)
}
