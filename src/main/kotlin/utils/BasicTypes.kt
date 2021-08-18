package utils

import java.math.BigDecimal
import java.sql.Date
import java.sql.Time
import java.sql.Timestamp
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSupertypeOf


val boolType = Boolean::class.createType()
val int1Type = Byte::class.createType()
val int2Type = Short::class.createType()
val int4Type = Int::class.createType()
val int8Type = Long::class.createType()
val decimalType = BigDecimal::class.createType()
val floatType = Float::class.createType()
val doubleType = Double::class.createType()
val dateType = Date::class.createType()
val timeType = Time::class.createType()
val timestampType = Timestamp::class.createType()
val stringType = String::class.createType()

val typeList = listOf(
    boolType,
    int1Type,
    int2Type,
    int4Type,
    int8Type,
    decimalType,
    floatType,
    doubleType,
    dateType,
    timeType,
    timestampType,
    stringType
)


val KMutableProperty1<*, *>.type: KType?
    get() = typeList.firstOrNull { returnType.isSupertypeOf(it) }
