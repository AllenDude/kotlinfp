@file:OptIn(ExperimentalMaterial3Api::class)

package com.allen.restocker.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(vm: MainViewModel, snackbarHost: SnackbarHostState) {
    val cat by vm.catalog.collectAsState()
    val numberKeypad by vm.settings.numberKeypad.collectAsState()
    val textScale by vm.settings.textScale.collectAsState()
    var header by remember { mutableStateOf(vm.settings.header.value) }
    val context = LocalContext.current
    val versionName = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
        } catch (e: Exception) {
            "?"
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> if (uri != null) vm.exportCatalog(uri) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) vm.importCatalog(uri) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { vm.back() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Text size", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1.0f to "Normal", 1.2f to "Large", 1.4f to "Extra large").forEach { (scale, label) ->
                    FilterChip(
                        selected = textScale == scale,
                        onClick = { vm.settings.setTextScale(scale) },
                        label = { Text(label) }
                    )
                }
            }
            Text(
                "This is on top of your phone's own font size setting.",
                style = MaterialTheme.typography.bodySmall
            )

            HorizontalDivider()

            Text("List title", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = header,
                onValueChange = {
                    header = it
                    vm.settings.setHeader(it)
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                supportingText = { Text("Shown at the top of the app and on the first line of every sent list.") }
            )

            HorizontalDivider()

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Number keypad for Qty", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Turn off to type text like \"2 box\". The + and - buttons keep any text after the number.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(checked = numberKeypad, onCheckedChange = { vm.settings.setNumberKeypad(it) })
            }

            HorizontalDivider()

            Text("Section names", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "These are the tags on each row. Their names become the headings in the sent list.",
                style = MaterialTheme.typography.bodySmall
            )
            cat.flags.forEach { flag ->
                var label by remember(flag.id) { mutableStateOf(flag.label) }
                OutlinedTextField(
                    value = label,
                    onValueChange = {
                        label = it
                        if (it.isNotBlank()) vm.renameFlag(flag.id, it.trim())
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            HorizontalDivider()

            Text("Catalog backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Save your items, types, tags and order to a JSON file, or load one. " +
                    "Importing adds new items and updates matching ones; it never deletes.",
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedButton(
                onClick = { exportLauncher.launch("restocker-catalog.json") },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Export catalog (JSON)") }
            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("*/*")) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Import catalog (JSON)") }

            HorizontalDivider()

            Text(
                text = "Version $versionName",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
