package com.allen.restocker.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Small settings store (SharedPreferences). */
class SettingsStore(context: Context) {

    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _header = MutableStateFlow(prefs.getString("header", DEFAULT_HEADER) ?: DEFAULT_HEADER)
    val header: StateFlow<String> = _header

    private val _numberKeypad = MutableStateFlow(prefs.getBoolean("numberKeypad", true))
    val numberKeypad: StateFlow<Boolean> = _numberKeypad

    fun setHeader(value: String) {
        _header.value = value
        prefs.edit().putString("header", value).apply()
    }

    fun setNumberKeypad(value: Boolean) {
        _numberKeypad.value = value
        prefs.edit().putBoolean("numberKeypad", value).apply()
    }

    companion object {
        const val DEFAULT_HEADER = "ORENCIO MENDOZA"
    }
}
