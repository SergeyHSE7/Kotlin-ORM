package utils

import Entity
import kotlin.jvm.internal.MutablePropertyReference1
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

fun KProperty1<out Entity, *>.returnValue(receiver: Any): Any? = getter.call(receiver)

val KProperty1<*, *>.columnName: String
    get() = name.transformCase(Case.Camel, Case.Snake)

private val KProperty1<*, *>.tableName: String?
    get() = with((this as? MutablePropertyReference1)?.owner as? KClass<*>) {
        if (this?.isAbstract == true) null
        else this?.simpleName?.transformCase(Case.Pascal, Case.Snake, true)
    }


val KProperty1<*, *>.fullColumnName: String
    get() {
        val tableName = this.tableName
        return if (tableName != null) "$tableName.$columnName" else columnName
    }

