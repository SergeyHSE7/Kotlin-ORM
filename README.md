![](logo.png)

**Kotlin-ORM** is an ORM-library for *PostgreSQL*, *MariaDB* and *SQLite*.

This library offers *easy* and *convenient* interaction with the database 
via lightweight data access objects and a ready-made set of basic methods
(for getting, updating, adding, deleting, searching, aggregating and so on).
The main purpose of this library is to provide the possibility of 
*rapid* prototyping of projects that work with databases.

#### For documentation go to: https://sergeyhse.gitbook.io/kotlin-orm/

### Example:
```kotlin
// User entity
data class User(
    override var id: Int = 0,
    var username: String = "",
    var age: Int? = 18,
) : Entity() {
    // Getter to get a list of Book entities
    val books: List<Book> by oneToMany(Book::user)
}

val userTable = table<User, Database> {
    column(User::username).unique()
    check(User::age) { it greaterEq 18 }

    defaultEntities { listOf(
        User(username = "Mike", age = 21),
        User(username = "Sue", age = 35),
        User(username = "Bill", age = 27),
    ) }
}

// Book entity
data class Book(
    override var id: Int = 0,
    var name: String? = null,
    var user: User? = null // reference to User class
) : Entity()

val bookTable = table<Book, Database> {
    defaultEntities { listOf(Book(name = "Book_1", user = User(2)), Book(name = "Book_2", user = User(2))) }
}

// Program entry point
fun main() {
    config {
        database = SQLite(url = """jdbc:sqlite:C:\\SQLite3\\test_db.sqlite""")
        setTables(::userTable, ::bookTable)
        refreshTables = true
    }

    // Get entity
    val user = userTable[2]!!

    // Get Book entities related through a One-To-Many relationship
    println(user.books)
    // Output: [Book(id=1, name=Book_1, user=User(id=2, username=Sue, age=35)),
    //          Book(id=2, name=Book_2, user=User(id=2, username=Sue, age=35))]

    // Add new entity
    val newUser = User(username = "Sam", age = 33).save()!!

    // Update entity's data
    newUser.update {
        username = "Samuel"
        age = 22
    }

    println(newUser in userTable) // Output: true

    println(userTable[User::username]) // Output: [Bill, Mike, Samuel, Sue]

    println(userTable.maxBy(User::age)?.age) // Output: 35

    println(userTable.count { User::age lessEq 25 }) // Output: 2

    println(userTable.findIdOf { User::username eq "Bill" }) // Output: 3

    println(userTable.any { User::username startsWith "Sa" }) // Output: true

    println(userTable.aggregateBy(User::age, SqlList::average)) // Output: 26

    // Delete entity
    newUser.delete()
}
```
Generated SQL:
```sql
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS books;

CREATE TABLE IF NOT EXISTS users 
(    username TEXT NOT NULL UNIQUE DEFAULT '',
     age      INTEGER DEFAULT 18,
     id       INTEGER PRIMARY KEY NOT NULL,
     CHECK (users.age >= 18)
);

CREATE TABLE IF NOT EXISTS books
(    id   INTEGER PRIMARY KEY NOT NULL,
     name TEXT DEFAULT null,
     user INTEGER DEFAULT null,
     FOREIGN KEY (user) REFERENCES users(id) ON DELETE SET DEFAULT
);

INSERT INTO users (username, age) VALUES (?, ?), (?, ?), (?, ?) ON CONFLICT DO NOTHING RETURNING id;
INSERT INTO books (name, user) VALUES (?, ?), (?, ?) ON CONFLICT DO NOTHING RETURNING id;

SELECT * FROM users WHERE users.id = 2 LIMIT 1;
SELECT * FROM books LEFT JOIN users ON users.id = books.user WHERE books.user = 2;
INSERT INTO users (username, age) VALUES (?, ?) ON CONFLICT DO NOTHING RETURNING id ;
SELECT * FROM users WHERE users.id = 4 LIMIT 1;
UPDATE users SET age = 22, username = 'Samuel' WHERE id = 4;
SELECT id FROM users WHERE users.age = 22 AND users.username = 'Samuel';
SELECT users.username FROM users;
SELECT * FROM users ORDER BY users.age DESC LIMIT 1;
SELECT COUNT(*) FROM users WHERE users.age <= 25;
SELECT id FROM users WHERE users.username = 'Bill';
SELECT id FROM users WHERE users.username LIKE 'Sa%' LIMIT 1;
SELECT AVG(users.age) FROM users;
DELETE FROM users WHERE id = 4;
```
