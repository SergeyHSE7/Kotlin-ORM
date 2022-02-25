import databases.SQLite
import entities.User
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.scopes.FreeSpecContainerContext
import io.kotest.matchers.shouldBe
import statements.Expression
import statements.WhereStatement
import statements.subQuery

suspend inline fun FreeSpecContainerContext.whereTests() {
    val usersTable = Table<User>()
    val expr = List(6) { Expression(it.toString()) }

    "Sql" {
        usersTable.getAll { User::enabled eq true }.all { it.enabled } shouldBe true
        usersTable.findIdOf { User::username eq "Marco" } shouldBe 2
        usersTable.first { (User::enabled eq false) * (User::username startsWith "S") }?.username shouldBe "Simon"
    }

    "BETWEEN" {
        usersTable.count { User::age.between(20, 42) } shouldBe 4
        usersTable.count { User::age.notBetween(20, 42) } shouldBe 2
    }

    "IN" {
        usersTable.all { User::username inColumn User::username } shouldBe true
        usersTable.count { User::username inColumn User::username.subQuery { where { User::username startsWith "S" } } } shouldBe 3
        usersTable.first { User::username notInColumn User::username } shouldBe null
    }

    "ALL/ANY" {
        if (Config.database is SQLite) {
            shouldThrowAny { usersTable.first { User::age greaterEq all(User::age) }!!.age }
            shouldThrowAny { usersTable.count { User::age less any(User::age.subQuery { where { User::enabled eq false } }) } }
        } else {
            usersTable.first { User::age greaterEq all(User::age) }!!.age shouldBe 67
            usersTable.count { User::age less any(User::age.subQuery { where { User::enabled eq false } }) } shouldBe 4
        }
    }

    "EXISTS" {
        usersTable.count { exists(User::address) } shouldBe 6
    }

    "Order" {
        listOf<WhereStatement.() -> Pair<Expression, Expression>>(
            { expr[0] + expr[1] + expr[2] to Expression("(0 OR 1 OR 2)") },
            { expr[0] + expr[1] * expr[2] to Expression("0 OR 1 AND 2") },
            { (expr[0] + expr[1]) * expr[2] to Expression("(0 OR 1) AND 2") },
            { expr[0] + expr[1] * (expr[2] + expr[3]) to Expression("0 OR 1 AND (2 OR 3)") },
        )
            .forEach { it(WhereStatement()).run { first shouldBe second } }
    }

    "Inverse" {
        listOf<WhereStatement.() -> Pair<Expression, Expression>>(
            { !(expr[0] eq expr[1]) to Expression("0 != 1") },
            { !(expr[0] less expr[1]) to Expression("0 >= 1") },
            { !((expr[0] eq expr[1]) + (expr[2] eq expr[3])) to Expression("0 != 1 AND 2 != 3") },
            { !((expr[0] eq expr[1]) + (expr[2] eq expr[3]) * (expr[4] eq expr[5])) to Expression("0 != 1 AND (2 != 3 OR 4 != 5)") },
        )
            .forEach { it(WhereStatement()).run { first shouldBe second } }
    }
}
