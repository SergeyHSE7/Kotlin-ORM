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
import java.util.*
import kotlin.collections.HashMap
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KType

class MariaDB(
    url: String,
    user: String,
    password: String,
) : Database(url, user, password, "org.mariadb.jdbc.Driver") {

    override val reservedKeyWords: List<String> =
        File("src\\main\\kotlin\\databases\\MariaDB Reserved Keywords.txt").readText().split(',')

    override val defaultTypesMap: HashMap<KType, SqlType<*>> = hashMapOf(
        boolType to SqlType<Boolean>("bool"),
        int1Type to SqlType<Byte>("int1"),
        int2Type to SqlType<Short>("int2"),
        int4Type to SqlType<Int>("int4"),
        int8Type to SqlType<Long>("int8"),
        timeType to SqlType<Time>("time"),
        dateType to SqlType<Date>("date"),
        calendarType to SqlType<Calendar>("date",
            customSetValue = { ps, index, value -> ps.setDate(index, Date(value.timeInMillis)) },
            customGetValue = { rs, name -> Calendar.getInstance().apply { time = rs.getDate(name) } }),
        decimalType to SqlType<BigDecimal>("decimal(10, 2)"),
        floatType to SqlType<Float>("float"),
        doubleType to SqlType<Double>("double"),
        stringType to SqlType<String>("text"),
        timestampType to SqlType<Timestamp>("char(30)",
            customSetValue = { ps, index, value -> ps.setString(index, value.toString()) },
            customGetValue = { rs, name -> Timestamp.valueOf(rs.getString(name)) }),
    )

    override fun <E : Entity> idColumn(table: Table<E>, prop: KMutableProperty1<E, Int>): Column<E, Int> =
        Column(table, prop, SqlType("int auto_increment")).primaryKey()

}
