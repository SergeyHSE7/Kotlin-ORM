package utils

import java.math.BigDecimal
import java.sql.Date
import java.sql.Time
import java.sql.Timestamp
import java.util.*
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.full.isSupertypeOf


internal val boolType = Boolean::class.createType()
internal val int1Type = Byte::class.createType()
internal val int2Type = Short::class.createType()
internal val int4Type = Int::class.createType()
internal val int8Type = Long::class.createType()
internal val decimalType = BigDecimal::class.createType()
internal val floatType = Float::class.createType()
internal val doubleType = Double::class.createType()
internal val dateType = Date::class.createType()
internal val calendarType = Calendar::class.createType()
internal val timeType = Time::class.createType()
internal val timestampType = Timestamp::class.createType()
internal val stringType = String::class.createType()

private val typeList = listOf(
    boolType,
    int1Type,
    int2Type,
    int4Type,
    int8Type,
    decimalType,
    floatType,
    doubleType,
    dateType,
    calendarType,
    timeType,
    timestampType,
    stringType
)


internal val KMutableProperty1<*, *>.type: KType?
    get() = typeList.firstOrNull(::isTypeOf)

fun KMutableProperty1<*, *>.isTypeOf(type: KType) = returnType.isSupertypeOf(type)
