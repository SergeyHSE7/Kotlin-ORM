package databases

import Column
import Entity
import Table
import column
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

class PostgreSQL(
    url: String,
    user: String,
    password: String,
) : Database(url, user, password, "org.postgresql.Driver") {
    override val reservedKeyWords: List<String> =
        File("src\\main\\kotlin\\databases\\PostgreSQL Reserved Keywords.txt").readText().split(',')


    override val defaultTypesMap: HashMap<KType, SqlType<*>> = hashMapOf(
        boolType to SqlType<Boolean>("boolean"),
        int2Type to SqlType<Short>("int2"),
        int4Type to SqlType<Int>("int4"),
        int8Type to SqlType<Long>("int8"),
        timeType to SqlType<Time>("time"),
        timestampType to SqlType<Timestamp>("timestamp"),
        dateType to SqlType<Date>("date"),
        calendarType to SqlType<Calendar>("date",
            customSetValue = { ps, index, value -> ps.setDate(index, Date(value.timeInMillis)) },
            customGetValue = { rs, name -> Calendar.getInstance().apply { time = rs.getDate(name) } }),
        decimalType to SqlType<BigDecimal>("decimal(10, 2)"),
        floatType to SqlType<Float>("float"),
        doubleType to SqlType<Double>("double precision"),
        stringType to SqlType<String>("text"),
    )

    override fun <E : Entity> idColumn(table: Table<E>, prop: KMutableProperty1<E, Int>) =
        Column(table, prop, SqlType("serial")).primaryKey()


    inline fun <reified E : Entity, T : BigDecimal?> decimal(
        prop: KMutableProperty1<E, T>,
        precision: Int,
        scale: Int
    ) =
        column(prop, "decimal($precision, $scale)")

    inline fun <reified E : Entity, T : BigDecimal?> numeric(
        prop: KMutableProperty1<E, T>,
        precision: Int,
        scale: Int
    ) =
        column(prop, "numeric($precision, $scale)")

    inline fun <reified E : Entity, T : String?> varchar(prop: KMutableProperty1<E, T>, size: Int = 60) =
        column(prop, "varchar($size)")

    inline fun <reified E : Entity, T : String?> char(prop: KMutableProperty1<E, T>, size: Int = 60) =
        column(prop, "char($size)")

    inline fun <reified E : Entity, T : String?> json(prop: KMutableProperty1<E, T>) = column(prop, "json")
    inline fun <reified E : Entity, T : String?> uuid(prop: KMutableProperty1<E, T>) = column(prop, "uuid")

    inline fun <reified E : Entity, T : Time?> time(prop: KMutableProperty1<E, T>, withTimeZone: Boolean = false) =
        column(prop, "time" + " with time zone".ifTrue(withTimeZone))

    inline fun <reified E : Entity, T : Timestamp?> timestamp(
        prop: KMutableProperty1<E, T>,
        withTimeZone: Boolean = false
    ) =
        column(prop, "timestamp" + " with time zone".ifTrue(withTimeZone))
}
