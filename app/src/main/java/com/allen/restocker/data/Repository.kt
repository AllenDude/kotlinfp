package com.allen.restocker.data

import android.content.Context
import androidx.room.withTransaction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A type row being edited in the item editor. id == null means a brand new type. */
data class EditableType(val id: String?, val name: String, val defaultFlag: String)

class Repository(private val context: Context, private val db: AppDatabase) {

    private val dao = db.dao()

    val items: Flow<List<ItemEntity>> = dao.observeItems()
    val types: Flow<List<TypeEntity>> = dao.observeTypes()
    val flags: Flow<List<FlagEntity>> = dao.observeFlags()
    val history: Flow<List<OrderEntity>> = dao.observeHistory()

    @OptIn(ExperimentalCoroutinesApi::class)
    val draftLines: Flow<List<LineEntity>> =
        dao.observeDraft().flatMapLatest { draft ->
            if (draft == null) flowOf(emptyList()) else dao.observeLines(draft.id)
        }

    // ---------- startup ----------

    suspend fun start() {
        if (dao.countItems() == 0) {
            applyCatalog(loadSeed())
        }
        if (dao.countFlags() == 0) {
            dao.upsertFlags(
                listOf(
                    FlagEntity("buo", "Buo", 0),
                    FlagEntity("chichirya", "Chichirya", 1),
                    FlagEntity("promo", "Promo", 2)
                )
            )
        }
        draftId()
    }

    /** The catalog that ships inside the app. Its order is also the old PWA's order (used by old codes). */
    suspend fun loadSeed(): Catalog = withContext(Dispatchers.IO) {
        val text = context.assets.open("catalog.json").bufferedReader().use { it.readText() }
        CatalogJson.parse(text)
    }

    private suspend fun draftId(): Long {
        dao.getDraft()?.let { return it.id }
        return dao.insertOrder(OrderEntity(createdAt = System.currentTimeMillis()))
    }

    // ---------- catalog ----------

    /** Adds or updates everything in [catalog] (matched by id). Nothing is deleted. */
    suspend fun applyCatalog(catalog: Catalog) = db.withTransaction {
        dao.upsertFlags(catalog.flags.mapIndexed { i, f -> FlagEntity(f.id, f.label, i) })
        catalog.items.forEachIndexed { i, item ->
            dao.upsertItem(
                ItemEntity(
                    id = item.id,
                    name = item.name,
                    category = item.category,
                    sortIndex = i,
                    typeOnlyExport = item.typeOnlyExport,
                    hidden = item.hidden
                )
            )
            dao.deleteTypesOf(item.id)
            dao.upsertTypes(
                item.types.mapIndexed { j, t ->
                    TypeEntity(t.id, item.id, t.name, j, t.defaultFlag)
                }
            )
        }
    }

    suspend fun saveItem(
        existingId: String?,
        originalName: String?,
        name: String,
        category: String,
        typeOnly: Boolean,
        hidden: Boolean,
        editedTypes: List<EditableType>
    ) = db.withTransaction {
        val cleanName = name.trim()
        val allItems = dao.getItems()
        val itemId = existingId ?: uniqueId(slugify(cleanName), allItems.map { it.id }.toSet())
        val sortIndex = allItems.firstOrNull { it.id == itemId }?.sortIndex
            ?: ((allItems.maxOfOrNull { it.sortIndex } ?: -1) + 1)

        dao.upsertItem(
            ItemEntity(
                id = itemId,
                name = cleanName,
                category = category.trim().ifBlank { "general" },
                sortIndex = sortIndex,
                typeOnlyExport = typeOnly,
                hidden = hidden
            )
        )

        val existingTypes = dao.getTypes()
        val taken = existingTypes.map { it.id }.toMutableSet()

        val clean = editedTypes
            .filter { it.name.isNotBlank() }
            .ifEmpty { listOf(EditableType(null, cleanName, "")) }

        val keptIds = clean.mapNotNull { it.id }.toSet()
        existingTypes
            .filter { it.itemId == itemId && it.id !in keptIds }
            .forEach { dao.deleteType(it.id) }

        val rows = clean.mapIndexed { index, t ->
            val id = t.id ?: uniqueId("$itemId.${slugify(t.name)}", taken).also { taken.add(it) }
            // A type that was named after the old item name follows a rename of the item.
            val typeName = if (originalName != null && t.name.trim() == originalName) cleanName else t.name.trim()
            TypeEntity(id, itemId, typeName, index, t.defaultFlag)
        }
        dao.upsertTypes(rows)
    }

