import entities.Address
import entities.User
import entities.UserBook
import io.kotest.core.spec.style.scopes.FreeSpecContainerContext
import io.kotest.matchers.shouldBe

suspend inline fun FreeSpecContainerContext.referenceTests() {
    "One To Many" {
        val users = Table<Address>()[5]!!.users
        users.size shouldBe 2
        users.forEach(::println)
    }

    "Many To Many" {
        val books = Table<User>()[1]!!.books
        books.size shouldBe 2
        books.forEach(::println)
    }

    "Cascade delete reference" {
        val user = Table<User>()[5]!!
        val userBooksSize = Table<UserBook>().size
        user.delete()
        Table<UserBook>().size shouldBe userBooksSize - 1
        user.save()
    }
}
