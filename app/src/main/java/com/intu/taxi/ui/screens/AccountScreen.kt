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
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.ButtonDefaults
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
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import android.content.Context
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.foundation.clickable

data class SavedPlace(val type: String, val name: String, val lat: Double, val lon: Double)

@Composable
fun AccountScreen(
    onDebugClick: () -> Unit = {},
    onLogout: () -> Unit = {},
    onVerifyPhone: (String) -> Unit = {},
    onStartDriver: () -> Unit = {},
    onStartTopUp: () -> Unit = {},
    onStartPickPlace: (String) -> Unit = {},
    pendingPickedPlace: SavedPlace? = null
) {
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

    LaunchedEffect(pendingPickedPlace) {
        val p = pendingPickedPlace
        val u = uid
        if (p != null && u != null) {
            val placeMap = mapOf("name" to p.name, "lat" to p.lat, "lon" to p.lon)
            val field = when (p.type) { "home" -> "savedPlaces.home"; "work" -> "savedPlaces.work"; else -> "savedPlaces.other" }
            FirebaseFirestore.getInstance().collection("users").document(u)
                .set(mapOf(field to placeMap), SetOptions.merge())
        }
    }
    val name = listOf(
        profile?.get("firstName") as? String ?: "",
        profile?.get("lastName") as? String ?: ""
    ).joinToString(" ").trim().ifEmpty { stringResource(R.string.account_title_fallback) }
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
                    linkStatus = context.getString(R.string.google_linked_success)
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

    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val teal = Color(0xFF08817E)
                val indigo = Color(0xFF1E1F47)
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(teal, indigo),
                        center = Offset(0.1f, 0.1f),
                        radius = size.height * 0.9f
                    ),
                    size = Size(width = size.width, height = size.height)
                )
                withTransform({
                    scale(scaleX = 1.6f, scaleY = 1.0f, pivot = Offset.Zero)
                }) {
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
                            center = Offset(0f, 0f),
                            radius = kotlin.math.max(size.width, size.height)
                        ),
                        size = Size(width = size.width, height = size.height),
                        blendMode = BlendMode.DstIn
                    )
                }
            }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ClientHeader(
                    title = name,
                    photoUrl = photoUrl,
                    onChangePhoto = { imagePicker.launch("image/*") },
                    showDriverToggle = ((profile?.get("driverApproved") as? Boolean) == true),
                    driverMode = ((profile?.get("driverMode") as? Boolean) == true),
                    onToggleDriver = { enabled ->
                        val u2 = uid
                        if (u2 != null) FirebaseFirestore.getInstance().collection("users").document(u2).set(mapOf("driverMode" to enabled), SetOptions.merge())
                    }
                )
            }
            
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
            if (((profile?.get("driverApproved") as? Boolean) == true) && ((profile?.get("driverMode") as? Boolean) == true)) {
                val driverData = profile?.get("driver") as? Map<String, Any> ?: emptyMap()
                val vehiclePhoto = (driverData["vehiclePhotoUrl"] as? String) ?: ""
                val vehicleType = (driverData["vehicleType"] as? String) ?: ""
                val vehicleBrand = (driverData["vehicleBrand"] as? String) ?: (driverData["vehicleMake"] as? String ?: "")
                val vehicleModel = (driverData["vehicleModel"] as? String) ?: ""
                val vehicleYear = (driverData["vehicleYear"] as? String) ?: ""
                val vehiclePlate = (driverData["vehiclePlate"] as? String) ?: ""
                val balance = (profile?.get("balance") as? Number)?.toDouble() ?: 0.0
                item { DriverBalanceCard(balance = balance, onRecharge = onStartTopUp) }
                item { VehicleCard(photoUrl = vehiclePhoto, type = vehicleType, brand = vehicleBrand, model = vehicleModel, year = vehicleYear, plate = vehiclePlate) }
                item { DriverStatsCard() }
                item { DriverRecentTripsCard() }
            }
            item {
                SavedPlacesCard(
                    profile = profile,
                    onSetHome = { onStartPickPlace("home") },
                    onSetWork = { onStartPickPlace("work") }
                )
            }
            item { CountrySelector(country = country, onSelect = { saveCountry(it) }) }
            item { LanguageSelector() }
            item { SupportPrivacyCard(onDebugClick, onLogout) }
            item {
                DriverSection(
                    uid = uid,
                    approved = ((profile?.get("driverApproved") as? Boolean) == true),
                    driverMode = ((profile?.get("driverMode") as? Boolean) == true),
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
private fun ClientHeader(title: String, photoUrl: String, onChangePhoto: () -> Unit, showDriverToggle: Boolean = false, driverMode: Boolean = false, onToggleDriver: (Boolean) -> Unit = {}) {
    val gradient = Brush.horizontalGradient(listOf(Color(0xFF0D9488), Color(0xFF0F172A)))
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.15f)),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
    ) {
        Box(modifier = Modifier.fillMaxWidth().background(gradient)) {
            if (showDriverToggle) {
                Row(modifier = Modifier.align(Alignment.TopEnd).padding(top = 12.dp, end = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Modo conductor", color = Color.White, style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.size(8.dp))
                    androidx.compose.material3.Switch(checked = driverMode, onCheckedChange = onToggleDriver)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFF0D9488), Color(0xFF22D3EE))
                        )
                    )
                    .clickable { onChangePhoto() },
                contentAlignment = Alignment.Center
            ) {
                if (photoUrl.isNotBlank()) {
                    AsyncImage(model = photoUrl, contentDescription = null, modifier = Modifier.size(56.dp).clip(CircleShape))
                } else {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                }
            }
            Spacer(Modifier.size(16.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    stringResource(R.string.client_since),
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.White.copy(alpha = 0.85f))
                )
            }
            Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CountrySelector(country: String, onSelect: (String) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.98f)),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.country_label), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = { onSelect("peru") }, enabled = country != "peru") { Text(stringResource(R.string.country_peru)) }
            OutlinedButton(onClick = { onSelect("usa") }, enabled = country != "usa") { Text(stringResource(R.string.country_usa)) }
        }
    }
}