    suspend fun deleteItem(id: String) = db.withTransaction {
        dao.deleteDraftLinesOfItem(id)
        dao.deleteTypesOf(id)
        dao.deleteItem(id)
    }

    suspend fun moveItem(id: String, up: Boolean) = db.withTransaction {
        val list = dao.getItems().toMutableList()
        val index = list.indexOfFirst { it.id == id }
        val target = if (up) index - 1 else index + 1
        if (index >= 0 && target in list.indices) {
            val moved = list.removeAt(index)
            list.add(target, moved)
        }
        list.forEachIndexed { i, item -> dao.setItemSort(item.id, i) }
    }

    suspend fun renameFlag(id: String, label: String) = dao.updateFlagLabel(id, label)

    // ---------- draft lines ----------

    suspend fun setQty(itemId: String, typeId: String, qty: String, defaultFlag: String) {
        val orderId = draftId()
        val existing = dao.findLine(orderId, itemId, typeId)
        if (existing == null) {
            if (qty.isNotBlank()) {
                dao.insertLine(
                    LineEntity(
                        orderId = orderId,
                        itemId = itemId,
                        typeId = typeId,
                        qty = qty,
                        flag = defaultFlag
                    )
                )
            }
        } else {
            dao.setLineQty(existing.id, qty)
        }
    }

    suspend fun setFlag(itemId: String, typeId: String, flag: String) {
        val orderId = draftId()
        val existing = dao.findLine(orderId, itemId, typeId)
        if (existing == null) {
            dao.insertLine(LineEntity(orderId = orderId, itemId = itemId, typeId = typeId, flag = flag))
        } else {
            dao.setLineFlag(existing.id, flag)
        }
    }

    suspend fun addCustomLine(itemId: String) {
        dao.insertLine(LineEntity(orderId = draftId(), itemId = itemId, typeId = null))
    }

    suspend fun setLineQty(lineId: Long, qty: String) = dao.setLineQty(lineId, qty)
    suspend fun setLineFlag(lineId: Long, flag: String) = dao.setLineFlag(lineId, flag)
    suspend fun setCustomName(lineId: Long, name: String) = dao.setLineCustomName(lineId, name)
    suspend fun deleteLine(lineId: Long) = dao.deleteLine(lineId)

    suspend fun currentLines(): List<LineEntity> = dao.getLines(draftId())

    /** Replaces the whole draft and returns what was there before (for Undo). */
    suspend fun replaceDraft(lines: List<LineEntity>): List<LineEntity> = db.withTransaction {
        val orderId = draftId()
        val previous = dao.getLines(orderId)
        dao.deleteLinesOf(orderId)
        dao.insertLines(lines.map { it.copy(id = 0, orderId = orderId) })
        previous
    }

    // ---------- history ----------

    suspend fun finishDraft(text: String, lineCount: Int) = db.withTransaction {
        val draft = dao.getDraft()
        if (draft != null) {
            val now = System.currentTimeMillis()
            dao.updateOrder(draft.copy(sentAt = now, text = text, lineCount = lineCount))
            dao.insertOrder(OrderEntity(createdAt = now))
        }
    }

    suspend fun linesOfOrder(orderId: Long): List<LineEntity> = dao.getLines(orderId)

    suspend fun deleteOrder(orderId: Long) = db.withTransaction {
        dao.deleteLinesOf(orderId)
        dao.deleteOrder(orderId)
    }

    suspend fun snapshotCatalog(): Catalog =
        CatalogJson.fromDb(dao.getItems(), dao.getTypes(), dao.getFlags())
}
