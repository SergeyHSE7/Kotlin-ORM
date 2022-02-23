package databases

import Column
import Entity
import Table
import utils.*
import java.io.File
import java.math.BigDecimal
import java.sql.Date
import java.sql.Time
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KType

class SQLite(
    url: String
) : Database(url, driver = "org.sqlite.JDBC") {
    override val reservedKeyWords: List<String> =
        File("src\\main\\kotlin\\databases\\SQLite Reserved Keywords.txt").readText().split(',')


    override val defaultTypesMap: HashMap<KType, SqlType<*>> = hashMapOf(
        boolType to SqlType(
            "integer",
            customGetValue = { rs, name -> rs.getInt(name) == 1 },
            customSetValue = { ps, index, value -> ps.setInt(index, if (value) 1 else 0) }),
        int1Type to SqlType<Byte>("integer"),
        int2Type to SqlType<Short>("integer"),
        int4Type to SqlType<Int>("integer"),
        int8Type to SqlType<Long>("integer"),
        floatType to SqlType<Float>("real"),
        doubleType to SqlType<Double>("real"),
        stringType to SqlType<String>("text"),
        dateType to SqlType<Date>("text",
            customGetValue = { rs, name -> Date.valueOf(rs.getString(name)) },
            customSetValue = { ps, index, value -> ps.setString(index, value.toString()) }),
        calendarType to SqlType<Calendar>("text",
            customGetValue = { rs, name ->
                Calendar.getInstance()
                    .apply { time = SimpleDateFormat("YYYY-MM-dd HH:mm:ss.SSS").parse(rs.getString(name)) }
            },
            customSetValue = { ps, index, value ->
                ps.setString(index, SimpleDateFormat("YYYY-MM-dd HH:mm:ss.SSS").format(value.time))
            }),
        timeType to SqlType<Time>("text",
            customGetValue = { rs, name -> Time.valueOf(rs.getString(name)) },
            customSetValue = { ps, index, value -> ps.setString(index, value.toString()) }),
        timestampType to SqlType<Timestamp>("text",
            customGetValue = { rs, name -> Timestamp.valueOf(rs.getString(name)) },
            customSetValue = { ps, index, value -> ps.setString(index, value.toString()) }),
        decimalType to SqlType("text",
            customGetValue = { rs, name -> BigDecimal(rs.getString(name)) },
            customSetValue = { ps, index, value -> ps.setString(index, value.toString()) }),
    )

    override fun <E : Entity> idColumn(table: Table<E>, prop: KMutableProperty1<E, Int>): Column<E, Int> =
        Column(table, prop, SqlType("integer")).primaryKey()
}
