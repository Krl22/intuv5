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
import com.google.firebase.ktx.Firebase
import com.google.firebase.functions.ktx.functions
import com.google.firebase.functions.FirebaseFunctionsException
import com.google.firebase.FirebaseApp
import androidx.compose.ui.platform.LocalContext
import com.intu.taxi.R
import com.intu.taxi.BuildConfig
 

@Composable
fun DebugScreen() {
    val ctx = LocalContext.current
    LazyColumn(Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Mensajes de debug (" + DebugLog.messages.size + ")", style = MaterialTheme.typography.titleMedium)
                Button(onClick = { DebugLog.messages.clear() }) { Text("Limpiar") }
                Button(onClick = {
                    val host = ctx.getString(R.string.functions_emulator_host)
                    val port = ctx.getString(R.string.functions_emulator_port)
                    val projectId = FirebaseApp.getInstance().options.projectId ?: ""
                    val target = "info-only: " + host + ":" + port
                    DebugLog.log("Llamando función ping... host=" + target + ", project=" + projectId)
                    Firebase.functions(ctx.getString(R.string.functions_region))
                        .getHttpsCallable("ping")
                        .call(mapOf("name" to "Intu"))
                        .addOnSuccessListener { result ->
                            DebugLog.log("Respuesta: " + result.data.toString())
                        }
                        .addOnFailureListener { e ->
                            val base = "Error: " + (e.message ?: "")
                            if (e is FirebaseFunctionsException) {
                                DebugLog.log(base + ", code=" + e.code.name + ", details=" + (e.details?.toString() ?: "") + ", cause=" + (e.cause?.message ?: ""))
                            } else {
                                DebugLog.log(base + ", type=" + e::class.java.simpleName + ", stack=" + e.stackTraceToString())
                            }
                        }
                }) { Text("Probar Functions") }
            }
        }
        items(DebugLog.messages) { msg ->
            Text(text = msg, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
