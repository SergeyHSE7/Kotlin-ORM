package entities

import Entity
import Table
import java.sql.Date

data class UserBook(
    override var id: Int = 0,
    var borrower: User? = null,
    var book: Book? = null,
    var checkoutDate: Date = Date(0),
    var returnDate: Date = Date(0),
) : Entity()


object UserBooksTable : Table<UserBook>(
    UserBook::class, true, {
    reference(UserBook::borrower, UsersTable, Action.Cascade)
    reference(UserBook::book, BooksTable, Action.Cascade)
    date(UserBook::checkoutDate)
    date(UserBook::returnDate)
}, listOf(
    UserBook(borrower = User(1), book = Book(2), checkoutDate = Date.valueOf("2020-12-20"), returnDate = Date.valueOf("2021-01-20")),
    UserBook(borrower = User(1), book = Book(3), checkoutDate = Date.valueOf("2020-12-20"), returnDate = Date.valueOf("2021-01-20")),
    UserBook(borrower = User(4), book = Book(1), checkoutDate = Date.valueOf("2021-04-12"), returnDate = Date.valueOf("2021-05-12")),
    UserBook(borrower = User(5), book = Book(2), checkoutDate = Date.valueOf("2021-02-05"), returnDate = Date.valueOf("2021-03-05")),
))