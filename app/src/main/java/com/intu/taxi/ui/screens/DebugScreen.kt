package com.intu.taxi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.intu.taxi.BuildConfig
import com.intu.taxi.R
import com.intu.taxi.ui.debug.DebugLog
import com.google.firebase.FirebaseApp
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.ktx.Firebase
import com.google.firebase.functions.ktx.functions
 

@Composable
fun DebugScreen() {
    val ctx = LocalContext.current
    val filterRatingOnly = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    val allMsgs = DebugLog.messages
    val ratingMsgs = allMsgs.filter { it.contains("rating", ignoreCase = true) || it.contains("RATING-", ignoreCase = true) }
    val shown = if (filterRatingOnly.value) ratingMsgs else allMsgs
    val bg = Brush.verticalGradient(listOf(Color(0xFF08817E).copy(alpha = 0.06f), Color(0xFF1E1F47).copy(alpha = 0.04f), MaterialTheme.colorScheme.surface))
    Box(Modifier.fillMaxSize().background(bg)) {
        LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)), shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(48.dp).background(Brush.radialGradient(listOf(Color(0xFF08817E), Color(0xFF1E1F47))), RoundedCornerShape(24.dp)), contentAlignment = Alignment.Center) {
                                Icon(Icons.Filled.BugReport, contentDescription = null, tint = Color.White)
                            }
                            Spacer(Modifier.size(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(androidx.compose.ui.res.stringResource(R.string.debug_title, shown.size), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
                                Text(androidx.compose.ui.res.stringResource(R.string.app_version_prefix) + BuildConfig.APP_VERSION_TAG, style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B7280))
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { DebugLog.messages.clear() }) { Text(androidx.compose.ui.res.stringResource(R.string.clear_button)) }
                                OutlinedButton(onClick = { filterRatingOnly.value = !filterRatingOnly.value }) { Text(if (filterRatingOnly.value) androidx.compose.ui.res.stringResource(R.string.show_all) else androidx.compose.ui.res.stringResource(R.string.filter_rating)) }
                            }
                            Row(horizontalArrangement = Arrangement.Start) {
                                Button(onClick = {
                                    val host = ctx.getString(R.string.functions_emulator_host)
                                    val port = ctx.getString(R.string.functions_emulator_port)
                                    val projectId = FirebaseApp.getInstance().options.projectId ?: ""
                                    val target = host + ":" + port
                                    DebugLog.log("Ping -> " + target + " · project=" + projectId)
                                    Firebase.functions(ctx.getString(R.string.functions_region))
                                        .getHttpsCallable("ping")
                                        .call(mapOf("name" to "Intu"))
                                        .addOnSuccessListener { result -> DebugLog.log("Respuesta: " + result.data.toString()) }
                                        .addOnFailureListener { e ->
                                            val base = "Error: " + (e.message ?: "")
                                            if (e is FirebaseFunctionsException) DebugLog.log(base + ", code=" + e.code.name + ", details=" + (e.details?.toString() ?: "") + ", cause=" + (e.cause?.message ?: "")) else DebugLog.log(base + ", type=" + e::class.java.simpleName + ", stack=" + e.stackTraceToString())
                                        }
                                }) { Text("Probar Functions") }
                            }
                        }
                    }
                }
            }
            items(shown) { msg ->
                DebugMessageRow(msg)
            }
        }
    }
}

private fun parseTimestamp(msg: String): Long? {
    val start = msg.indexOf('[')
    val end = msg.indexOf(']')
    if (start == 0 && end > start) {
        val raw = msg.substring(start + 1, end)
        return raw.toLongOrNull()
    }
    return null
}

private fun formatTime(ms: Long): String {
    val d = java.util.Date(ms)
    return java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(d)
}

@Composable
private fun DebugMessageRow(msg: String) {
    val ts = parseTimestamp(msg)
    val timeStr = ts?.let { formatTime(it) } ?: ""
    Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.98f)), shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).background(Color(0xFFE5E7EB), RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                Text(timeStr, style = MaterialTheme.typography.labelSmall, color = Color(0xFF374151))
            }
            Spacer(Modifier.size(10.dp))
            Text(msg, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace), color = Color(0xFF111827))
        }
    }
}
