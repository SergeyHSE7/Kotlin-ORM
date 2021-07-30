import entities.AddressesTable
import entities.UsersTable
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe

class ReferenceTests : FreeSpec({
    "One To Many" {
        val users = AddressesTable[5]!!.users
        users.size shouldBe 2
        users.forEach(::println)
    }

    "Many To Many" {
        val books = UsersTable[1]!!.books
        books.size shouldBe 2
        books.forEach(::println)
    }
})
