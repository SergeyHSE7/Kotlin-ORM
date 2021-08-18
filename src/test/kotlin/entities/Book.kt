package entities

import Entity
import column
import databases.PostgreSQL
import databases.SQLite
import kotlinx.serialization.Serializable
import table

@Serializable
data class Book(
    override var id: Int = 0,
    var title: String = "",
    var author: String = "",
    var publishedYear: Int = 0,
    var isbn: Int = 0
) : Entity()

private val defaultEntities = { listOf(
    Book(title = "1984", author = "George Orwell", isbn = 123456, publishedYear = 1949),
    Book(title = "Witcher", author = "Andrzej Sapkowski", isbn = 505604, publishedYear = 1986),
    Book(title = "Martin Eden", author = "Jack London", isbn = 507634, publishedYear = 1909),
    Book(title = "Brave New World", author = "Aldous Huxley", isbn = 985462, publishedYear = 1932),
    Book(title = "White Fang", author = "Jack London", isbn = 123456, publishedYear = 1906),
)}

val BooksTablePostgreSQL = table<Book, PostgreSQL> {
    varchar(Book::title, 100)
    varchar(Book::author, 100)
    column(Book::isbn).unique()

    defaultEntities(defaultEntities)
}

val BooksTableSQLite = table<Book, SQLite> {
    column(Book::isbn).unique()

    defaultEntities(defaultEntities)
}
