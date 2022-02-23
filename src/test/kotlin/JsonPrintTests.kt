import entities.User
import io.kotest.core.spec.style.scopes.FreeSpecContainerContext
import io.kotest.matchers.shouldBe
import kotlinx.serialization.decodeFromString
import utils.toJson
import utils.toJsonOnly
import utils.toJsonWithout

suspend inline fun FreeSpecContainerContext.jsonPrintTests() {
    val usersTable = Table<User>()
    val user = usersTable[1]!!

    "toJson" {
        val jsonUser = user.toJson()
        jsonUser shouldBe """{"id":1,"username":"Kevin","address":{"id":1,"country":"USA","city":"Chicago"},"enabled":true,"age":18}"""
        user shouldBe json.decodeFromString<User>(jsonUser)
        // println(json.decodeFromString<List<User>>(usersTable.getAll().toJson()))
    }

    "toJsonOnly" {
        val jsonUser = user.toJsonOnly(User::username, User::address)
        jsonUser shouldBe """{"username":"Kevin","address":{"id":1,"country":"USA","city":"Chicago"}}"""
        User(username = user.username, address = user.address) shouldBe json.decodeFromString<User>(jsonUser)
    }

    "toJsonWithout" {
        val jsonUser = user.toJsonWithout(User::username, User::address)
        jsonUser shouldBe """{"id":1,"enabled":true,"age":18}"""
        User(id = user.id, age = user.age, enabled = user.enabled) shouldBe json.decodeFromString<User>(jsonUser)
    }
}
