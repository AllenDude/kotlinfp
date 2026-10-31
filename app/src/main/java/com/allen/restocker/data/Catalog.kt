package com.allen.restocker.data

import org.json.JSONArray
import org.json.JSONObject

data class CatalogType(val id: String, val name: String, val defaultFlag: String = "")

data class CatalogItem(
    val id: String,
    val name: String,
    val category: String,
    val typeOnlyExport: Boolean,
    val hidden: Boolean,
    val types: List<CatalogType>
)

data class CatalogFlag(val id: String, val label: String)

data class Catalog(val flags: List<CatalogFlag>, val items: List<CatalogItem>)

/** Reads and writes the catalog JSON used for the bundled seed and for backups. */
object CatalogJson {

    private val defaultFlags = listOf(
        CatalogFlag("buo", "Buo"),
        CatalogFlag("chichirya", "Chichirya"),
        CatalogFlag("promo", "Promo")
    )

    fun parse(text: String): Catalog {
        val root = JSONObject(text)

        val flags = mutableListOf<CatalogFlag>()
        root.optJSONArray("flags")?.let { arr ->
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                flags.add(CatalogFlag(o.getString("id"), o.optString("label", o.getString("id"))))
            }
        }

        val items = mutableListOf<CatalogItem>()
        val arr = root.getJSONArray("items")
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            val name = o.getString("name")
            val types = mutableListOf<CatalogType>()
            val tArr = o.optJSONArray("types")
            if (tArr != null) {
                for (j in 0 until tArr.length()) {
                    val t = tArr.getJSONObject(j)
                    types.add(
                        CatalogType(
                            id = t.getString("id"),
                            name = t.getString("name"),
                            defaultFlag = t.optString("defaultFlag", "")
                        )
                    )
                }
            }
            val id = o.getString("id")
            if (types.isEmpty()) {
                types.add(CatalogType("$id.main", name))
            }
            items.add(
                CatalogItem(
                    id = id,
                    name = name,
                    category = o.optString("category", "general"),
                    typeOnlyExport = o.optBoolean("typeOnlyExport", false),
                    hidden = o.optBoolean("hidden", false),
                    types = types
                )
            )
        }

        return Catalog(if (flags.isEmpty()) defaultFlags else flags, items)
    }

    fun write(catalog: Catalog): String {
        val root = JSONObject()
        root.put("version", 1)

        val flags = JSONArray()
        catalog.flags.forEach {
            flags.put(JSONObject().put("id", it.id).put("label", it.label))
        }
        root.put("flags", flags)

        val items = JSONArray()
        catalog.items.forEach { item ->
            val o = JSONObject()
            o.put("id", item.id)
            o.put("name", item.name)
            o.put("category", item.category)
            if (item.typeOnlyExport) o.put("typeOnlyExport", true)
            if (item.hidden) o.put("hidden", true)
            val types = JSONArray()
            item.types.forEach { t ->
                val to = JSONObject().put("id", t.id).put("name", t.name)
                if (t.defaultFlag.isNotEmpty()) to.put("defaultFlag", t.defaultFlag)
                types.put(to)
            }
            o.put("types", types)
            items.put(o)
        }
        root.put("items", items)

        return root.toString(2)
    }

    fun fromDb(items: List<ItemEntity>, types: List<TypeEntity>, flags: List<FlagEntity>): Catalog {
        val typesByItem = types.groupBy { it.itemId }
        return Catalog(
            flags = flags.sortedBy { it.sortIndex }.map { CatalogFlag(it.id, it.label) },
            items = items.sortedBy { it.sortIndex }.map { item ->
                CatalogItem(
                    id = item.id,
                    name = item.name,
                    category = item.category,
                    typeOnlyExport = item.typeOnlyExport,
                    hidden = item.hidden,
                    types = (typesByItem[item.id] ?: emptyList())
                        .sortedBy { it.sortIndex }
                        .map { CatalogType(it.id, it.name, it.defaultFlag) }
                )
            }
        )
    }
}

fun slugify(text: String): String {
    val base = text.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
    return if (base.isEmpty()) "item" else base
}

fun uniqueId(base: String, taken: Set<String>): String {
    if (base !in taken) return base
    var n = 2
    while ("$base-$n" in taken) n++
    return "$base-$n"
}
