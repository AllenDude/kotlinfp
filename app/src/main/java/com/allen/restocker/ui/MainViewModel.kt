package com.allen.restocker.ui

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.allen.restocker.data.AppDatabase
import com.allen.restocker.data.CatalogJson
import com.allen.restocker.data.EditableType
import com.allen.restocker.data.FlagEntity
import com.allen.restocker.data.ItemEntity
import com.allen.restocker.data.LineEntity
import com.allen.restocker.data.OrderEntity
import com.allen.restocker.data.Repository
import com.allen.restocker.data.SettingsStore
import com.allen.restocker.data.TypeEntity
import com.allen.restocker.domain.Codes
import com.allen.restocker.domain.OrderText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface Screen {
    data object Order : Screen
    data object History : Screen
    data object ManageItems : Screen
    data class EditItem(val id: String?) : Screen
    data object Settings : Screen
}

data class SnackEvent(
    val message: String,
    val actionLabel: String? = null,
    val action: (() -> Unit)? = null
)

/** One type row on screen. [key] is stable for the lifetime of the row. */
data class RowUi(
    val key: String,
    val lineId: Long?,
    val typeId: String?,
    val typeName: String,
    val isCustom: Boolean,
    val qty: String,
    val flag: String,
    val defaultFlag: String
)

data class ItemUi(val item: ItemEntity, val rows: List<RowUi>)

data class OrderUi(
    val groups: List<Pair<String, List<ItemUi>>> = emptyList(),
    val categories: List<String> = emptyList(),
    val flags: List<FlagEntity> = emptyList(),
    val lineCount: Int = 0,
    val text: String = "",
    val header: String = "",
    val loaded: Boolean = false
)

