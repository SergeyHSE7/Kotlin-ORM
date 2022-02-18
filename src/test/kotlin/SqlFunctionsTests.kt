import io.kotest.core.spec.style.scopes.FreeSpecContainerContext
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import sql_type_functions.SqlBase
import sql_type_functions.SqlDate
import utils.timestampType
import java.sql.Timestamp
import java.time.Instant
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


suspend inline fun FreeSpecContainerContext.sqlFunctionsTests() {

    "now()" {
        val beginTime = Timestamp.from(Instant.now()).time / 1000
        val sqlTime = (select(SqlDate.nowWithMs(), timestampType) as Timestamp).also(::println).time / 1000
        val endTime = Timestamp.from(Instant.now()).time / 1000
        println(sqlTime)

        (sqlTime in beginTime..endTime) shouldBe true
    }

    "nowWithMs()" {
        val sqlTime = (select(SqlDate.nowWithMs(), timestampType) as Timestamp).also(::println).time
        val time = Timestamp.from(Instant.now()).time
        println(sqlTime)
        println(time)

        abs(time - sqlTime) shouldBeLessThan 300
    }
}
