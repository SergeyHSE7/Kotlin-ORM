import io.kotest.core.spec.style.scopes.FreeSpecContainerContext
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import sql_type_functions.SqlDate
import java.sql.Timestamp
import java.time.Instant
import java.util.*
import kotlin.math.abs


suspend inline fun FreeSpecContainerContext.sqlFunctionsTests() {

    "now()" {
        val sqlTime = SqlDate.now().getFromDB().also(::println).time / 1000
        val time = (Timestamp.from(Instant.now()).time - TimeZone.getDefault().rawOffset) / 1000
        println(sqlTime)
        println(time)

        abs(time - sqlTime) shouldBeLessThanOrEqualTo  2
    }

    "nowWithMs()" {
        val sqlTime = SqlDate.now().getFromDB().also(::println).time
        val time = Timestamp.from(Instant.now()).time - TimeZone.getDefault().rawOffset
        println(sqlTime)
        println(time)

        abs(time - sqlTime) shouldBeLessThan 2000
    }
}
