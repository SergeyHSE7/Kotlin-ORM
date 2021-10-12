import io.kotest.core.spec.style.scopes.FreeSpecContainerContext
import io.kotest.matchers.shouldBe
import sql_type_functions.SqlBase
import sql_type_functions.SqlDate
import utils.timestampType
import java.sql.Timestamp
import java.time.Instant
import kotlin.reflect.KType

fun <T> select(str: String, propType: KType): T {
    val obj = database.executeQuery("SELECT $str").apply { next() }.getObject(1)
    return when {
        propType == timestampType && (obj is String) -> Timestamp.valueOf(obj)
        else -> obj
    } as T
}
fun <T> select(sqlBase: SqlBase, propType: KType): T = select(sqlBase.toString(), propType)

fun Timestamp.nearly(other: Timestamp, epsMillis: Int = 1000) = kotlin.math.abs(this.time - other.time) < epsMillis


suspend inline fun FreeSpecContainerContext.sqlFunctionsTests() {

    "now()" {
        Timestamp.from(Instant.now()).nearly(select(SqlDate.now(), timestampType)) shouldBe true
    }
}
