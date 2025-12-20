package com.intu.taxi.ui.screens

import android.app.Activity
import android.util.Log
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Lock
import androidx.compose.ui.draw.drawBehind
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.intu.taxi.ui.debug.DebugLog
import java.util.concurrent.TimeUnit
import com.intu.taxi.BuildConfig

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PhoneLoginScreen(onBack: () -> Unit, onLoggedIn: () -> Unit) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    if (BuildConfig.DEBUG && isProbablyEmulator()) {
        try {
            FirebaseAuth.getInstance().firebaseAuthSettings.setAppVerificationDisabledForTesting(true)
            DebugLog.log("App verification disabled for testing on emulator")
            Log.w("PhoneLogin", "App verification disabled for testing on emulator")
        } catch (e: Exception) {
            Log.e("PhoneLogin", "Failed to disable app verification for testing", e)
        }
    }
    val countryCodes = listOf("+1", "+34", "+44", "+51", "+52", "+54", "+57", "+58")
    val countryExpanded = remember { mutableStateOf(false) }
    val countryCode = remember { mutableStateOf("+51") }
    val phone = remember { mutableStateOf("") }
    val code = remember { mutableStateOf("") }
    val verificationId = remember { mutableStateOf<String?>(null) }
    val resendToken = remember { mutableStateOf<PhoneAuthProvider.ForceResendingToken?>(null) }
    val status = remember { mutableStateOf("") }
    val loading = remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val teal = Color(0xFF08817E)
                val indigo = Color(0xFF1E1F47)
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(teal, indigo),
                        center = androidx.compose.ui.geometry.Offset(0.1f, 0.1f),
                        radius = size.height * 0.9f
                    ),
                    size = size
                )
                drawRect(
                    brush = Brush.radialGradient(
                        colorStops = arrayOf(
                            0.00f to Color.White.copy(alpha = 1.0f),
                            0.70f to Color.White.copy(alpha = 1.0f),
                            0.75f to Color.White.copy(alpha = 0.95f),
                            0.80f to Color.White.copy(alpha = 0.85f),
                            0.85f to Color.White.copy(alpha = 0.70f),
                            0.90f to Color.White.copy(alpha = 0.45f),
                            0.95f to Color.White.copy(alpha = 0.25f),
                            1.00f to Color.Transparent
                        ),
                        center = androidx.compose.ui.geometry.Offset(0f, 0f),
                        radius = kotlin.math.max(size.width, size.height)
                    ),
                    size = size,
                    blendMode = androidx.compose.ui.graphics.BlendMode.DstIn
                )
            }
    ) {
        Column(Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .align(Alignment.Center), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                stringResource(com.intu.taxi.R.string.login_phone_title),
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                color = Color.White
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.14f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF14B8A6).copy(alpha = 0.18f),
                                    Color(0xFF4F46E5).copy(alpha = 0.08f),
                                    Color.Transparent
                                ),
                                center = androidx.compose.ui.geometry.Offset(0.7f, 0.2f),
                                radius = 700f
                            )
                        )
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ExposedDropdownMenuBox(expanded = countryExpanded.value, onExpandedChange = { countryExpanded.value = !countryExpanded.value }) {
                        TextField(
                            value = countryCode.value,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(com.intu.taxi.R.string.country_code_label)) },
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
                        label = { Text(stringResource(com.intu.taxi.R.string.phone_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                        leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null, tint = Color(0xFF08817E)) },
                        shape = RoundedCornerShape(16.dp),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color(0xFF08817E),
                            unfocusedIndicatorColor = Color(0x661E1F47),
                            cursorColor = Color(0xFF08817E)
                        )
                    )
                    if (verificationId.value != null) {
                        OutlinedTextField(
                            value = code.value,
                            onValueChange = { code.value = it.filter { ch -> ch.isDigit() } },
                            label = { Text(stringResource(com.intu.taxi.R.string.sms_code_label)) },
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null, tint = Color(0xFF1E1F47)) },
                            shape = RoundedCornerShape(16.dp),
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color(0xFF1E1F47),
                                unfocusedIndicatorColor = Color(0x6608817E),
                                cursorColor = Color(0xFF1E1F47)
                            )
                        )
                    }
                    Button(onClick = {
                        if (verificationId.value == null) {
                            val ccDigits = countryCode.value.filter { it.isDigit() }
                            val phoneDigits = phone.value.filter { it.isDigit() }
                            val totalDigits = ccDigits.length + phoneDigits.length
                            val phoneInput = "+$ccDigits$phoneDigits"
                            if (phoneDigits.isBlank() || totalDigits < 8 || totalDigits > 15) {
                                status.value = context.getString(com.intu.taxi.R.string.status_invalid_phone_format)
                                return@Button
                            }
                            loading.value = true
                            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                                    loading.value = false
                                    DebugLog.log("onVerificationCompleted")
                                    Log.d("PhoneLogin", "onVerificationCompleted")
                                    FirebaseAuth.getInstance().signInWithCredential(credential).addOnSuccessListener {
                                        DebugLog.log("signInWithCredential success")
                                        Log.d("PhoneLogin", "signInWithCredential success")
                                        onLoggedIn()
                                    }.addOnFailureListener {
                                        status.value = context.getString(com.intu.taxi.R.string.status_login_error)
                                        DebugLog.log("signInWithCredential failure: ${it.message}")
                                        Log.e("PhoneLogin", "signInWithCredential failure", it)
                                    }
                                }
                                override fun onVerificationFailed(e: FirebaseException) {
                                    loading.value = false
                                    status.value = when (e) {
                                        is FirebaseAuthInvalidCredentialsException -> context.getString(com.intu.taxi.R.string.status_invalid_number)
                                        is FirebaseTooManyRequestsException -> context.getString(com.intu.taxi.R.string.status_too_many_requests)
                                        else -> {
                                            val msg = e.message ?: ""
                                            if (msg.contains("missing a valid app identifier", ignoreCase = true)) {
                                                context.getString(com.intu.taxi.R.string.status_integrity_error)
                                            } else if (msg.contains("internal error", ignoreCase = true)) {
                                                context.getString(com.intu.taxi.R.string.status_provider_error)
                                            } else {
                                                "${context.getString(com.intu.taxi.R.string.error_prefix)}${e.message}"
                                            }
                                        }
                                    }
                                    DebugLog.log("onVerificationFailed: ${e::class.java.simpleName} - ${e.message}")
                                    Log.e("PhoneLogin", "onVerificationFailed", e)
                                }
                                override fun onCodeSent(vid: String, token: PhoneAuthProvider.ForceResendingToken) {
                                    loading.value = false
                                    verificationId.value = vid
                                    resendToken.value = token
                                    status.value = context.getString(com.intu.taxi.R.string.code_sent_status)
                                    DebugLog.log("onCodeSent")
                                    Log.d("PhoneLogin", "onCodeSent")
                                }
                            }
                            val activity = context as? Activity
                            if (activity == null) {
                                loading.value = false
                                status.value = context.getString(com.intu.taxi.R.string.status_context_error)
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
                                loading.value = true
                                val cred = PhoneAuthProvider.getCredential(vid, code.value)
                                auth.signInWithCredential(cred).addOnSuccessListener {
                                    DebugLog.log("signInWithCredential success manual code")
                                    Log.d("PhoneLogin", "signInWithCredential success manual code")
                                    onLoggedIn()
                                }.addOnCompleteListener {
                                    loading.value = false
                                }.addOnFailureListener {
                                    status.value = context.getString(com.intu.taxi.R.string.status_invalid_code)
                                    DebugLog.log("signInWithCredential failure manual code: ${it.message}")
                                    Log.e("PhoneLogin", "signInWithCredential failure manual code", it)
                                }
                            }
                        }
                    },
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF08817E), Color(0xFF1E1F47))
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !loading.value) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(if (verificationId.value == null) Icons.Filled.Phone else Icons.Filled.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                            Text(if (verificationId.value == null) stringResource(com.intu.taxi.R.string.send_code_button) else stringResource(com.intu.taxi.R.string.verify_code_button))
                        }
                    }
                    if (verificationId.value != null) {
                        OutlinedButton(
                            onClick = {
                                val activity = context as? Activity
                                val token = resendToken.value
                                val ccDigits = countryCode.value.filter { it.isDigit() }
                                val phoneDigits = phone.value.filter { it.isDigit() }
                                val phoneInput = "+$ccDigits$phoneDigits"
                                if (activity != null && token != null) {
                                    loading.value = true
                                    val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                                        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                                            loading.value = false
                                            DebugLog.log("onVerificationCompleted (resend)")
                                            Log.d("PhoneLogin", "onVerificationCompleted (resend)")
                                            FirebaseAuth.getInstance().signInWithCredential(credential).addOnSuccessListener {
                                                onLoggedIn()
                                            }.addOnFailureListener {
                                                status.value = context.getString(com.intu.taxi.R.string.status_login_error)
                                            }
                                        }
                                        override fun onVerificationFailed(e: FirebaseException) {
                                            loading.value = false
                                            status.value = when (e) {
                                                is FirebaseAuthInvalidCredentialsException -> context.getString(com.intu.taxi.R.string.status_invalid_number)
                                                is FirebaseTooManyRequestsException -> context.getString(com.intu.taxi.R.string.status_too_many_requests)
                                                else -> {
                                                    val msg = e.message ?: ""
                                                    if (msg.contains("missing a valid app identifier", ignoreCase = true)) {
                                                        context.getString(com.intu.taxi.R.string.status_integrity_error)
                                                    } else {
                                                        "${context.getString(com.intu.taxi.R.string.error_prefix)}${e.message}"
                                                    }
                                                }
                                            }
                                            DebugLog.log("onVerificationFailed (resend): ${e.message}")
                                            Log.e("PhoneLogin", "onVerificationFailed (resend)", e)
                                        }
                                        override fun onCodeSent(vid: String, tokenNew: PhoneAuthProvider.ForceResendingToken) {
                                            loading.value = false
                                            verificationId.value = vid
                                            resendToken.value = tokenNew
                                            status.value = context.getString(com.intu.taxi.R.string.status_code_resent)
                                            DebugLog.log("onCodeSent (resend)")
                                            Log.d("PhoneLogin", "onCodeSent (resend)")
                                        }
                                    }
                                    val options = PhoneAuthOptions.newBuilder(auth)
                                        .setPhoneNumber(phoneInput)
                                        .setTimeout(60L, TimeUnit.SECONDS)
                                        .setActivity(activity)
                                        .setCallbacks(callbacks)
                                        .setForceResendingToken(token)
                                        .build()
                                    PhoneAuthProvider.verifyPhoneNumber(options)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !loading.value && resendToken.value != null,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF1E1F47))
                        ) {
                            Text(stringResource(com.intu.taxi.R.string.resend_code))
                        }
                    }
                }
            }

            Divider()

            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.12f), shape = RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                enabled = !loading.value,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
            ) { Text("Volver") }
            if (status.value.isNotEmpty()) Text(status.value, color = Color.White)
        }
    }
}

private fun isProbablyEmulator(): Boolean {
    val fp = Build.FINGERPRINT.lowercase()
    val model = Build.MODEL.lowercase()
    val brand = Build.BRAND.lowercase()
    val device = Build.DEVICE.lowercase()
    val product = Build.PRODUCT.lowercase()
    return fp.contains("generic")
            || fp.contains("unknown")
            || brand.contains("generic") && device.contains("generic")
            || product.contains("sdk_gphone")
            || model.contains("emulator")
            || model.contains("android sdk built for x86")
}
