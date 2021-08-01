import entities.AddressesTable
import entities.UserBooksTable
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

    "Cascade delete reference" {
        val user = UsersTable[5]!!
        val userBooksSize = UserBooksTable.size
        user.delete()
        UserBooksTable.size shouldBe userBooksSize - 1
        user.save()
    }
})
