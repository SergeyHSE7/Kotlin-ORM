import entities.User
import io.kotest.core.spec.style.scopes.FreeSpecContainerContext
import io.kotest.matchers.shouldBe
import statements.Expression
import statements.WhereStatement

suspend inline fun FreeSpecContainerContext.whereTests() {
    val usersTable = Table<User>()
    val expr = List(6) { Expression(it.toString()) }

    "Sql" {
        usersTable.getAll { User::enabled eq true }.all { it.enabled } shouldBe true
        usersTable.findIdOf { User::username eq "Marco" } shouldBe 2
        usersTable.first { (User::enabled eq false) * (User::username startsWith "S") }?.username shouldBe "Simon"
    }

    "Order" {
        listOf<WhereStatement.() -> Pair<Expression, Expression>>(
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
            { !(!(expr[0] neq expr[1]) + (expr[2] eq expr[3]) * !(expr[4] neq expr[5])) to Expression("0 != 1 AND (2 != 3 OR 4 != 5)") },
        )
            .forEach { it(WhereStatement()).run { first shouldBe second } }
    }
}
