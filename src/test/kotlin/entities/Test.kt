package entities

import Entity
import column
import databases.Database
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import table
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
    @Contextual var timestamp: Timestamp = Timestamp(0),

    var uniqueValue: Int = 0,
) : Entity()


val TestTable = table<Test, Database> {
    column(Test::uniqueValue).unique()
}
