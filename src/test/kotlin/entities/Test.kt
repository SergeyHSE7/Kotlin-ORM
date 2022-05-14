package entities

import Entity
import column
import databases.Database
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import sql_type_functions.SqlDate
import table
import java.math.BigDecimal
import java.sql.Date
import java.sql.Time
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
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
    @Contextual var localDateValue: LocalDate = LocalDate.now(),
    var ktLocalDateValue: kotlinx.datetime.LocalDate =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
    @Contextual var calendarValue: Calendar = Calendar.getInstance(),
    @Contextual var instantValue: Instant = Instant.now(),
    var ktInstantValue: kotlinx.datetime.Instant = Clock.System.now(),
    @Contextual var timeValue: Time = Time(0),
    @Contextual var localTimeValue: LocalTime = LocalTime.now(),
    @Contextual var timestampValue: Timestamp = Timestamp(0),
    @Contextual var localDateTimeValue: LocalDateTime = LocalDateTime.now(),
    var ktLocalDateTimeValue: kotlinx.datetime.LocalDateTime =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),

    var uniqueValue: Int = 0,
) : Entity()


val TestTable = table<Test, Database> {
    column(Test::uniqueValue).unique()
    column(Test::calendarValue).default(SqlDate.now())

    defaultEntities { listOf(
        Test(
            dateValue = Date(1652555279906L),
            timeValue = Time(1652555279906L),
            timestampValue = Timestamp(1652555279906L),
        )
    ) }
}
