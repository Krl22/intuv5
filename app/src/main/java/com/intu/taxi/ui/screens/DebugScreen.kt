package com.intu.taxi.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.intu.taxi.ui.debug.DebugLog

@Composable
fun DebugScreen() {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Mensajes de debug (" + DebugLog.messages.size + ")", style = MaterialTheme.typography.titleMedium)
                Button(onClick = { DebugLog.messages.clear() }) { Text("Limpiar") }
            }
        }
        items(DebugLog.messages) { msg ->
            Text(text = msg, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