data class CatalogState(
    val items: List<ItemEntity>,
    val types: List<TypeEntity>,
    val flags: List<FlagEntity>,
    val header: String
)

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = Repository(app, AppDatabase.get(app))
    val settings = SettingsStore(app)

    var screen: Screen by mutableStateOf(Screen.Order)

    val query = MutableStateFlow("")
    val category = MutableStateFlow<String?>(null)
    val enteredOnly = MutableStateFlow(false)

    /**
     * Text being typed, shown immediately. The database is updated behind it, so the
     * cursor never jumps while typing. Cleared whenever the whole list is replaced.
     */
    private val edits = mutableStateMapOf<String, String>()

    val snacks = MutableSharedFlow<SnackEvent>(extraBufferCapacity = 16)

    // All database writes run one after another, in the order they were made.
    private val writes = Channel<suspend () -> Unit>(Channel.UNLIMITED)

    val catalog: StateFlow<CatalogState> =
        combine(repo.items, repo.types, repo.flags, settings.header) { items, types, flags, header ->
            CatalogState(items, types, flags, header)
        }.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            CatalogState(emptyList(), emptyList(), emptyList(), settings.header.value)
        )

    val ui: StateFlow<OrderUi> =
        combine(catalog, repo.draftLines, query, category, enteredOnly) { cat, lines, q, c, entered ->
            buildUi(cat, lines, q, c, entered)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, OrderUi())

    val history: StateFlow<List<OrderEntity>> =
        repo.history.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            for (job in writes) {
                try {
                    job()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    snack("Something went wrong: ${e.message ?: "unknown error"}")
                }
            }
        }
        write { repo.start() }
    }

    private fun write(job: suspend () -> Unit) {
        writes.trySend(job)
    }

    private fun snack(message: String, actionLabel: String? = null, action: (() -> Unit)? = null) {
        snacks.tryEmit(SnackEvent(message, actionLabel, action))
    }

    // ---------- navigation ----------

    fun back() {
        screen = when (screen) {
            is Screen.EditItem -> Screen.ManageItems
            else -> Screen.Order
        }
    }

    // ---------- building the screen ----------

    private fun letterOf(name: String): String {
        val c = name.trim().firstOrNull()
        return if (c != null && c.isLetter()) c.uppercaseChar().toString() else "#"
    }

    private fun buildUi(
        cat: CatalogState,
        lines: List<LineEntity>,
        q: String,
        categoryFilter: String?,
        enteredOnly: Boolean
    ): OrderUi {
        val typesByItem = cat.types.groupBy { it.itemId }

        val catalogLines = HashMap<String, LineEntity>()
        val customByItem = HashMap<String, MutableList<LineEntity>>()
        for (l in lines) {
            val typeId = l.typeId
            if (typeId != null) {
                catalogLines["${l.itemId}|$typeId"] = l
            } else {
                customByItem.getOrPut(l.itemId) { mutableListOf() }.add(l)
            }
        }

        val needle = q.trim().lowercase()
        val itemUis = ArrayList<ItemUi>()

        for (item in cat.items) {
            if (item.hidden) continue
            if (categoryFilter != null && item.category != categoryFilter) continue

            val rows = ArrayList<RowUi>()
            for (t in typesByItem[item.id] ?: emptyList()) {
                val key = "${item.id}|${t.id}"
                val l = catalogLines[key]
                rows.add(
                    RowUi(
                        key = key,
                        lineId = l?.id,
                        typeId = t.id,
                        typeName = t.name,
                        isCustom = false,
                        qty = l?.qty ?: "",
                        flag = l?.flag ?: t.defaultFlag,
                        defaultFlag = t.defaultFlag
                    )
                )
            }
            for (l in customByItem[item.id] ?: emptyList()) {
                rows.add(
                    RowUi(
                        key = "c${l.id}",
                        lineId = l.id,
                        typeId = null,
                        typeName = l.customName,
                        isCustom = true,
                        qty = l.qty,
                        flag = l.flag,
                        defaultFlag = ""
                    )
                )
            }

            if (needle.isNotEmpty()) {
                val hit = item.name.lowercase().contains(needle) ||
                    rows.any { it.typeName.lowercase().contains(needle) }
                if (!hit) continue
            }
            if (enteredOnly && rows.none { it.qty.isNotBlank() }) continue

            itemUis.add(ItemUi(item, rows))
        }

        val sorted = itemUis.sortedBy { it.item.name.lowercase() }
        val groups = sorted.groupBy { letterOf(it.item.name) }.toList()

        val built = OrderText.build(cat.header, cat.items, cat.types, cat.flags, lines)

        return OrderUi(
            groups = groups,
            categories = cat.items.map { it.category }.distinct().sorted(),
            flags = cat.flags,
            lineCount = built.lineCount,
            text = built.text,
            header = cat.header,
            loaded = cat.items.isNotEmpty()
        )
    }

    // ---------- row editing ----------

    fun qtyOf(row: RowUi): String = edits["q:${row.key}"] ?: row.qty

    fun nameOf(row: RowUi): String = edits["n:${row.key}"] ?: row.typeName

    fun setQty(itemId: String, row: RowUi, qty: String) {
        edits["q:${row.key}"] = qty
        write {
            if (row.isCustom) {
                repo.setLineQty(row.lineId ?: return@write, qty)
            } else {
                repo.setQty(itemId, row.typeId ?: return@write, qty, row.defaultFlag)
            }
        }
    }

    /** + / - buttons. Keeps any text after the number, so "2 box" becomes "3 box". */
    fun step(itemId: String, row: RowUi, delta: Int) {
        val current = qtyOf(row).trim()
        if (current.isEmpty()) {
            if (delta > 0) setQty(itemId, row, "1")
            return
        }
        val m = Regex("^(\\d+)(.*)$").find(current) ?: return
        val number = m.groupValues[1].toLongOrNull() ?: return
        val next = number + delta
        setQty(itemId, row, if (next <= 0) "" else "$next${m.groupValues[2]}")
    }

    fun setFlag(itemId: String, row: RowUi, flag: String) {
        write {
            if (row.isCustom) {
                repo.setLineFlag(row.lineId ?: return@write, flag)
            } else {
                repo.setFlag(itemId, row.typeId ?: return@write, flag)
            }
        }
    }

    fun addCustom(itemId: String) {
        write { repo.addCustomLine(itemId) }
    }

    fun setCustomName(row: RowUi, name: String) {
        edits["n:${row.key}"] = name
        write { repo.setCustomName(row.lineId ?: return@write, name) }
    }

    fun deleteCustom(row: RowUi) {
        write { repo.deleteLine(row.lineId ?: return@write) }
    }

    // ---------- whole-list actions (all undoable) ----------

    private fun undo(previous: List<LineEntity>) {
        write {
            repo.replaceDraft(previous)
            edits.clear()
        }
    }

    fun clearList() {
        write {
            val previous = repo.replaceDraft(emptyList())
            edits.clear()
            snack("List cleared", "Undo") { undo(previous) }
        }
    }

    fun importCode(code: String) {
        write {
            try {
                val cs = catalog.value
                val seed = repo.loadSeed().items
                val decoded = Codes.decode(code, cs.items, cs.types, cs.flags, seed)
                val previous = repo.replaceDraft(decoded.lines)
                edits.clear()
                val message = if (decoded.skipped > 0) "Imported, ${decoded.skipped} skipped" else "Imported"
                snack(message, "Undo") { undo(previous) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                snack("That code didn't work")
            }
        }
    }

    fun exportCode(onCode: (String) -> Unit) {
        write {
            val code = Codes.encode(repo.currentLines())
            if (code.isEmpty()) snack("Nothing to export yet") else onCode(code)
        }
    }

    /** Called right after the list is sent: keeps a copy in history (no duplicates of the same text). */
    fun saveSent() {
        write {
            val cs = catalog.value
            val lines = repo.currentLines()
            val built = OrderText.build(cs.header, cs.items, cs.types, cs.flags, lines)
            if (built.lineCount > 0 && repo.saveToHistory(built.text, built.lineCount, lines)) {
                snack("Saved to history")
            }
        }
    }

    /** "Done": makes sure the list is in history, then empties it for the next order. */
    fun startNewList() {
        write {
            val cs = catalog.value
            val lines = repo.currentLines()
            val built = OrderText.build(cs.header, cs.items, cs.types, cs.flags, lines)
            if (built.lineCount == 0) {
                snack("The list is already empty")
            } else {
                repo.saveToHistory(built.text, built.lineCount, lines)
                val previous = repo.replaceDraft(emptyList())
                edits.clear()
                snack("Saved to history. New list started", "Undo") { undo(previous) }
            }
        }
    }

    fun reuse(order: OrderEntity) {
        write {
            val cs = catalog.value
            val itemIds = cs.items.map { it.id }.toSet()
            val typesById = cs.types.associateBy { it.id }
            val source = repo.linesOfOrder(order.id).filter { l ->
                val typeOk = l.typeId.let { id -> id == null || typesById[id]?.itemId == l.itemId }
                l.qty.isNotBlank() && l.itemId in itemIds && typeOk
            }
            val previous = repo.replaceDraft(source)
            edits.clear()
            screen = Screen.Order
            snack("Started from this order", "Undo") { undo(previous) }
        }
    }

    fun deleteOrder(order: OrderEntity) {
        write { repo.deleteOrder(order.id) }
    }

    // ---------- catalog management ----------

    fun saveItem(
        existingId: String?,
        originalName: String?,
        name: String,
        category: String,
        typeOnly: Boolean,
        hidden: Boolean,
        types: List<EditableType>
    ) {
        write { repo.saveItem(existingId, originalName, name, category, typeOnly, hidden, types) }
    }

    fun deleteItem(id: String) {
        write { repo.deleteItem(id) }
    }

    fun moveItem(id: String, up: Boolean) {
        write { repo.moveItem(id, up) }
    }

    fun renameFlag(id: String, label: String) {
        write { repo.renameFlag(id, label) }
    }

    fun exportCatalog(uri: Uri) {
        write {
            try {
                val json = CatalogJson.write(repo.snapshotCatalog())
                withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver
                        .openOutputStream(uri, "wt")
                        ?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                }
                snack("Catalog saved")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                snack("Couldn't save the file")
            }
        }
    }

    fun importCatalog(uri: Uri) {
        write {
            try {
                val text = withContext(Dispatchers.IO) {
                    getApplication<Application>().contentResolver
                        .openInputStream(uri)
                        ?.use { it.readBytes().toString(Charsets.UTF_8) }
                        ?: ""
                }
                val parsed = CatalogJson.parse(text)
                repo.applyCatalog(parsed)
                snack("Catalog imported (${parsed.items.size} items)")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                snack("That file isn't a valid catalog")
            }
        }
    }
}
