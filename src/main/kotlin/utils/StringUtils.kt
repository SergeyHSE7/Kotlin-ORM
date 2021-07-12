package utils

import org.atteo.evo.inflector.English
import java.util.*

fun String.ifTrue(flag: Boolean) = if (flag) this else ""

fun String.transformCase(from: Case, to: Case, toPluralForm: Boolean = false): String {
    val list = splitByCase(from).toMutableList()
    if (toPluralForm)
        list[list.size - 1] = English.plural(list[list.size - 1])
    return list.joinWithCase(to)
}

fun String.splitByCase(case: Case): List<String> {
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

fun List<String>.joinWithCase(case: Case): String = when (case) {
    Case.Normal -> this.fold("") { acc, s -> acc + s.capitalize() + ' ' }.trim()
    Case.Pascal -> this.fold("") { acc, s -> acc + s.capitalize() }
    Case.Camel -> this.reduce { acc, s -> acc + s.capitalize() }
    Case.Snake -> this.joinToString("_")
    Case.Kebab -> this.joinToString("-")
}

fun String.capitalize(): String =
    replaceFirstChar { it.uppercaseChar() }

enum class Case {
    Normal, Pascal, Snake, Camel, Kebab
}

