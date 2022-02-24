import entities.Address
import entities.User
import io.kotest.core.spec.style.scopes.FreeSpecContainerContext
import io.kotest.matchers.shouldBe
import sql_type_functions.SqlList

suspend inline fun FreeSpecContainerContext.tableMethodsTests() {
    val usersTable = Table<User>()
    val defaultUsers = usersTable.defaultEntities

    "get" {
        usersTable[1]!!.compareValuesWith(defaultUsers[0]) shouldBe true
    }

    "size" {
        usersTable.size shouldBe defaultUsers.size
    }

    "count" {
        usersTable.count { User::address eq Address(5) } shouldBe 2
        usersTable.count { (User::address eq 5) * (User::age eq 42) } shouldBe 1
        usersTable.count { (User::address eq 5) * (User::age eq 43) * (User::enabled eq false) } shouldBe 0

        usersTable.countNotNull(User::address) shouldBe usersTable.getAll().count { it.address != null }
    }

    "first/last" {
        usersTable.first()!!.compareValuesWith(defaultUsers.first()) shouldBe true
        usersTable.last()!!.compareValuesWith(usersTable.getAll().last()) shouldBe true
        usersTable.last { User::address eq 5 }!!.username shouldBe "Alex"
    }

    "firstOrDefault/lastOrDefault" {
        usersTable.firstOrDefault { User::age less 18 }.age shouldBe 18
        usersTable.lastOrDefault(User(username = "Default")) { User::address eq 8 }.username shouldBe "Default"
    }

    "maxBy/minBy" {
        usersTable.maxBy(User::age)?.username shouldBe "Sergey"
        usersTable.minBy(User::age)?.username shouldBe "Kevin"
    }

    "contains" {
        (defaultUsers[2] in usersTable) shouldBe true
        (defaultUsers in usersTable) shouldBe true

        (User(username = "Anonymous") in usersTable) shouldBe false
        (defaultUsers[2].copy(enabled = false) in usersTable) shouldBe false
    }

    "all/any/none" {
        usersTable.all { User::age greater 10 } shouldBe true
        usersTable.all { User::age greater 50 } shouldBe false

        usersTable.any { User::age greater 50 } shouldBe true
        usersTable.any { User::age greater 150 } shouldBe false

        usersTable.none { User::age greater 150 } shouldBe true
        usersTable.none { User::age greater 50 } shouldBe false
    }

    "getColumn" {
        usersTable[User::username] shouldBe usersTable.getAll().map(User::username)
        usersTable.getColumn(User::username) { User::username startsWith "S" } shouldBe usersTable.getAll().map(User::username).filter { it.startsWith('S') }
    }

    "aggregateBy" {
        usersTable.aggregateBy(User::age, SqlList::sum) shouldBe usersTable.getColumn(User::age).sum()
    }

    "groupAggregate" {
        //val map = Table<Address>().groupAggregate(Address::country, Address::id, { count() })
        val map = Table<Address>().groupAggregate(Address::country, Address::id, SqlList::count)
        println(map)
        map["USA"] shouldBe 2

        Table<Address>().groupCounts(Address::country) shouldBe map
        Table<Address>().groupAggregate(Address::country, Address::id, { count() }) { it eq 2 }.size shouldBe 1
    }

    "asSequence" {
        usersTable.asSequence(2).filter { it.username.startsWith('S') }.map { it.age }.take(2).toList() shouldBe listOf(22, 67)
        usersTable.asSequence(2).filter { it.username.startsWith('S') }.map { it.age }.take(4).toList() shouldBe listOf(22, 67, 34)
    }
}
