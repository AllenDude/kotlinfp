package com.allen.restocker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.allen.restocker.ui.EditItemScreen
import com.allen.restocker.ui.HistoryScreen
import com.allen.restocker.ui.MainViewModel
import com.allen.restocker.ui.ManageItemsScreen
import com.allen.restocker.ui.OrderScreen
import com.allen.restocker.ui.RestockerTheme
import com.allen.restocker.ui.Screen
import com.allen.restocker.ui.SettingsScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val vm: MainViewModel = viewModel()
            val textScale by vm.settings.textScale.collectAsState()
            RestockerTheme(textScale = textScale) {
                App(vm)
            }
        }
    }
}

@Composable
fun App(vm: MainViewModel = viewModel()) {
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        vm.snacks.collect { event ->
            launch {
                val result = snackbarHost.showSnackbar(
                    message = event.message,
                    actionLabel = event.actionLabel
                )
                if (result == SnackbarResult.ActionPerformed) {
                    event.action?.invoke()
                }
            }
        }
    }

    BackHandler(enabled = vm.screen != Screen.Order) {
        vm.back()
    }

    when (val screen = vm.screen) {
        Screen.Order -> OrderScreen(vm, snackbarHost)
        Screen.History -> HistoryScreen(vm, snackbarHost)
        Screen.ManageItems -> ManageItemsScreen(vm, snackbarHost)
        is Screen.EditItem -> EditItemScreen(vm, screen.id, snackbarHost)
        Screen.Settings -> SettingsScreen(vm, snackbarHost)
    }
}
