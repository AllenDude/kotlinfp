package com.allen.restocker.domain

import com.allen.restocker.data.CatalogItem
import com.allen.restocker.data.FlagEntity
import com.allen.restocker.data.ItemEntity
import com.allen.restocker.data.LineEntity
import com.allen.restocker.data.TypeEntity
import java.net.URLDecoder
import java.net.URLEncoder

data class Decoded(val lines: List<LineEntity>, val skipped: Int)

/**
 * Share codes for moving a list between phones.
 *
 * New format (stable ids, so editing the catalog never breaks an old code):
 *   ORD2:itemId|typeId|qty|flag|customName;itemId|typeId|qty|flag|customName;...
 * Every field is URL-encoded, so "|" and ";" can never appear inside one.
 *
 * Old PWA codes ("0001" + "03" + qty + flag digit, joined by ";") are still understood.
 */
object Codes {

    const val PREFIX = "ORD2:"

    private val legacyFlags = listOf("", "buo", "chichirya", "promo")

    private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")
    private fun dec(s: String): String = URLDecoder.decode(s, "UTF-8")

    fun encode(lines: List<LineEntity>): String {
        val parts = lines
            .filter { it.qty.isNotBlank() }
            .sortedBy { it.id }
            .map { l ->
                listOf(l.itemId, l.typeId ?: "", l.qty.trim(), l.flag, l.customName.trim())
                    .joinToString("|") { enc(it) }
            }
        return if (parts.isEmpty()) "" else PREFIX + parts.joinToString(";")
    }

    fun decode(
        raw: String,
        items: List<ItemEntity>,
        types: List<TypeEntity>,
        flags: List<FlagEntity>,
        legacySeed: List<CatalogItem>
    ): Decoded {
        val compact = raw.filterNot { it.isWhitespace() }
        val result = if (compact.startsWith(PREFIX)) {
            decodeNew(compact.removePrefix(PREFIX), items, types, flags)
        } else {
            decodeLegacy(raw.replace(Regex("[\\r\\n]+"), "").trim(), items, types, legacySeed)
        }
        if (result.lines.isEmpty()) throw IllegalArgumentException("Not a valid code")
        return result
    }

    private fun decodeNew(
        body: String,
        items: List<ItemEntity>,
        types: List<TypeEntity>,
        flags: List<FlagEntity>
    ): Decoded {
        val itemIds = items.map { it.id }.toSet()
        val typesById = types.associateBy { it.id }
        val flagIds = flags.map { it.id }.toSet()

        val lines = mutableListOf<LineEntity>()
        var skipped = 0

        body.split(";").forEach { entry ->
            if (entry.isBlank()) return@forEach
            try {
                val p = entry.split("|").map { dec(it) }
                if (p.size < 4) {
                    skipped++
                    return@forEach
                }
                val itemId = p[0]
                val typeId = p[1]
                val qty = p[2].trim()
                val flag = if (p[3] in flagIds) p[3] else ""
                val custom = if (p.size > 4) p[4] else ""

                if (itemId !in itemIds || qty.isEmpty()) {
                    skipped++
                    return@forEach
                }
                if (typeId.isNotEmpty()) {
                    val type = typesById[typeId]
                    if (type == null || type.itemId != itemId) {
                        skipped++
                        return@forEach
                    }
                    lines.add(LineEntity(orderId = 0, itemId = itemId, typeId = typeId, qty = qty, flag = flag))
                } else {
                    lines.add(LineEntity(orderId = 0, itemId = itemId, typeId = null, customName = custom, qty = qty, flag = flag))
                }
            } catch (e: IllegalArgumentException) {
                skipped++
            }
        }
        return Decoded(lines, skipped)
    }

    private fun decodeLegacy(
        code: String,
        items: List<ItemEntity>,
        types: List<TypeEntity>,
        seed: List<CatalogItem>
    ): Decoded {
        val itemIds = items.map { it.id }.toSet()
        val typesById = types.associateBy { it.id }
        val pattern = Regex("^(\\d{4})(\\d{2})([\\s\\S]+)([0-3])$")

        val lines = mutableListOf<LineEntity>()
        var skipped = 0

        code.split(";").forEach { rawEntry ->
            val entry = rawEntry.trim()
            if (entry.isEmpty()) return@forEach

            val m = pattern.find(entry)
            if (m == null) {
                skipped++
                return@forEach
            }

            val seedItem = seed.getOrNull(m.groupValues[1].toInt() - 1)
            if (seedItem == null || seedItem.id !in itemIds) {
                skipped++
                return@forEach
            }

            val typeNo = m.groupValues[2].toInt()
            var qty = m.groupValues[3]
            val flag = legacyFlags[m.groupValues[4].toInt()]

            if (typeNo == 0) {
                var custom = ""
                val at = qty.indexOf("@")
                if (at != -1) {
                    custom = qty.substring(at + 1)
                    qty = qty.substring(0, at)
                }
                qty = qty.trim()
                if (qty.isEmpty()) {
                    skipped++
                    return@forEach
                }
                lines.add(LineEntity(orderId = 0, itemId = seedItem.id, typeId = null, customName = custom, qty = qty, flag = flag))
            } else {
                val seedType = seedItem.types.getOrNull(typeNo - 1)
                val type = seedType?.let { typesById[it.id] }
                qty = qty.trim()
                if (type == null || type.itemId != seedItem.id || qty.isEmpty()) {
                    skipped++
                    return@forEach
                }
                lines.add(LineEntity(orderId = 0, itemId = seedItem.id, typeId = type.id, qty = qty, flag = flag))
            }
        }
        return Decoded(lines, skipped)
    }
}
