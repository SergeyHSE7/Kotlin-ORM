import io.kotest.core.spec.style.scopes.FreeSpecContainerContext
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.comparables.shouldBeLessThanOrEqualTo
import sql_type_functions.SqlBase
import sql_type_functions.SqlDate
import utils.timestampType
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*
import kotlin.math.abs
import kotlin.reflect.KType

fun <T> select(str: String, propType: KType): T {
    val obj = database.executeQuery("SELECT $str").apply { next() }.getObject(1)
    return when {
        propType == timestampType && (obj is String) -> Timestamp.valueOf(obj)
        else -> obj
    } as T
}
fun <T> select(sqlBase: SqlBase, propType: KType): T = select(sqlBase.toString(), propType)

fun LocalDateTime.toMillis(zone: ZoneId = ZoneId.systemDefault()) = atZone(zone).toInstant().toEpochMilli()

suspend inline fun FreeSpecContainerContext.sqlFunctionsTests() {

    "now()" {
        val sqlTime = (select(SqlDate.now(), timestampType) as Timestamp).also(::println).time / 1000
        val time = (Timestamp.from(Instant.now()).time - TimeZone.getDefault().rawOffset) / 1000
        println(sqlTime)
        println(time)

        abs(time - sqlTime) shouldBeLessThanOrEqualTo  1
    }

    "nowWithMs()" {
        val sqlTime = (select(SqlDate.nowWithMs(), timestampType) as Timestamp).also(::println).time
        val time = Timestamp.from(Instant.now()).time - TimeZone.getDefault().rawOffset
        println(sqlTime)
        println(time)

        abs(time - sqlTime) shouldBeLessThan 1000
    }
}
