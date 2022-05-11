package databases

import Column
import Entity
import Reference
import Table
import statements.AlterStatement
import statements.InsertStatement
import statements.SelectStatement
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
 * Represents a class for MariaDB database.
 *
 * @param[url] A database url of the form jdbc:mariadb:subname.
 * @param[user] The database user on whose behalf the connection is being made.
 * @param[password] The user's password to the database.
 */
class MariaDB(
    url: String,
    user: String,
    password: String,
) : Database(url, user, password, "org.mariadb.jdbc.Driver") {

    override val reservedKeyWords: List<String> =
        "accessible,add,all,alter,analyze,and,as,asc,asensitive,before,between,bigint,binary,blob,both,by,call,cascade,case,change,char,character,check,collate,column,condition,constraint,continue,convert,create,cross,current_date,current_role,current_time,current_timestamp,current_user,cursor,database,databases,day_hour,day_microsecond,day_minute,day_second,dec,decimal,declare,default,delayed,delete,desc,describe,deterministic,distinct,distinctrow,div,do_domain_ids,double,drop,dual,each,else,elseif,enclosed,escaped,except,exists,exit,explain,false,fetch,float,float4,float8,for,force,foreign,from,fulltext,general,grant,group,having,high_priority,hour_microsecond,hour_minute,hour_second,if,ignore,ignore_domain_ids,ignore_server_ids,in,index,infile,inner,inout,insensitive,insert,int,int1,int2,int3,int4,int8,integer,intersect,interval,into,is,iterate,join,key,keys,kill,leading,leave,left,like,limit,linear,lines,load,localtime,localtimestamp,lock,long,longblob,longtext,loop,low_priority,master_heartbeat_period,master_ssl_verify_server_cert,match,maxvalue,mediumblob,mediumint,mediumtext,middleint,minute_microsecond,minute_second,mod,modifies,natural,not,no_write_to_binlog,null,numeric,on,optimize,option,optionally,or,order,out,outer,outfile,over,page_checksum,parse_vcol_expr,partition,precision,primary,procedure,purge,range,read,reads,read_write,real,recursive,ref_system_id,references,regexp,release,rename,repeat,replace,require,resignal,restrict,return,returning,revoke,right,rlike,rows,schema,schemas,second_microsecond,select,sensitive,separator,set,show,signal,slow,smallint,spatial,specific,sql,sqlexception,sqlstate,sqlwarning,sql_big_result,sql_calc_found_rows,sql_small_result,ssl,starting,stats_auto_recalc,stats_persistent,stats_sample_pages,straight_join,table,terminated,then,tinyblob,tinyint,tinytext,to,trailing,trigger,true,undo,union,unique,unlock,unsigned,update,usage,use,using,utc_date,utc_time,utc_timestamp,values,varbinary,varchar,varcharacter,varying,when,where,while,window,with,write,xor,year_month,zerofill"
            .split(',')

    override val defaultTypesMap: HashMap<KType, SqlType<*>> = hashMapOf(
        boolType to SqlType<Boolean>("bool"),
        int1Type to SqlType<Byte>("int1"),
        int2Type to SqlType<Short>("int2"),
        int4Type to SqlType<Int>("int4"),
        int8Type to SqlType<Long>("int8"),
        timeType to SqlType<Time>("time"),
        dateType to SqlType<Date>("date"),
        calendarType to SqlType<Calendar>("char(30)",
            customGetValue = { rs, name ->
                Calendar.getInstance()
                    .apply { time = SimpleDateFormat("YYYY-MM-dd HH:mm:ss.SSS").parse(rs.getString(name)) }
            },
            customSetValue = { ps, index, value ->
                ps.setString(index, SimpleDateFormat("YYYY-MM-dd HH:mm:ss.SSS").format(value.time))
            }),
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

    override val alterStatementSql: AlterStatement<*>.(reference: Reference<*, *>) -> String = {
        "ALTER TABLE ${table.tableName} ADD CONSTRAINT ${it.getForeignKey()}"
    }
    override val insertStatementSql: InsertStatement<*>.(preparedEntities: List<*>) -> String = {
        "INSERT IGNORE INTO ${table.tableName} " +
                "(${table.columns.filter { it.name != "id" }.joinToString { it.name }}) " +
                "VALUES ${it.joinToString { "(${props.joinToString { "?" }})" }} " +
                "RETURNING ${if (getEntity) "*" else "id"}"
    }

    override val selectStatementSql: SelectStatement<*>.() -> String = {
        "SELECT${getSelectValues()} FROM ${table.tableName}" +
                joinTables.joinToString("") +
                (groupColumn ?: "") +
                whereStatement.getSql() +
                (" ORDER BY " + orderColumns.joinToString()).ifTrue(orderColumns.isNotEmpty()) +
                if (offset != 0) " LIMIT ${limit ?: (table.size - offset)} OFFSET $offset"
                else " LIMIT $limit".ifTrue(limit != null)
    }
}
