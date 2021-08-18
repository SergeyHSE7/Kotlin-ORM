package entities

import Entity
import autoTable
import databases.PostgreSQL
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.sql.Date
import java.sql.Time
import java.sql.Timestamp

@Serializable
data class Test(
    override var id: Int = 0,
    var string: String = "",
    var bool: Boolean = false,
    var short: Short = 0,
    var int: Int = 0,
    var long: Long = 0L,
    var float: Float = 0F,
    var double: Double = 0.0,
    @Contextual var decimal: BigDecimal = BigDecimal(0),
    @Contextual var date: Date = Date(0),
    @Contextual var time: Time = Time(0),
    @Contextual var timestamp: Timestamp = Timestamp(0)
) : Entity()


val TestTable = autoTable<Test, PostgreSQL> {
    varchar(Test::string).unique()

    defaultEntities {
        listOf(
            Test(string = "str1", int = 1),
            Test(string = "str2", int = 2),
        )
    }
}
