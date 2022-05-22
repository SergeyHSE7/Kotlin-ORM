import entities.Test
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.core.spec.style.scopes.FreeSpecContainerContext
import io.kotest.matchers.shouldBe
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import java.text.ParseException
import java.text.SimpleDateFormat

suspend inline fun FreeSpecContainerContext.typeTests() {
    val test = Table<Test>().first()!!


    "Date & time types from java.sql" {
        with(test) {
            println(dateValue)
            println(timeValue)
            println(timestampValue)

            val dateFormat = SimpleDateFormat("yyyy-MM-dd")
            val timeFormat = SimpleDateFormat("HH:mm:ss")
            val timestampFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S")

            shouldNotThrow<ParseException> {
                dateFormat.parse(dateValue.toString()) shouldBe dateValue
                timeFormat.parse(timeValue.toString()) shouldBe timeValue
                timestampFormat.parse(timestampValue.toString()) shouldBe timestampValue
            }
        }
    }

    "Date & time types from java.util" {
        with(test) {
            println(calendarValue.time)
            println(instantValue)
            println(localDateValue)
            println(localTimeValue)
            println(localDateTimeValue)

            calendarValue.toInstant() shouldBe instantValue
            shouldNotThrow<ParseException> {
                Instant.parse(instantValue.toString())
                java.time.LocalDate.parse(localDateValue.toString()) shouldBe localDateValue
                java.time.LocalTime.parse(localTimeValue.toString()) shouldBe localTimeValue
                java.time.LocalDateTime.parse(localDateTimeValue.toString()) shouldBe localDateTimeValue
            }
        }
    }

    "Date & time types from kotlinx.datetime" {
        with(test) {
            println(ktLocalDateValue)
            println(ktLocalDateTimeValue)
            println(ktInstantValue)

            shouldNotThrow<ParseException> {
                LocalDate.parse(ktLocalDateValue.toString()) shouldBe ktLocalDateValue
                LocalDateTime.parse(ktLocalDateTimeValue.toString()) shouldBe ktLocalDateTimeValue
                Instant.parse(ktInstantValue.toString()) shouldBe ktInstantValue
            }
        }
    }
}