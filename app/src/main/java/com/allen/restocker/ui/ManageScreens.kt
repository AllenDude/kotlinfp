@file:OptIn(ExperimentalMaterial3Api::class)

package com.allen.restocker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.allen.restocker.data.EditableType

@Composable
fun ManageItemsScreen(vm: MainViewModel, snackbarHost: SnackbarHostState) {
    val cat by vm.catalog.collectAsState()
    var search by remember { mutableStateOf("") }
    val typesByItem = remember(cat.types) { cat.types.groupBy { it.itemId } }

    val shown = if (search.isBlank()) {
        cat.items
    } else {
        cat.items.filter { it.name.contains(search.trim(), ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage items") },
                navigationIcon = {
                    IconButton(onClick = { vm.back() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { vm.screen = Screen.EditItem(null) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add item")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                singleLine = true,
                placeholder = { Text("Search items") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (search.isNotEmpty()) {
                        IconButton(onClick = { search = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear search")
                        }
                    }
                }
            )
            Text(
                text = "Top to bottom is the order lines appear in the sent list. " +
                    "Use the arrows to move an item (arrows are off while searching).",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
            )

            LazyColumn(
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(shown, key = { it.id }) { item ->
                    Card(
                        Modifier
                            .fillMaxWidth()
                            .clickable { vm.screen = Screen.EditItem(item.id) }
                    ) {
                        Row(
                            modifier = Modifier.padding(start = 14.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                val count = typesByItem[item.id]?.size ?: 0
                                Text(
                                    text = item.category + " · " + count + (if (count == 1) " type" else " types") +
                                        (if (item.hidden) " · hidden" else ""),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            IconButton(
                                enabled = search.isBlank(),
                                onClick = { vm.moveItem(item.id, true) }
                            ) { Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up") }
                            IconButton(
                                enabled = search.isBlank(),
                                onClick = { vm.moveItem(item.id, false) }
                            ) { Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EditItemScreen(vm: MainViewModel, itemId: String?, snackbarHost: SnackbarHostState) {
    val cat by vm.catalog.collectAsState()
    val existing = cat.items.firstOrNull { it.id == itemId }

    var name by remember(itemId) { mutableStateOf(existing?.name ?: "") }
    var category by remember(itemId) { mutableStateOf(existing?.category ?: "general") }
    var typeOnly by remember(itemId) { mutableStateOf(existing?.typeOnlyExport ?: false) }
    var hidden by remember(itemId) { mutableStateOf(existing?.hidden ?: false) }
    val types = remember(itemId) {
        mutableStateListOf<EditableType>().apply {
            cat.types
                .filter { it.itemId == itemId }
                .sortedBy { it.sortIndex }
                .forEach { add(EditableType(it.id, it.name, it.defaultFlag)) }
        }
    }
    var confirmDelete by remember { mutableStateOf(false) }

    val categories = remember(cat.items) { cat.items.map { it.category }.distinct().sorted() }
    val flagOptions = listOf("" to "None") + cat.flags.map { it.id to it.label }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (existing == null) "New item" else "Edit item") },
                navigationIcon = {
                    IconButton(onClick = { vm.back() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = {
                        if (name.isBlank()) {
                            vm.snacks.tryEmit(SnackEvent("Give the item a name first"))
                        } else {
                            vm.saveItem(
                                existingId = existing?.id,
                                originalName = existing?.name,
                                name = name,
                                category = category,
                                typeOnly = typeOnly,
                                hidden = hidden,
                                types = types.toList()
                            )
                            vm.back()
                        }
                    }) { Text("Save") }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Item name") }
            )

            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Category") }
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { c ->
                    FilterChip(
                        selected = category == c,
                        onClick = { category = c },
                        label = { Text(c) }
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Send with type name only", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Lines read \"2pcs. <type>\" without the item name (like School Supplies).",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(checked = typeOnly, onCheckedChange = { typeOnly = it })
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Hide from the order list", fontWeight = FontWeight.SemiBold)
                }
                Switch(checked = hidden, onCheckedChange = { hidden = it })
            }

            HorizontalDivider()
            Text("Types", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "A type named exactly like the item is sent as just the item name.",
                style = MaterialTheme.typography.bodySmall
            )

            types.forEachIndexed { index, type ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = type.name,
                                onValueChange = { types[index] = type.copy(name = it) },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                label = { Text("Type name") }
                            )
                            IconButton(onClick = { types.removeAt(index) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Remove type")
                            }
                        }
                        Text("Auto tag when a quantity is entered", style = MaterialTheme.typography.bodySmall)
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            flagOptions.forEach { (id, label) ->
                                FilterChip(
                                    selected = type.defaultFlag == id,
                                    onClick = { types[index] = type.copy(defaultFlag = id) },
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }
            }

            TextButton(onClick = { types.add(EditableType(null, "", "")) }) {
                Text("+ Add type")
            }

            if (existing != null) {
                HorizontalDivider()
                OutlinedButton(
                    onClick = { confirmDelete = true },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Delete item", color = MaterialTheme.colorScheme.error) }
            }
        }
    }

    if (confirmDelete && existing != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${existing.name}?") },
            text = { Text("It is removed from the catalog and from the current list. Saved history keeps its text.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.deleteItem(existing.id)
                    vm.back()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel") }
            }
        )
    }
}
