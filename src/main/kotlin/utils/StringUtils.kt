package utils

import Entity
import column
import org.atteo.evo.inflector.English
import java.sql.Date
import java.sql.Time
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*
import kotlin.reflect.KMutableProperty1

fun String.ifTrue(flag: Boolean) = if (flag) this else ""

internal fun String.transformCase(from: Case, to: Case, toPluralForm: Boolean = false): String {
    val list = splitByCase(from).toMutableList()
    if (toPluralForm)
        list[list.size - 1] = English.plural(list[list.size - 1])
    return list.joinWithCase(to)
}

private fun String.splitByCase(case: Case): List<String> {
    val res = mutableListOf<String>()
    var temp = ""
    when (case) {
        Case.Normal, Case.Pascal, Case.Camel -> this.forEach { c ->
            if (c.isUpperCase()) {
                res.add(temp)
                temp = ""
            }
            if (c != ' ')
                temp += c.lowercaseChar()
        }.also { res.add(temp) }
        Case.Snake -> res.addAll(this.split("_"))
        Case.Kebab -> res.addAll(this.split("-"))
    }
    return res.apply { removeIf { it.isEmpty() } }
}

private fun List<String>.joinWithCase(case: Case): String = when (case) {
    Case.Normal -> this.fold("") { acc, s -> acc + s.capitalize() + ' ' }.trim()
    Case.Pascal -> this.fold("") { acc, s -> acc + s.capitalize() }
    Case.Camel -> this.reduce { acc, s -> acc + s.capitalize() }
    Case.Snake -> this.joinToString("_")
    Case.Kebab -> this.joinToString("-")
}

private fun String.capitalize(): String =
    replaceFirstChar { it.uppercaseChar() }

enum class Case {
    Normal, Pascal, Snake, Camel, Kebab
}


fun <T : Any?> T.toSql() = when (this) {
    is Calendar -> "'${SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SS").format(time)}'"
    is String, is Date, is Time, is Timestamp -> "'$this'"
    is Entity -> id.toString()
    is List<Any?> -> joinToString(", ", "(", ")") { if (it is String) "'$it'" else it.toString() }
    is KMutableProperty1<*, *> -> column.fullName
    else -> this.toString()
}
