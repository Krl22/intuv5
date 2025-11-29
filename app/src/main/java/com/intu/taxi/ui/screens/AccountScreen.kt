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
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.PriceChange
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
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
import com.google.firebase.storage.FirebaseStorage

@Composable
fun AccountScreen(onDebugClick: () -> Unit = {}, onLogout: () -> Unit = {}, onVerifyPhone: (String) -> Unit = {}, onStartDriver: () -> Unit = {}) {
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
    val photoUrl = profile?.get("photoUrl") as? String ?: ""
    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val u = uid
        if (uri != null && u != null) {
            val ref = FirebaseStorage.getInstance().reference.child("users/$u/profile.jpg")
            ref.putFile(uri).addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { url ->
                    FirebaseFirestore.getInstance().collection("users").document(u)
                        .set(mapOf("photoUrl" to url.toString()), SetOptions.merge())
                }
            }
        }
    }
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

    // Country selection: default from phone prefix or Firestore field if present
    val storedCountry = (profile?.get("country") as? String)?.lowercase()
    var country by remember {
        mutableStateOf(
            when {
                storedCountry == "peru" -> "peru"
                storedCountry == "usa" -> "usa"
                phone.startsWith("+51") -> "peru"
                else -> "usa"
            }
        )
    }
    fun saveCountry(newCountry: String) {
        val u = uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(u)
            .set(mapOf("country" to newCountry), SetOptions.merge())
        country = newCountry
    }

    // Payment method state based on country
    var paymentMethod by remember {
        mutableStateOf(
            (profile?.get("paymentMethod") as? String)
                ?: if (country == "peru") "efectivo" else "tarjeta"
        )
    }
    fun savePayment(method: String) {
        val u = uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(u)
            .set(mapOf("paymentMethod" to method), SetOptions.merge())
        paymentMethod = method
    }

    val screenBg = Brush.linearGradient(listOf(Color(0xFF08817E), Color(0xFF1E1F47)))
    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize().background(screenBg)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { ClientHeader(name, photoUrl, onChangePhoto = { imagePicker.launch("image/*") }) }
            item { CountrySelector(country = country, onSelect = { saveCountry(it) }) }
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
            item { PaymentCard(country = country, method = paymentMethod, onChange = { savePayment(it) }) }
            val driverApproved = (profile?.get("driverApproved") as? Boolean) == true
            val driverMode = (profile?.get("driverMode") as? Boolean) == true
            if (driverApproved && driverMode) {
                val driverData = profile?.get("driver") as? Map<String, Any> ?: emptyMap()
                val vehiclePhoto = (driverData["vehiclePhotoUrl"] as? String) ?: ""
                val vehicleType = (driverData["vehicleType"] as? String) ?: ""
                val vehicleBrand = (driverData["vehicleBrand"] as? String) ?: (driverData["vehicleMake"] as? String ?: "")
                val vehicleModel = (driverData["vehicleModel"] as? String) ?: ""
                val vehicleYear = (driverData["vehicleYear"] as? String) ?: ""
                val vehiclePlate = (driverData["vehiclePlate"] as? String) ?: ""
                item { VehicleCard(photoUrl = vehiclePhoto, type = vehicleType, brand = vehicleBrand, model = vehicleModel, year = vehicleYear, plate = vehiclePlate) }
                item { DriverStatsCard() }
                item { DriverRecentTripsCard() }
            }
            item { SavedPlacesCard() }
            item { SupportPrivacyCard(onDebugClick, onLogout) }
            item {
                DriverSection(
                    uid = uid,
                    approved = driverApproved,
                    driverMode = driverMode,
                    submitted = profile?.get("driver") != null,
                    onStartDriver = onStartDriver,
                    onToggleMode = { enabled ->
                        val u = uid
                        if (u != null) FirebaseFirestore.getInstance().collection("users").document(u).set(mapOf("driverMode" to enabled), SetOptions.merge())
                    }
                )
            }
        }
    }
}

@Composable
private fun ClientHeader(title: String, photoUrl: String, onChangePhoto: () -> Unit) {
    val gradient = Brush.horizontalGradient(listOf(Color(0xFF08817E), Color(0xFF1E1F47)))
    Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f)), shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(gradient).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (photoUrl.isNotBlank()) {
                AsyncImage(model = photoUrl, contentDescription = null, modifier = Modifier.size(56.dp).clip(CircleShape))
            } else {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(56.dp).clip(CircleShape)
                )
            }
            Spacer(Modifier.size(16.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Text(
                    "Cliente desde 2023",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.85f))
                )
            }
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = { onChangePhoto() }) { Text("Cambiar foto") }
        }
    }
}

