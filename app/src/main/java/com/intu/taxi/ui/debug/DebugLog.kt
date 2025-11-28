package com.intu.taxi.ui.debug

import androidx.compose.runtime.mutableStateListOf

object DebugLog {
    val messages = mutableStateListOf<String>()

    fun log(message: String) {
        messages.add("[" + System.currentTimeMillis() + "] " + message)
    }
}

