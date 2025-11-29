package com.intu.taxi.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.intu.taxi.ui.debug.DebugLog

@Composable
fun DriverTopUpScreen(onFinished: () -> Unit) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val amount = remember { mutableStateOf("") }
    val method = remember { mutableStateOf("yape") }
    val screenshotUri = remember { mutableStateOf<Uri?>(null) }
    val status = remember { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> screenshotUri.value = uri }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Recargar saldo", style = MaterialTheme.typography.titleLarge)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(4.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = amount.value, onValueChange = { amount.value = it.filter { ch -> ch.isDigit() || ch == '.' } }, label = { Text("Monto (S/)") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { method.value = "yape" }, enabled = method.value != "yape") { Text("Yape") }
                    OutlinedButton(onClick = { method.value = "plin" }, enabled = method.value != "plin") { Text("Plin") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { picker.launch("image/*") }) { Text("Subir captura") }
                    if (screenshotUri.value != null) {
                        AsyncImage(model = screenshotUri.value, contentDescription = null, modifier = Modifier.size(72.dp))
                    }
                }
                Button(onClick = {
                    val u = uid
                    if (u == null) { status.value = "Usuario no autenticado"; DebugLog.log("topup uid null"); return@Button }
                    val amt = amount.value.toDoubleOrNull()
                    if (amt == null) { status.value = "Monto inválido"; DebugLog.log("topup amount invalid='${amount.value}'"); return@Button }
                    val uri = screenshotUri.value
                    if (uri == null) { status.value = "Sube una captura"; DebugLog.log("topup screenshot missing"); return@Button }
                    DebugLog.log("topup start uid=$u amount=$amt method=${method.value}")
                    val ts = System.currentTimeMillis()
                    val ref = FirebaseStorage.getInstance().reference.child("users/$u/topups/$ts.jpg")
                    ref.putFile(uri).addOnSuccessListener {
                        DebugLog.log("topup upload success ts=$ts")
                        ref.downloadUrl.addOnSuccessListener { url ->
                            DebugLog.log("topup downloadUrl=$url")
                            val data = mapOf(
                                "userId" to u,
                                "amount" to amt,
                                "method" to method.value,
                                "screenshotUrl" to url.toString(),
                                "status" to "pending",
                                "createdAt" to FieldValue.serverTimestamp()
                            )
                            FirebaseFirestore.getInstance().collection("topups").add(data)
                                .addOnSuccessListener { docRef ->
                                    DebugLog.log("topup add success id=${docRef.id}")
                                    status.value = "Solicitud enviada"
                                    onFinished()
                                }
                                .addOnFailureListener { e ->
                                    DebugLog.log("topup add failed: ${e.message}")
                                    status.value = "Error al guardar"
                                }
                        }
                    }.addOnFailureListener { e -> DebugLog.log("topup upload failed: ${e.message}"); status.value = "Error al subir captura" }
                }) { Text("Enviar solicitud") }
                if (status.value.isNotEmpty()) { Text(status.value) }
            }
        }
    }
}
