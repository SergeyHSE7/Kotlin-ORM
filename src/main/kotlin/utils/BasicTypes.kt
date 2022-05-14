package utils

import java.math.BigDecimal
import java.sql.Date
import java.sql.Time
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
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
internal val localDateType = LocalDate::class.createType()
internal val ktLocalDateType = kotlinx.datetime.LocalDate::class.createType()
internal val calendarType = Calendar::class.createType()
internal val instantType = Instant::class.createType()
internal val ktInstantType = kotlinx.datetime.Instant::class.createType()
internal val timeType = Time::class.createType()
internal val localTimeType = LocalTime::class.createType()
internal val timestampType = Timestamp::class.createType()
internal val localDateTimeType = LocalDateTime::class.createType()
internal val ktLocalDateTimeType = kotlinx.datetime.LocalDateTime::class.createType()
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
    localDateType,
    ktLocalDateType,
    calendarType,
    instantType,
    ktInstantType,
    timeType,
    localTimeType,
    timestampType,
    localDateTimeType,
    ktLocalDateTimeType,
    stringType
)


internal val KMutableProperty1<*, *>.type: KType?
    get() = typeList.firstOrNull(::isTypeOf)

fun KMutableProperty1<*, *>.isTypeOf(type: KType) = returnType.isSupertypeOf(type)
