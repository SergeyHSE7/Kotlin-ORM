package databases

import Column
import Entity
import Table
import autoColumn
import column
import utils.*
import java.math.BigDecimal
import java.sql.Date
import java.sql.Time
import java.sql.Timestamp
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KType

class PostgreSql(
    url: String,
    user: String,
    password: String,
) : Database(url, user, password, "org.postgresql.Driver") {
    override val reservedKeyWords: List<String> =
        executeQuery("SELECT word FROM pg_get_keywords() WHERE catcode = 'R'").map { getString("word") }

    override val columnTypesMap: HashMap<KType, String> = hashMapOf(
        boolType to "boolean",
        int1Type to "int1",
        int2Type to "int2",
        int4Type to "int4",
        int8Type to "int8",
        timeType to "time",
        timestampType to "timestamp",
        dateType to "date",
        decimalType to "decimal(10, 2)",
        floatType to "float",
        doubleType to "double",
        stringType to "text",
    )

    override fun <E : Entity> idColumn(table: Table<E>, prop: KMutableProperty1<E, Int>) =
        Column(table, prop, "serial").primaryKey()


    inline fun <reified E : Entity, T : UByte?> byte(prop: KMutableProperty1<E, T>) = autoColumn(prop)

    inline fun <reified E : Entity, T : Float?> float(prop: KMutableProperty1<E, T>) = autoColumn(prop)

    inline fun <reified E : Entity, T : BigDecimal?> decimal(prop: KMutableProperty1<E, T>, precision: Int, scale: Int) =
        column(prop, "decimal($precision, $scale)")
    inline fun <reified E : Entity, T : BigDecimal?> numeric(prop: KMutableProperty1<E, T>, precision: Int, scale: Int) =
        column(prop, "numeric($precision, $scale)")

    inline fun <reified E : Entity, T : String?> varchar(prop: KMutableProperty1<E, T>, size: Int = 60) =
        column(prop, "varchar($size)")
    inline fun <reified E : Entity, T : String?> char(prop: KMutableProperty1<E, T>, size: Int = 60) =
        column(prop, "char($size)")
    inline fun <reified E : Entity, T : String?> text(prop: KMutableProperty1<E, T>) = autoColumn(prop)

    inline fun <reified E : Entity, T : String?> json(prop: KMutableProperty1<E, T>) = column(prop, "json")
    inline fun <reified E : Entity, T : String?> uuid(prop: KMutableProperty1<E, T>) = column(prop, "uuid")


    inline fun <reified E : Entity, T : Date?> date(prop: KMutableProperty1<E, T>) = autoColumn(prop)
    inline fun <reified E : Entity, T : Time?> time(prop: KMutableProperty1<E, T>, withTimeZone: Boolean = false) =
        column(prop, "time" + " with time zone".ifTrue(withTimeZone))

    inline fun <reified E : Entity, T : Timestamp?> timestamp(prop: KMutableProperty1<E, T>, withTimeZone: Boolean = false) =
        column(prop, "timestamp" + " with time zone".ifTrue(withTimeZone))
}
