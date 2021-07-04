import org.tinylog.Logger
import utils.map
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.SQLException

val database: Database = Database(
    url = "jdbc:postgresql://localhost:5432/FinAssistant",
    user = "postgres",
    password = "123456"
)

class Database(
    url: String,
    user: String,
    password: String,
    driver: String = "org.postgresql.Driver"
) {
    val connection: Connection = DriverManager.getConnection(url, user, password)
    val reservedKeyWords: List<String> = executeQuery("SELECT word FROM pg_get_keywords() WHERE catcode = 'R'").map { getString("word") }

    init {
        if (driver.isNotBlank())
            Class.forName(driver)
    }

    fun executeSql(sql: String) {
        try {
            connection.createStatement().execute(sql)
        } catch (ex: SQLException) {
            Logger.error { ex }
        }
    }

    fun executeQuery(sql: String): ResultSet = connection.createStatement().executeQuery(sql)
}