@Composable
private fun LanguageSelector() {
    val ctx = LocalContext.current
    val prefs = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE)
    Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.98f)), shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), elevation = CardDefaults.cardElevation(8.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.language_label), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.weight(1f))
            OutlinedButton(onClick = {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("es"))
                prefs.edit().putBoolean("lang_user_set", true).apply()
                (ctx as? Activity)?.recreate()
            }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488), contentColor = Color.White)) { Text(stringResource(R.string.language_es)) }
            OutlinedButton(onClick = {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
                prefs.edit().putBoolean("lang_user_set", true).apply()
                (ctx as? Activity)?.recreate()
            }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A), contentColor = Color.White)) { Text(stringResource(R.string.language_en)) }
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
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.98f)),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(stringResource(R.string.contact_label), style = MaterialTheme.typography.titleMedium)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            ListItem(
                headlineContent = { Text(email.ifEmpty { stringResource(R.string.no_email) }) },
                leadingContent = { Icon(Icons.Filled.Email, contentDescription = null, tint = Color(0xFF0F172A)) },
                trailingContent = {
                    if (!isGoogleLinked) {
                        OutlinedButton(onClick = onLinkGoogle) { Text(stringResource(R.string.link_google)) }
                    }
                }
            )
            ListItem(
                headlineContent = { Text(phone.ifEmpty { stringResource(R.string.no_phone) }) },
                leadingContent = { Icon(Icons.Filled.Phone, contentDescription = null, tint = Color(0xFF0D9488)) },
    
                trailingContent = {
                    if (!isPhoneLinked && phone.isNotEmpty()) {
                        OutlinedButton(onClick = { onVerifyPhone(phone) }) { Text(stringResource(R.string.verify)) }
                    }
                }
            )
            if (linkStatus.isNotEmpty()) Text(linkStatus, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun PaymentCard(country: String, method: String, onChange: (String) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.98f)),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.payment_label), style = MaterialTheme.typography.titleMedium)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            if (country == "peru") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onChange("yape_plin") }, enabled = method != "yape_plin", colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488), contentColor = Color.White)) { Text(stringResource(R.string.method_yape_plin)) }
                    OutlinedButton(onClick = { onChange("efectivo") }, enabled = method != "efectivo") { Text(stringResource(R.string.method_cash)) }
                }
                Text(
                    when (method) {
                        "yape_plin" -> stringResource(R.string.method_selected_yape_plin)
                        else -> stringResource(R.string.method_selected_cash)
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                ListItem(headlineContent = { Text(stringResource(R.string.method_card_default)) }, leadingContent = { Icon(Icons.Filled.CreditCard, contentDescription = null) })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {}, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488), contentColor = Color.White)) { Text(stringResource(R.string.manage_methods)) }
                    OutlinedButton(onClick = {}) { Text(stringResource(R.string.add_card)) }
                }
            }
        }
    }
}

