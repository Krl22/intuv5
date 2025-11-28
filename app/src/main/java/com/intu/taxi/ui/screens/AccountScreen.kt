package com.intu.taxi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextField
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.FirebaseException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.auth.GoogleAuthProvider
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.intu.taxi.R
import java.util.concurrent.TimeUnit

@Composable
fun AccountScreen(onDebugClick: () -> Unit = {}, onLogout: () -> Unit = {}, onVerifyPhone: (String) -> Unit = {}) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    var profile by remember { mutableStateOf<Map<String, Any>?>(null) }
    var loading by remember { mutableStateOf(true) }
    DisposableEffect(uid) {
        val db = FirebaseFirestore.getInstance()
        val reg = if (uid != null) db.collection("users").document(uid).addSnapshotListener { doc, _ ->
            profile = doc?.data
            loading = false
        } else null
        onDispose { reg?.remove() }
    }
    val name = listOf(
        profile?.get("firstName") as? String ?: "",
        profile?.get("lastName") as? String ?: ""
    ).joinToString(" ").trim().ifEmpty { "Tu cuenta" }
    val email = profile?.get("email") as? String ?: ""
    val phone = (profile?.get("fullNumber") as? String) ?: (profile?.get("number") as? String) ?: ""
    val isPhoneLinked = FirebaseAuth.getInstance().currentUser?.phoneNumber?.isNotBlank() == true
    val initialGoogleLinked = FirebaseAuth.getInstance().currentUser?.providerData?.any { it.providerId == com.google.firebase.auth.GoogleAuthProvider.PROVIDER_ID } == true
    var isGoogleLinked by remember { mutableStateOf(initialGoogleLinked) }
    val context = LocalContext.current
    var linkStatus by remember { mutableStateOf("") }
    val googleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = runCatching { task.result }.getOrNull()
            if (account != null) {
                val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(account.idToken, null)
                FirebaseAuth.getInstance().currentUser?.linkWithCredential(credential)?.addOnSuccessListener {
                    isGoogleLinked = true
                    linkStatus = "Google vinculado"
                    val uidSafe = FirebaseAuth.getInstance().currentUser?.uid
                    val mail = account.email
                    if (uidSafe != null && mail != null) {
                        FirebaseFirestore.getInstance().collection("users").document(uidSafe)
                            .set(mapOf("email" to mail), SetOptions.merge())
                    }
                }?.addOnFailureListener { e -> linkStatus = "Error: ${e.message}" }
            }
        }
    }

    LaunchedEffect(linkStatus) {
        if (linkStatus.isNotEmpty()) {
            delay(2000)
            linkStatus = ""
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { ClientHeader(name) }
        item {
            ContactCard(
                email = email,
                phone = phone,
                isPhoneLinked = isPhoneLinked,
                onVerifyPhone = onVerifyPhone,
                isGoogleLinked = isGoogleLinked,
                onLinkGoogle = {
                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(context.getString(R.string.default_web_client_id))
                        .requestEmail()
                        .build()
                    val client = GoogleSignIn.getClient(context, gso)
                    googleLauncher.launch(client.signInIntent)
                },
                linkStatus = linkStatus
            )
        }
        item { PaymentCard() }
        item { SavedPlacesCard() }
        item { SupportPrivacyCard(onDebugClick, onLogout) }
    }
}

@Composable
private fun ClientHeader(title: String) {
    val gradient = Brush.horizontalGradient(
        listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary
        )
    )
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(gradient).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(56.dp).clip(CircleShape)
            )
            Spacer(Modifier.size(16.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Text(
                    "Cliente desde 2023",
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onPrimary)
                )
            }
        }
    }
}

@Composable
private fun ContactCard(
    email: String,
    phone: String,
    isPhoneLinked: Boolean,
    onVerifyPhone: (String) -> Unit,
    isGoogleLinked: Boolean,
    onLinkGoogle: () -> Unit,
    linkStatus: String
) {
    Card {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Contacto", style = MaterialTheme.typography.titleMedium)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            ListItem(
                headlineContent = { Text(email.ifEmpty { "Sin email" }) },
                leadingContent = { Icon(Icons.Filled.Email, contentDescription = null) },
                trailingContent = {
                    if (!isGoogleLinked) {
                        OutlinedButton(onClick = onLinkGoogle) { Text("Vincular Google") }
                    }
                }
            )
            ListItem(
                headlineContent = { Text(phone.ifEmpty { "Sin número" }) },
                leadingContent = { Icon(Icons.Filled.Phone, contentDescription = null) },
                trailingContent = {
                    if (!isPhoneLinked && phone.isNotEmpty()) {
                        OutlinedButton(onClick = { onVerifyPhone(phone) }) { Text("Verificar") }
                    }
                }
            )
            if (linkStatus.isNotEmpty()) Text(linkStatus, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PaymentCard() {
    Card {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Pago", style = MaterialTheme.typography.titleMedium)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            ListItem(headlineContent = { Text("Visa •••• 1234 (predeterminada)") }, leadingContent = { Icon(Icons.Filled.CreditCard, contentDescription = null) })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {}) { Text("Gestionar métodos") }
                OutlinedButton(onClick = {}) { Text("Agregar tarjeta") }
            }
        }
    }
}

