package com.intu.taxi.ui.screens

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.util.concurrent.TimeUnit

@Composable
fun VerifyPhoneScreen(phone: String, onFinished: () -> Unit) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val verificationId = remember { mutableStateOf<String?>(null) }
    val code = remember { mutableStateOf("") }
    val status = remember { mutableStateOf("") }

    LaunchedEffect(phone) {
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                auth.currentUser?.linkWithCredential(credential)?.addOnSuccessListener {
                    status.value = "Número vinculado"
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        FirebaseFirestore.getInstance().collection("users").document(uid)
                            .set(mapOf("fullNumber" to phone), SetOptions.merge())
                    }
                    onFinished()
                }
            }
            override fun onVerificationFailed(e: FirebaseException) { status.value = "Error: ${e.message ?: "verificación"}" }
            override fun onCodeSent(vid: String, token: PhoneAuthProvider.ForceResendingToken) { verificationId.value = vid; status.value = "Código enviado" }
        }
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phone)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(context as Activity)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Verificación de número")
        Text("Se envió un código a $phone")
        OutlinedTextField(value = code.value, onValueChange = { code.value = it.filter { ch -> ch.isDigit() } }, label = { Text("Código SMS") }, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            val vid = verificationId.value
            if (vid != null) {
                val cred = PhoneAuthProvider.getCredential(vid, code.value)
                auth.currentUser?.linkWithCredential(cred)?.addOnSuccessListener {
                    status.value = "Número vinculado"
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        FirebaseFirestore.getInstance().collection("users").document(uid)
                            .set(mapOf("fullNumber" to phone), SetOptions.merge())
                    }
                    onFinished()
                }
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Verificar") }
        if (status.value.isNotEmpty()) Text(status.value)
    }
}