@Composable
private fun SavedPlacesCard(
    profile: Map<String, Any>?,
    onSetHome: () -> Unit,
    onSetWork: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.98f)),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.saved_places_label), style = MaterialTheme.typography.titleMedium)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            val sp = profile?.get("savedPlaces") as? Map<String, Any> ?: emptyMap()
            val home = sp["home"] as? Map<String, Any>
            val work = sp["work"] as? Map<String, Any>
            ListItem(
                headlineContent = { Text((home?.get("name") as? String) ?: stringResource(R.string.home_place)) },
                leadingContent = { Icon(Icons.Filled.Home, contentDescription = null, tint = Color(0xFF0D9488)) },
                trailingContent = { OutlinedButton(onClick = onSetHome) { Text("Establecer casa") } }
            )
            ListItem(
                headlineContent = { Text((work?.get("name") as? String) ?: stringResource(R.string.work_place)) },
                leadingContent = { Icon(Icons.Filled.Work, contentDescription = null, tint = Color(0xFF0F172A)) },
                trailingContent = { OutlinedButton(onClick = onSetWork) { Text("Establecer trabajo") } }
            )
            OutlinedButton(onClick = {}) { Text(stringResource(R.string.manage_places)) }
        }
    }
}

@Composable
private fun VehicleCard(photoUrl: String, type: String, brand: String, model: String, year: String, plate: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f)),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.vehicle_data_label), style = MaterialTheme.typography.titleMedium)
            HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (photoUrl.isNotBlank()) {
                    AsyncImage(model = photoUrl, contentDescription = null, modifier = Modifier.size(72.dp).clip(CircleShape))
                } else {
                    Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = Color(0xFF0D9488), modifier = Modifier.size(48.dp))
                }
                Spacer(Modifier.size(16.dp))
                Column {
                    Text(listOf(type, brand, model).filter { it.isNotBlank() }.joinToString(" "), style = MaterialTheme.typography.titleSmall)
                    Text(listOf(
                        (if (year.isNotBlank()) stringResource(R.string.year_label_prefix) + year else ""),
                        (if (plate.isNotBlank()) stringResource(R.string.plate_label_prefix) + plate else "")
                    ).filter { it.isNotBlank() }.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
private fun DriverStatsCard() {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.98f)), shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.earnings_trips_label), style = MaterialTheme.typography.titleMedium)
            HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.TrendingUp, contentDescription = null, tint = Color(0xFF0F172A))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.today_summary), style = MaterialTheme.typography.bodyMedium)
            }
            LinearProgressIndicator(progress = 0.6f, color = Color(0xFF0D9488))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.PriceChange, contentDescription = null, tint = Color(0xFF0D9488))
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.week_summary), style = MaterialTheme.typography.bodyMedium)
            }
            LinearProgressIndicator(progress = 0.42f, color = Color(0xFF0F172A))
        }
    }
}

@Composable
private fun DriverBalanceCard(balance: Double, onRecharge: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.98f)), shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Saldo", style = MaterialTheme.typography.titleMedium)
                Text("S/ %.2f".format(balance), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            }
            Button(onClick = onRecharge, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488), contentColor = Color.White)) { Text("Recargar saldo") }
        }
    }
}

