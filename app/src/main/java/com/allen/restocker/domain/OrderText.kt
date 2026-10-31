package com.allen.restocker.domain

import com.allen.restocker.data.FlagEntity
import com.allen.restocker.data.ItemEntity
import com.allen.restocker.data.LineEntity
import com.allen.restocker.data.TypeEntity

data class BuiltOrder(val text: String, val lineCount: Int)

/** Turns the draft lines into the text that gets sent. Same layout as the old PWA. */
object OrderText {

    fun isNumeric(qty: String): Boolean = qty.isNotEmpty() && qty.all { it in '0'..'9' }

    fun formatLine(item: ItemEntity, typeName: String, qty: String): String {
        val q = if (isNumeric(qty)) "${qty}pcs." else qty
        val t = typeName.trim()
        return if (item.typeOnlyExport) {
            if (t.isEmpty() || t == item.name) "$q ${item.name}" else "$q $t"
        } else {
            if (t.isEmpty() || t == item.name) "$q ${item.name}" else "$q ${item.name} $t"
        }
    }

    private class Row(val line: LineEntity, val typeName: String, val order: Long)

    fun build(
        header: String,
        items: List<ItemEntity>,
        types: List<TypeEntity>,
        flags: List<FlagEntity>,
        lines: List<LineEntity>
    ): BuiltOrder {
        val typesById = types.associateBy { it.id }
        val linesByItem = lines.groupBy { it.itemId }

        val normal = mutableListOf<String>()
        val flagged = LinkedHashMap<String, MutableList<String>>()
        flags.sortedBy { it.sortIndex }.forEach { flagged[it.id] = mutableListOf() }

        var count = 0

        for (item in items.sortedBy { it.sortIndex }) {
            val itemLines = linesByItem[item.id] ?: continue

            val rows = ArrayList<Row>()
            for (line in itemLines) {
                val typeId = line.typeId
                if (typeId == null) {
                    rows.add(Row(line, line.customName, 1_000_000_000L + line.id))
                } else {
                    val type = typesById[typeId]
                    if (type != null && type.itemId == item.id) {
                        rows.add(Row(line, type.name, type.sortIndex.toLong()))
                    }
                }
            }
            rows.sortBy { it.order }

            for (row in rows) {
                val qty = row.line.qty.trim()
                if (qty.isEmpty()) continue
                val text = formatLine(item, row.typeName, qty)
                val bucket = flagged[row.line.flag]
                if (bucket != null) bucket.add(text) else normal.add(text)
                count++
            }
        }

        val sections = mutableListOf<String>()
        if (normal.isNotEmpty()) sections.add(normal.joinToString("\n"))
        for (flag in flags.sortedBy { it.sortIndex }) {
            val list = flagged[flag.id] ?: continue
            if (list.isNotEmpty()) {
                sections.add(flag.label.uppercase() + "\n\n" + list.joinToString("\n"))
            }
        }

        if (sections.isEmpty()) return BuiltOrder("", 0)
        return BuiltOrder(header + "\n\n" + sections.joinToString("\n\n"), count)
    }
}
