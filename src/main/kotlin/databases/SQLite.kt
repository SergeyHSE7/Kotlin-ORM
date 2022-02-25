package databases

import Column
import Entity
import Table
import utils.*
import java.math.BigDecimal
import java.sql.Date
import java.sql.Time
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KType

/**
 * Represents a class for PostgreSQL database.
 *
 * @param[url] A database url of the form jdbc:sqlite:subname.
 */
class SQLite(
    url: String
) : Database(url, driver = "org.sqlite.JDBC") {
    override val reservedKeyWords: List<String> =
        "abort,action,add,after,all,alter,always,analyze,and,as,asc,attach,autoincrement,before,begin,between,by,cascade,case,cast,check,collate,column,commit,conflict,constraint,create,cross,current,current_date,current_time,current_timestamp,database,default,deferrable,deferred,delete,desc,detach,distinct,do,drop,each,else,end,escape,except,exclude,exclusive,exists,explain,fail,filter,first,following,for,foreign,from,full,generated,glob,group,groups,having,if,ignore,immediate,in,index,indexed,initially,inner,insert,instead,intersect,into,is,isnull,join,key,last,left,like,limit,match,materialized,natural,no,not,nothing,notnull,null,nulls,of,offset,on,or,order,others,outer,over,partition,plan,pragma,preceding,primary,query,raise,range,recursive,references,regexp,reindex,release,rename,replace,restrict,returning,right,rollback,row,rows,savepoint,select,set,table,temp,temporary,then,ties,to,transaction,trigger,unbounded,union,unique,update,using,vacuum,values,view,virtual,when,where,window,with,without"
            .split(',')


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
