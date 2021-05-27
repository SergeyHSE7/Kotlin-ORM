import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.SQLException

class Database(
    url: String,
    user: String,
    password: String,
    driver: String = "org.postgresql.Driver"
) {
    val connection: Connection = DriverManager.getConnection(url, user, password)

    init {
        if (driver.isNotBlank())
            Class.forName(driver)
    }

    fun executeSql(sql: String) {
        try {
            connection.createStatement().execute(sql)
        } catch (ex: SQLException) {
            println("SQL-EXCEPTION: ${ex.message}")
        }
    }

    fun executeQuery(sql: String): ResultSet = connection.createStatement().executeQuery(sql)
}


