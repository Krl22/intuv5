package com.intu.taxi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue

data class Message(
    val id: String,
    val senderId: String,
    val text: String,
    val timestamp: Long
)

@Composable
fun RideChatSheet(
    rideId: String,
    meUid: String,
    onClose: () -> Unit
) {
    val messages = remember { mutableStateListOf<Message>() }
    var input by remember { mutableStateOf("") }

    DisposableEffect(rideId) {
        val ref = FirebaseDatabase.getInstance().reference
            .child("currentRides")
            .child(rideId)
            .child("chat")
            .child("messages")
        val query = ref.orderByChild("timestamp").limitToLast(100)
        val listener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val id = snapshot.key ?: return
                val sender = snapshot.child("senderId").getValue(String::class.java) ?: return
                val text = snapshot.child("text").getValue(String::class.java) ?: ""
                val ts = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                messages.add(Message(id = id, senderId = sender, text = text, timestamp = ts))
                messages.sortBy { it.timestamp }
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        }
        query.addChildEventListener(listener)
        onDispose { ref.removeEventListener(listener) }
    }

    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(text = "Chat del viaje", style = MaterialTheme.typography.titleMedium) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                LazyColumn(modifier = Modifier.fillMaxWidth().height(280.dp)) {
                    items(messages) { msg ->
                        MessageBubble(meUid = meUid, msg = msg)
                        Spacer(Modifier.height(6.dp))
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.take(500) },
                    singleLine = true,
                    label = { Text("Escribe un mensaje") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val txt = input.trim()
                    if (txt.isNotEmpty()) {
                        val ref = FirebaseDatabase.getInstance().reference
                            .child("currentRides")
                            .child(rideId)
                            .child("chat")
                            .child("messages")
                            .push()
                        val data = mapOf(
                            "senderId" to meUid,
                            "text" to txt,
                            "timestamp" to ServerValue.TIMESTAMP
                        )
                        ref.setValue(data)
                        input = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488), contentColor = Color.White)
            ) { Text("Enviar", fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE5E7EB), contentColor = Color(0xFF111827))) {
                Text("Cerrar")
            }
        }
    )
}

@Composable
fun MessageBubble(meUid: String, msg: Message) {
    val isMe = msg.senderId == meUid
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .background(
                    color = if (isMe) Color(0xFF0D9488) else Color(0xFFF3F4F6),
                    shape = RoundedCornerShape(12.dp)
                )
                .border(
                    width = 1.dp,
                    color = if (isMe) Color(0xFF0D9488) else Color(0xFFE5E7EB),
                    shape = RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = msg.text,
                style = MaterialTheme.typography.bodySmall,
                color = if (isMe) Color.White else Color(0xFF111827)
            )
        }
    }
}

