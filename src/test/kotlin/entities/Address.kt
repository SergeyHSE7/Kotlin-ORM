package entities

import Entity
import databases.PostgreSQL
import databases.SQLite
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

val AddressesTablePostgreSQL = table<Address, PostgreSQL> {
    varchar(Address::country, 30)
    varchar(Address::city, 30)

    defaultEntities(defaultEntities)
}

val AddressesTableSQLite = table<Address, SQLite> {
    defaultEntities(defaultEntities)
}