@Composable
private fun DriverRecentTripsCard() {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.98f)), shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.recent_trips_label), style = MaterialTheme.typography.titleMedium)
            HorizontalDivider()
            ListItem(headlineContent = { Text("12:10 · Centro → Aeropuerto · S/ 45.00") }, leadingContent = { Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = Color(0xFF0D9488)) })
            ListItem(headlineContent = { Text("10:35 · Universidad → Mall · S/ 23.50") }, leadingContent = { Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = Color(0xFF0D9488)) })
            ListItem(headlineContent = { Text("09:05 · Casa → Oficina · S/ 18.00") }, leadingContent = { Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = Color(0xFF0D9488)) })
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
            Text(stringResource(R.string.link_phone_label))
            ExposedDropdownMenuBox(expanded = countryExpanded.value, onExpandedChange = { countryExpanded.value = !countryExpanded.value }) {
                TextField(
                    value = countryCode,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.country_code_label)) },
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
            OutlinedTextField(value = number, onValueChange = { number = it.filter { ch -> ch.isDigit() } }, label = { Text(stringResource(R.string.phone_number_label)) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = code, onValueChange = { code = it.filter { ch -> ch.isDigit() } }, label = { Text(stringResource(R.string.sms_code_label)) }, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    val full = countryCode + number
                    val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                            auth.currentUser?.linkWithCredential(credential)?.addOnSuccessListener {
                                status = context.getString(R.string.phone_linked_success)
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
                        override fun onCodeSent(vid: String, token: PhoneAuthProvider.ForceResendingToken) { verificationId = vid; status = context.getString(R.string.code_sent) }
                    }
                    val options = PhoneAuthOptions.newBuilder(auth)
                        .setPhoneNumber(full)
                        .setTimeout(60L, TimeUnit.SECONDS)
                        .setActivity(context as Activity)
                        .setCallbacks(callbacks)
                        .build()
                    PhoneAuthProvider.verifyPhoneNumber(options)
                }) { Text(stringResource(R.string.send_code)) }
                Button(onClick = {
                    val vid = verificationId
                    if (vid != null) {
                        val cred = PhoneAuthProvider.getCredential(vid, code)
                        auth.currentUser?.linkWithCredential(cred)?.addOnSuccessListener {
                            status = context.getString(R.string.phone_linked_success)
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
                }) { Text(stringResource(R.string.verify_and_link)) }
            }
            if (status.isNotEmpty()) Text(status, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun SupportPrivacyCard(onDebugClick: () -> Unit, onLogout: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.98f)), shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.support_privacy_label), style = MaterialTheme.typography.titleMedium)
            HorizontalDivider()
            OutlinedButton(onClick = {}) { Text(stringResource(R.string.help_center)) }
            OutlinedButton(onClick = {}) { Text(stringResource(R.string.privacy)) }
            OutlinedButton(onClick = onDebugClick) {
                Icon(Icons.Filled.BugReport, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.debug))
            }
            Button(onClick = onLogout, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A), contentColor = Color.White)) {
                Text(stringResource(R.string.logout))
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
    Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.98f)), shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.drive_and_earn_section), style = MaterialTheme.typography.titleMedium)
            HorizontalDivider()
            if (!approved) {
                if (submitted) {
                    OutlinedButton(onClick = {}, enabled = false) { Text(stringResource(R.string.driver_review_pending)) }
                    Text(stringResource(R.string.driver_review_help), style = MaterialTheme.typography.bodySmall)
                } else {
                    Button(onClick = onStartDriver, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488), contentColor = Color.White)) { Text(stringResource(R.string.gain_with_intu)) }
                    Text(stringResource(R.string.driver_complete_data), style = MaterialTheme.typography.bodySmall)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.driver_mode_label))
                    Spacer(Modifier.weight(1f))
                    androidx.compose.material3.Switch(checked = driverMode, onCheckedChange = onToggleMode)
                }
            }
        }
    }
}
