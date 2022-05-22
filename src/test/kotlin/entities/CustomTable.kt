package entities

import Entity
import column
import databases.Database
import table


data class CustomTable(
    override var id: Int = 0,
    var field: String = "",
) : Entity()

val customTable = table<CustomTable, Database>("table_with_custom_name") {
    column(CustomTable::field).name("column_with_custom_name")

    defaultEntities { listOf(CustomTable(1, "value")) }
}