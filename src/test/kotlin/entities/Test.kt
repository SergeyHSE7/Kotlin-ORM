package entities

import Entity
import Table

data class Test(
    override var id: Int = 0,
    var string: String = "",
    var int: Int = 0,
) : Entity()

val defaultTestEntities = listOf(
    Test(string = "str1", int = 1),
    Test(string = "str2", int = 2),
)

object TestTable : Table<Test>(Test::class, true, {
    varchar(Test::string).unique()
    int(Test::int)

}, defaultTestEntities)
