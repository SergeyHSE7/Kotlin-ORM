import entities.User
import entities.UsersTable
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.decodeFromString
import utils.toJson
import utils.toJsonOnly
import utils.toJsonWithout

class JsonPrintTests: FreeSpec({
    val user = UsersTable[1]!!

    "toJson" {
        val jsonUser = user.toJson()
        println(jsonUser)
        println(json.decodeFromString<User>(jsonUser))
        user shouldBe json.decodeFromString<User>(jsonUser)

        println(UsersTable.getAll().toJson())
        println(json.decodeFromString<List<User>>(UsersTable.getAll().toJson()))
    }

    "toJsonOnly" {
        val jsonUser = user.toJsonOnly(User::username, User::address)
        println(jsonUser)
        println(json.decodeFromString<User>(jsonUser))
        User(username = user.username, address = user.address) shouldBe json.decodeFromString<User>(jsonUser)

        println(UsersTable.getAll().toJsonOnly(User::username, User::address))
    }

    "toJsonWithout" {
        val jsonUser = user.toJsonWithout(User::username, User::address)
        println(jsonUser)
        println(json.decodeFromString<User>(jsonUser))
        User(id = user.id, age = user.age, enabled = user.enabled) shouldBe json.decodeFromString<User>(jsonUser)

        println(UsersTable.getAll().toJsonWithout(User::username, User::address))
    }
})