@Composable
private fun SavedPlacesCard() {
    Card {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Lugares guardados", style = MaterialTheme.typography.titleMedium)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            ListItem(headlineContent = { Text("Casa: Av. Primavera 123") }, leadingContent = { Icon(Icons.Filled.Home, contentDescription = null) })
            ListItem(headlineContent = { Text("Trabajo: Calle Sol 456") }, leadingContent = { Icon(Icons.Filled.Work, contentDescription = null) })
            OutlinedButton(onClick = {}) { Text("Administrar lugares") }
        }
    }
}

@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
private fun PhoneLinkSection(uid: String?, existingPhone: String) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val countryCodes = listOf("+1", "+34", "+44", "+51", "+52", "+54", "+57", "+58")
    val countryExpanded = remember { mutableStateOf(false) }
    var countryCode by remember { mutableStateOf(existingPhone.takeWhile { it == '+' || it.isDigit() }.ifEmpty { "+51" }) }
    var number by remember { mutableStateOf( existingPhone.dropWhile { it == '+' || it.isDigit() } ) }
    var verificationId by remember { mutableStateOf<String?>(null) }
    var code by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }

    Card {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Vincular número")
            ExposedDropdownMenuBox(expanded = countryExpanded.value, onExpandedChange = { countryExpanded.value = !countryExpanded.value }) {
                TextField(
                    value = countryCode,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Código") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = countryExpanded.value) },
                    colors = ExposedDropdownMenuDefaults.textFieldColors(),
                    modifier = Modifier.menuAnchor()
                )
                DropdownMenu(expanded = countryExpanded.value, onDismissRequest = { countryExpanded.value = false }) {
                    countryCodes.forEach { codeOpt ->
                        DropdownMenuItem(text = { Text(codeOpt) }, onClick = {
                            countryCode = codeOpt
                            countryExpanded.value = false
                        })
                    }
                }
            }
            OutlinedTextField(value = number, onValueChange = { number = it.filter { ch -> ch.isDigit() } }, label = { Text("Número") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = code, onValueChange = { code = it.filter { ch -> ch.isDigit() } }, label = { Text("Código SMS") }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    val full = countryCode + number
                    val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                            auth.currentUser?.linkWithCredential(credential)?.addOnSuccessListener {
                                status = "Número vinculado"
                                val uidSafe = uid ?: return@addOnSuccessListener
                                FirebaseFirestore.getInstance().collection("users").document(uidSafe)
                                    .set(
                                        mapOf(
                                            "countryCode" to countryCode,
                                            "number" to number,
                                            "fullNumber" to full
                                        ), SetOptions.merge()
                                    )
                            }
                        }
                        override fun onVerificationFailed(e: FirebaseException) { status = "Error: ${e.message ?: "verificación"}" }
                        override fun onCodeSent(vid: String, token: PhoneAuthProvider.ForceResendingToken) { verificationId = vid; status = "Código enviado" }
                    }
                    val options = PhoneAuthOptions.newBuilder(auth)
                        .setPhoneNumber(full)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(context as Activity)
                        .setCallbacks(callbacks)
                        .build()
                    PhoneAuthProvider.verifyPhoneNumber(options)
                }) { Text("Enviar código") }
                Button(onClick = {
                    val vid = verificationId
                    if (vid != null) {
                        val cred = PhoneAuthProvider.getCredential(vid, code)
                        auth.currentUser?.linkWithCredential(cred)?.addOnSuccessListener {
                            status = "Número vinculado"
                            val full = countryCode + number
                            val uidSafe = uid ?: return@addOnSuccessListener
                            FirebaseFirestore.getInstance().collection("users").document(uidSafe)
                                .set(
                                    mapOf(
                                        "countryCode" to countryCode,
                                        "number" to number,
                                        "fullNumber" to full
                                    ), SetOptions.merge()
                                )
                        }
                    }
                }) { Text("Verificar y vincular") }
            }
            if (status.isNotEmpty()) Text(status, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SupportPrivacyCard(onDebugClick: () -> Unit, onLogout: () -> Unit) {
    Card {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Ayuda y privacidad", style = MaterialTheme.typography.titleMedium)
            HorizontalDivider()
            OutlinedButton(onClick = {}) { Text("Centro de ayuda") }
            OutlinedButton(onClick = {}) { Text("Privacidad") }
            OutlinedButton(onClick = onDebugClick) {
                Icon(Icons.Filled.BugReport, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Debug")
            }
            Button(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
                Text("Cerrar sesión")
            }
        }
    }
}
