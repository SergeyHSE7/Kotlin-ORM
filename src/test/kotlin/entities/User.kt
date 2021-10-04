package entities

import Action
import Entity
import databases.Database
import kotlinx.serialization.Serializable
import table

@Serializable
data class User(
    override var id: Int = 0,
    var username: String = "",
    var address: Address? = null,
    var enabled: Boolean = true,
    var age: Int? = 18
) : Entity() {
    val books: List<Book?> by manyToMany(UserBook::borrower, UserBook::book)
}

private val defaultEntities = { listOf(
        User(username = "Kevin", address = Address(1), enabled = true, age = 18),
        User(username = "Marco", address = Address(3), enabled = false, age = 25),
        User(username = "Sue", address = Address(2), enabled = true, age = 22),
        User(username = "Sergey", address = Address(5), enabled = true, age = 67),
        User(username = "Alex", address = Address(5), enabled = false, age = 42),
        User(username = "Simon", address = Address(4), enabled = false, age = 34),
    )}

val UsersTable = table<User, Database> {
    reference(User::address, Action.SetNull)
    check(User::age) { (it greaterEq 18) and (it less 80) }
    defaultEntities(defaultEntities)
}