@Composable
private fun CountrySelector(country: String, onSelect: (String) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f)), shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("País", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = { onSelect("peru") }, enabled = country != "peru") { Text("Perú") }
            OutlinedButton(onClick = { onSelect("usa") }, enabled = country != "usa") { Text("USA") }
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
    Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f)), shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Contacto", style = MaterialTheme.typography.titleMedium)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            ListItem(
                headlineContent = { Text(email.ifEmpty { "Sin email" }) },
                leadingContent = { Icon(Icons.Filled.Email, contentDescription = null, tint = Color(0xFF1E1F47)) },
                trailingContent = {
                    if (!isGoogleLinked) {
                        OutlinedButton(onClick = onLinkGoogle) { Text("Vincular Google") }
                    }
                }
            )
            ListItem(
                headlineContent = { Text(phone.ifEmpty { "Sin número" }) },
                leadingContent = { Icon(Icons.Filled.Phone, contentDescription = null, tint = Color(0xFF08817E)) },
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
private fun PaymentCard(country: String, method: String, onChange: (String) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f)), shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Pago", style = MaterialTheme.typography.titleMedium)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            if (country == "peru") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { onChange("yape_plin") }, enabled = method != "yape_plin") { Text("Yape/Plin") }
                    OutlinedButton(onClick = { onChange("efectivo") }, enabled = method != "efectivo") { Text("Efectivo") }
                }
                Text(
                    when (method) {
                        "yape_plin" -> "Método: Yape/Plin"
                        else -> "Método: Efectivo"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                ListItem(headlineContent = { Text("Visa •••• 1234 (predeterminado)") }, leadingContent = { Icon(Icons.Filled.CreditCard, contentDescription = null) })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {}) { Text("Gestionar métodos") }
                    OutlinedButton(onClick = {}) { Text("Agregar tarjeta") }
                }
            }
        }
    }
}

@Composable
private fun SavedPlacesCard() {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f)), shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) {
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
private fun VehicleCard(photoUrl: String, type: String, brand: String, model: String, year: String, plate: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f)), shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Datos del vehículo", style = MaterialTheme.typography.titleMedium)
            HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (photoUrl.isNotBlank()) {
                    AsyncImage(model = photoUrl, contentDescription = null, modifier = Modifier.size(72.dp).clip(CircleShape))
                } else {
                    Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = Color(0xFF08817E), modifier = Modifier.size(48.dp))
                }
                Spacer(Modifier.size(16.dp))
                Column {
                    Text(listOf(type, brand, model).filter { it.isNotBlank() }.joinToString(" "), style = MaterialTheme.typography.titleSmall)
                    Text(listOf("Año $year", "Placa $plate").filter { it.isNotBlank() }.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
private fun DriverStatsCard() {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f)), shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Ingresos y viajes", style = MaterialTheme.typography.titleMedium)
            HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.TrendingUp, contentDescription = null, tint = Color(0xFF1E1F47))
                Spacer(Modifier.size(8.dp))
                Text("Hoy: S/ 120.00 · 6 viajes", style = MaterialTheme.typography.bodyMedium)
            }
            LinearProgressIndicator(progress = 0.6f, color = Color(0xFF08817E))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.PriceChange, contentDescription = null, tint = Color(0xFF08817E))
                Spacer(Modifier.size(8.dp))
                Text("Semana: S/ 840.00 · 42 viajes", style = MaterialTheme.typography.bodyMedium)
            }
            LinearProgressIndicator(progress = 0.42f, color = Color(0xFF1E1F47))
        }
    }
}

@Composable
private fun DriverRecentTripsCard() {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f)), shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Últimos viajes", style = MaterialTheme.typography.titleMedium)
            HorizontalDivider()
            ListItem(headlineContent = { Text("12:10 · Centro → Aeropuerto · S/ 45.00") }, leadingContent = { Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = Color(0xFF08817E)) })
            ListItem(headlineContent = { Text("10:35 · Universidad → Mall · S/ 23.50") }, leadingContent = { Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = Color(0xFF08817E)) })
            ListItem(headlineContent = { Text("09:05 · Casa → Oficina · S/ 18.00") }, leadingContent = { Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = Color(0xFF08817E)) })
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
    Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f)), shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) {
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

@Composable
private fun DriverSection(
    uid: String?,
    approved: Boolean,
    driverMode: Boolean,
    submitted: Boolean,
    onStartDriver: () -> Unit,
    onToggleMode: (Boolean) -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f)), shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Conduce y gana", style = MaterialTheme.typography.titleMedium)
            HorizontalDivider()
            if (!approved) {
                if (submitted) {
                    OutlinedButton(onClick = {}, enabled = false) { Text("Solicitud en revisión") }
                    Text("Tu solicitud está siendo revisada", style = MaterialTheme.typography.bodySmall)
                } else {
                    Button(onClick = onStartDriver, modifier = Modifier.fillMaxWidth()) { Text("Ganar con Intu") }
                    Text("Completa datos de vehículo y licencia", style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Modo conductor")
                    Spacer(Modifier.weight(1f))
                    androidx.compose.material3.Switch(checked = driverMode, onCheckedChange = onToggleMode)
                }
            }
        }
    }
}
