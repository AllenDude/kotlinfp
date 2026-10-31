@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.allen.restocker.ui

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.allen.restocker.data.FlagEntity
import kotlinx.coroutines.launch

fun shareText(context: Context, text: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    val chooser = Intent.createChooser(send, "Send order")
    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(chooser)
}

@Composable
fun OrderScreen(vm: MainViewModel, snackbarHost: SnackbarHostState) {
    val ui by vm.ui.collectAsState()
    val query by vm.query.collectAsState()
    val category by vm.category.collectAsState()
    val enteredOnly by vm.enteredOnly.collectAsState()
    val numberKeypad by vm.settings.numberKeypad.collectAsState()

    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var menuOpen by remember { mutableStateOf(false) }
    var showPreview by remember { mutableStateOf(false) }
    var showImport by remember { mutableStateOf(false) }

    val letterIndex = remember(ui.groups) {
        val map = LinkedHashMap<String, Int>()
        var index = 0
        ui.groups.forEach { (letter, list) ->
            map[letter] = index
            index += 1 + list.size
        }
        map
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = ui.header,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(text = { Text("Order history") }, onClick = {
                            menuOpen = false
                            vm.screen = Screen.History
                        })
                        DropdownMenuItem(text = { Text("Manage items") }, onClick = {
                            menuOpen = false
                            vm.screen = Screen.ManageItems
                        })
                        DropdownMenuItem(text = { Text("Settings") }, onClick = {
                            menuOpen = false
                            vm.screen = Screen.Settings
                        })
                        HorizontalDivider()
                        DropdownMenuItem(text = { Text("Copy code") }, onClick = {
                            menuOpen = false
                            vm.exportCode { code ->
                                clipboard.setText(AnnotatedString(code))
                                vm.snacks.tryEmit(SnackEvent("Code copied"))
                            }
                        })
                        DropdownMenuItem(text = { Text("Import code") }, onClick = {
                            menuOpen = false
                            showImport = true
                        })
                        DropdownMenuItem(text = { Text("Clear list") }, onClick = {
                            menuOpen = false
                            vm.clearList()
                        })
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp, shadowElevation = 6.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (ui.lineCount == 1) "1 line" else "${ui.lineCount} lines",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "\u2713 Autosaved",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { vm.saveNow() },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp),
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) { Text("Save", maxLines = 1) }
                        OutlinedButton(
                            onClick = { showPreview = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp),
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) { Text("Preview", maxLines = 1) }
                        Button(
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            contentPadding = PaddingValues(horizontal = 6.dp),
                            onClick = {
                                if (ui.text.isEmpty()) {
                                    vm.snacks.tryEmit(SnackEvent("List is empty"))
                                } else {
                                    shareText(context, ui.text)
                                    vm.saveSent()
                                }
                            }
                        ) { Text("Send", maxLines = 1) }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            Column(Modifier.fillMaxSize()) {

                OutlinedTextField(
                    value = query,
                    onValueChange = { vm.query.value = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    singleLine = true,
                    placeholder = { Text("Search items") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { vm.query.value = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    }
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = category == null,
                        onClick = { vm.category.value = null },
                        label = { Text("All") }
                    )
                    ui.categories.forEach { c ->
                        FilterChip(
                            selected = category == c,
                            onClick = { vm.category.value = if (category == c) null else c },
                            label = { Text(c.replaceFirstChar { it.uppercase() }) }
                        )
                    }
                    FilterChip(
                        selected = enteredOnly,
                        onClick = { vm.enteredOnly.value = !enteredOnly },
                        label = { Text("Entered only") }
                    )
                }

                if (!ui.loaded) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (ui.groups.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No items match", style = MaterialTheme.typography.titleMedium)
                    }
                } else {
                  Box(Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 12.dp, end = 44.dp, top = 4.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ui.groups.forEach { (letter, list) ->
                            stickyHeader(key = "header-$letter") {
                                LetterHeader(letter)
                            }
                            items(list, key = { it.item.id }) { itemUi ->
                                ItemCard(
                                    itemUi = itemUi,
                                    flags = ui.flags,
                                    numberKeypad = numberKeypad,
                                    vm = vm
                                )
                            }
                        }
                    }
                    FastScroller(
                        letters = letterIndex.keys.toList(),
                        onSelect = { letter ->
                            val index = letterIndex[letter] ?: 0
                            scope.launch { listState.scrollToItem(index) }
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 4.dp, top = 4.dp, bottom = 4.dp)
                    )
                  }
                }
            }
        }
    }

    if (showPreview) {
        ModalBottomSheet(onDismissRequest = { showPreview = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Order preview", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

                if (ui.text.isEmpty()) {
                    Text("Nothing entered yet.")
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        SelectionContainer {
                            Text(ui.text, fontSize = 20.sp)
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                        onClick = {
                            if (ui.text.isNotEmpty()) {
                                clipboard.setText(AnnotatedString(ui.text))
                                vm.snacks.tryEmit(SnackEvent("List copied"))
                            }
                        }
                    ) { Text("Copy") }
                    Button(
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        onClick = {
                            if (ui.text.isNotEmpty()) {
                                shareText(context, ui.text)
                                vm.saveSent()
                            }
                        }
                    ) { Text("Send") }
                }
                OutlinedButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface),
                    onClick = {
                        showPreview = false
                        vm.startNewList()
                    }
                ) { Text("Done, start a new list") }
            }
        }
    }

    if (showImport) {
        var code by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showImport = false },
            title = { Text("Paste the code") },
            text = {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 4,
                    maxLines = 8
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showImport = false
                    vm.importCode(code)
                }) { Text("Import") }
            },
            dismissButton = {
                TextButton(onClick = { showImport = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun LetterHeader(letter: String) {
    Text(
        text = letter,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun ItemCard(
    itemUi: ItemUi,
    flags: List<FlagEntity>,
    numberKeypad: Boolean,
    vm: MainViewModel
) {
    val item = itemUi.item
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            itemUi.rows.forEach { row ->
                key(row.key) {
                    VariantRow(
                        itemName = item.name,
                        itemId = item.id,
                        row = row,
                        flags = flags,
                        numberKeypad = numberKeypad,
                        vm = vm
                    )
                }
            }

            TextButton(onClick = { vm.addCustom(item.id) }) {
                Text("+ Additional type")
            }
        }
    }
}

@Composable
private fun VariantRow(
    itemName: String,
    itemId: String,
    row: RowUi,
    flags: List<FlagEntity>,
    numberKeypad: Boolean,
    vm: MainViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (row.isCustom) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = vm.nameOf(row),
                    onValueChange = { vm.setCustomName(row, it) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("Type name") }
                )
                IconButton(onClick = { vm.deleteCustom(row) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Remove type")
                }
            }
        } else if (row.typeName != itemName) {
            Text(
                text = row.typeName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = { vm.step(itemId, row, -1) },
                modifier = Modifier.size(60.dp),
                contentPadding = PaddingValues(0.dp)
            ) { Text("−", fontSize = 32.sp, fontWeight = FontWeight.Bold) }

            OutlinedTextField(
                value = vm.qtyOf(row),
                onValueChange = { vm.setQty(itemId, row, it) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                placeholder = { Text("Qty") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (numberKeypad) KeyboardType.Number else KeyboardType.Text
                )
            )

            FilledTonalButton(
                onClick = { vm.step(itemId, row, 1) },
                modifier = Modifier.size(60.dp),
                contentPadding = PaddingValues(0.dp)
            ) { Text("+", fontSize = 32.sp, fontWeight = FontWeight.Bold) }
        }

        FlagSelector(
            flags = flags,
            selected = row.flag,
            onSelect = { vm.setFlag(itemId, row, it) }
        )
    }
}

@Composable
private fun FlagSelector(flags: List<FlagEntity>, selected: String, onSelect: (String) -> Unit) {
    val options = listOf("" to "None") + flags.map { it.id to it.label }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                pair.forEach { (id, label) ->
                    if (selected == id) {
                        Button(
                            onClick = { onSelect(id) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text("\u2713 $label", maxLines = 1, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        OutlinedButton(
                            onClick = { onSelect(id) },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface
                            ),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Text(label, maxLines = 1)
                        }
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** A-Z strip on the right edge. Tap a letter or drag along it. Sized to always fit its space. */
@Composable
private fun FastScroller(
    letters: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var heightPx by remember { mutableIntStateOf(1) }

    fun pick(y: Float) {
        if (letters.isEmpty()) return
        val index = ((y / heightPx) * letters.size).toInt().coerceIn(0, letters.size - 1)
        onSelect(letters[index])
    }

    BoxWithConstraints(modifier = modifier.fillMaxHeight()) {
        val perLetter = maxHeight / letters.size
        val letterDp = minOf(17.dp, perLetter * 0.8f)
        val letterSp = with(LocalDensity.current) { letterDp.toSp() }

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(34.dp)
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                .onSizeChanged { heightPx = it.height.coerceAtLeast(1) }
                .pointerInput(letters) {
                    detectTapGestures { offset -> pick(offset.y) }
                }
                .pointerInput(letters) {
                    detectVerticalDragGestures(
                        onDragStart = { offset -> pick(offset.y) },
                        onVerticalDrag = { change, _ -> pick(change.position.y) }
                    )
                },
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround
        ) {
            letters.forEach { letter ->
                Text(
                    text = letter,
                    fontSize = letterSp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
