package entities

import Entity
import column
import databases.Database
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import sql_type_functions.SqlDate
import table
import java.math.BigDecimal
import java.sql.Date
import java.sql.Time
import java.sql.Timestamp
import java.util.*

@Serializable
data class Test(
    override var id: Int = 0,
    var stringValue: String = "",
    var boolValue: Boolean = false,
    var shortValue: Short = 0,
    var intValue: Int = 0,
    var longValue: Long = 0L,
    var floatValue: Float = 0F,
    var doubleValue: Double = 0.0,
    @Contextual var decimalValue: BigDecimal = BigDecimal(0),
    @Contextual var dateValue: Date = Date(0),
    @Contextual var calendarValue: Calendar = Calendar.getInstance(),
    @Contextual var timeValue: Time = Time(0),
    @Contextual var timestampValue: Timestamp = Timestamp(0),

    var uniqueValue: Int = 0,
) : Entity()


val TestTable = table<Test, Database> {
    column(Test::uniqueValue).unique()
    column(Test::calendarValue).default(SqlDate.now())
}
