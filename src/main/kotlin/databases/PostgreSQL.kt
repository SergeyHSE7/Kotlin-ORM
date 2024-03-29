package databases

import Column
import Entity
import Table
import column
import statements.DropStatement
import statements.SelectStatement
import utils.*
import java.math.BigDecimal
import java.sql.Date
import java.sql.Time
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KType

/**
 * Represents a class for PostgreSQL database.
 *
 * @param[url] A database url of the form jdbc:postgresql:subname.
 * @param[user] The database user on whose behalf the connection is being made.
 * @param[password] The user's password to the database.
 */
class PostgreSQL(
    url: String,
    user: String,
    password: String,
) : Database(url, user, password, "org.postgresql.Driver") {
    override val reservedKeyWords: List<String> =
        "all,analyse,analyze,and,any,array,as,asc,asymmetric,both,case,cast,check,collate,column,constraint,create,current_catalog,current_date,current_role,current_time,current_timestamp,current_user,default,deferrable,desc,distinct,do,else,end,except,false,fetch,for,foreign,from,grant,group,having,in,initially,intersect,into,lateral,leading,limit,localtime,localtimestamp,not,null,offset,on,only,or,order,placing,primary,references,returning,select,session_user,some,symmetric,table,then,to,trailing,true,union,unique,user,using,variadic,when,where,window,with"
            .split(',')

    override val defaultTypesMap: HashMap<KType, SqlType<*>> = hashMapOf(
        boolType to SqlType<Boolean>("boolean"),
        int2Type to SqlType<Short>("int2"),
        int4Type to SqlType<Int>("int4"),
        int8Type to SqlType<Long>("int8"),
        timeType to SqlType<Time>("time"),
        timestampType to SqlType<Timestamp>("timestamp"),
        dateType to SqlType<Date>("date"),
        localDateType to SqlType<LocalDate>("text",
            customGetValue = { rs, name -> LocalDate.parse(rs.getString(name)) },
            customSetValue = { ps, index, value -> ps.setString(index, value.toString()) }),
        localDateTimeType to SqlType<LocalDateTime>("text",
            customGetValue = { rs, name -> LocalDateTime.parse(rs.getString(name)) },
            customSetValue = { ps, index, value -> ps.setString(index, value.toString()) }),
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


    override val dropStatementSql: DropStatement<*>.() -> String =
        { "DROP TABLE IF EXISTS ${table.tableName} CASCADE" }

    override val selectStatementSql: SelectStatement<*>.() -> String = {
        "SELECT${getSelectValues()} FROM ${table.tableName}" +
                joinTables.joinToString("") +
                (groupColumn ?: "") +
                whereStatement.getSql() +
                (" ORDER BY " + orderColumns.joinToString()).ifTrue(orderColumns.isNotEmpty()) +
                " LIMIT $limit".ifTrue(limit != null) + " OFFSET $offset".ifTrue(offset != 0)
    }
}
