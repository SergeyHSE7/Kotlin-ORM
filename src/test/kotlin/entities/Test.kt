package entities

import Entity
import databases.PostgreSql
import kotlinx.serialization.Serializable
import table

@Serializable
data class Test(
    override var id: Int = 0,
    var string: String = "",
    var int: Int = 0,
) : Entity()


val TestTable = table<Test, PostgreSql> {
    varchar(Test::string).unique()
    int4(Test::int)

    defaultEntities {
        listOf(
            Test(string = "str1", int = 1),
            Test(string = "str2", int = 2),
        )
    }
}
