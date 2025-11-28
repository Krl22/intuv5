package com.intu.taxi.ui.screens

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
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
import java.util.concurrent.TimeUnit

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PhoneLoginScreen(onBack: () -> Unit, onLoggedIn: () -> Unit) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val countryCodes = listOf("+1", "+34", "+44", "+51", "+52", "+54", "+57", "+58")
    val countryExpanded = remember { mutableStateOf(false) }
    val countryCode = remember { mutableStateOf("+51") }
    val phone = remember { mutableStateOf("") }
    val code = remember { mutableStateOf("") }
    val verificationId = remember { mutableStateOf<String?>(null) }
    val status = remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Iniciar con teléfono")
        ExposedDropdownMenuBox(expanded = countryExpanded.value, onExpandedChange = { countryExpanded.value = !countryExpanded.value }) {
            TextField(
                value = countryCode.value,
                onValueChange = {},
                readOnly = true,
                label = { Text("Código") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = countryExpanded.value) },
                colors = ExposedDropdownMenuDefaults.textFieldColors(),
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            DropdownMenu(expanded = countryExpanded.value, onDismissRequest = { countryExpanded.value = false }) {
                countryCodes.forEach { codeOpt ->
                    DropdownMenuItem(text = { Text(codeOpt) }, onClick = {
                        countryCode.value = codeOpt
                        countryExpanded.value = false
                    })
                }
            }
        }
        OutlinedTextField(
            value = phone.value,
            onValueChange = { phone.value = it.filter { ch -> ch.isDigit() } },
            label = { Text("Teléfono (+NNNN...)") },
            modifier = Modifier.fillMaxWidth()
        )
        if (verificationId.value != null) {
            OutlinedTextField(
                value = code.value,
                onValueChange = { code.value = it.filter { ch -> ch.isDigit() } },
                label = { Text("Código SMS") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        Button(onClick = {
            if (verificationId.value == null) {
                val phoneInput = countryCode.value + phone.value
                if (phone.value.isBlank() || phoneInput.length < 8) {
                    status.value = "Ingresa un teléfono válido en formato +NNNN..."
                    return@Button
                }
                val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                        FirebaseAuth.getInstance().signInWithCredential(credential).addOnSuccessListener { onLoggedIn() }
                    }
                    override fun onVerificationFailed(e: FirebaseException) { status.value = "Error: ${e.message}" }
                    override fun onCodeSent(vid: String, token: PhoneAuthProvider.ForceResendingToken) {
                        verificationId.value = vid
                        status.value = "Código enviado"
                    }
                }
                val activity = context as? Activity
                if (activity == null) {
                    status.value = "No se pudo iniciar verificación (contexto)"
                    return@Button
                }
                val options = PhoneAuthOptions.newBuilder(auth)
                    .setPhoneNumber(phoneInput)
                    .setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(activity)
                    .setCallbacks(callbacks)
                    .build()
                PhoneAuthProvider.verifyPhoneNumber(options)
            } else {
                val vid = verificationId.value
                if (vid != null) {
                    val cred = PhoneAuthProvider.getCredential(vid, code.value)
                    auth.signInWithCredential(cred).addOnSuccessListener { onLoggedIn() }
                }
            }
        }, modifier = Modifier.fillMaxWidth()) { Text(if (verificationId.value == null) "Enviar código" else "Verificar código") }

        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Volver") }
        if (status.value.isNotEmpty()) Text(status.value)
    }
}
