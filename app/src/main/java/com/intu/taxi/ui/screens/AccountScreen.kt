package com.intu.taxi.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.PriceChange
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.AttachMoney
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
import androidx.compose.material3.AlertDialog
 
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.FirebaseException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FieldPath
import android.app.Activity
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseAuthUserCollisionException
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import com.intu.taxi.ui.debug.DebugLog
import com.intu.taxi.BuildConfig

data class SavedPlace(val type: String, val name: String, val lat: Double, val lon: Double, val label: String? = null, val icon: String? = null)

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
    var lastProfileNonBalance by remember { mutableStateOf<Map<String, Any>?>(null) }
    DisposableEffect(uid) {
        val db = FirebaseFirestore.getInstance()
        val reg = if (uid != null) db.collection("users").document(uid).addSnapshotListener { doc, _ ->
            loading = false
            val data = doc?.data
            val nonBalance = data?.filter { it.key != "balance" && it.key != "updatedAt" } ?: emptyMap()
            val prev = lastProfileNonBalance
            if (prev == null || nonBalance != prev) {
                profile = data
                lastProfileNonBalance = nonBalance
            }
        } else null
        onDispose { reg?.remove() }
    }


    LaunchedEffect(pendingPickedPlace) {
        val p = pendingPickedPlace
        val u = uid
        if (p != null && u != null) {
            val placeMap = mutableMapOf<String, Any>(
                "name" to p.name,
                "lat" to p.lat,
                "lon" to p.lon
            )
            if (!p.label.isNullOrBlank()) placeMap["label"] = p.label!!
            if (!p.icon.isNullOrBlank()) placeMap["icon"] = p.icon!!
            FirebaseFirestore.getInstance().collection("users").document(u)
                .update(FieldPath.of("savedPlaces", "places"), FieldValue.arrayUnion(placeMap))
            com.intu.taxi.ui.debug.DebugLog.log("Account(pending): lugar guardado -> ${p.name}")
        }
    }

    var showAddPlace by remember { mutableStateOf(false) }
    var savedPlaceQueued by remember { mutableStateOf<SavedPlace?>(null) }
    LaunchedEffect(savedPlaceQueued) {
        val p = savedPlaceQueued
        val u = uid
        if (p != null && u != null) {
            val placeMap = mutableMapOf<String, Any>(
                "name" to p.name,
                "lat" to p.lat,
                "lon" to p.lon
            )
            if (!p.label.isNullOrBlank()) placeMap["label"] = p.label!!
            if (!p.icon.isNullOrBlank()) placeMap["icon"] = p.icon!!
            FirebaseFirestore.getInstance().collection("users").document(u)
                .update(FieldPath.of("savedPlaces", "places"), FieldValue.arrayUnion(placeMap))
            DebugLog.log("Account: lugar guardado -> ${p.name}")
            savedPlaceQueued = null
        }
    }
    val name = listOf(
        profile?.get("firstName") as? String ?: "",
        profile?.get("lastName") as? String ?: ""
    ).joinToString(" ").trim().ifEmpty { stringResource(R.string.account_title_fallback) }
    val email = profile?.get("email") as? String ?: ""
    val authPhoneNow = FirebaseAuth.getInstance().currentUser?.phoneNumber ?: ""
    val phone = authPhoneNow.ifBlank { (profile?.get("fullNumber") as? String) ?: (profile?.get("number") as? String) ?: "" }
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
                    com.intu.taxi.ui.debug.DebugLog.log("Google link success uid=${uidSafe} email=${mail}")
                }?.addOnFailureListener { e ->
                    if (e is FirebaseAuthUserCollisionException) {
                        val prevUid = FirebaseAuth.getInstance().currentUser?.uid
                        FirebaseAuth.getInstance().signInWithCredential(credential).addOnSuccessListener { authRes ->
                            isGoogleLinked = true
                            linkStatus = context.getString(R.string.google_linked_success)
                            val uidSigned = authRes.user?.uid
                            val mail = account.email
                            if (uidSigned != null && mail != null) {
                                FirebaseFirestore.getInstance().collection("users").document(uidSigned)
                                    .set(mapOf("email" to mail), SetOptions.merge())
                            }
                            com.intu.taxi.ui.debug.DebugLog.log("Google collision resolved: signed in uid=${uidSigned} email=${mail}")
                            val oldUid = prevUid
                            if (oldUid != null && uidSigned != null && oldUid != uidSigned) {
                                FirebaseFirestore.getInstance().collection("users").document(oldUid).get().addOnSuccessListener { oldDoc ->
                                    val data = oldDoc.data
                                    if (data != null) {
                                        FirebaseFirestore.getInstance().collection("users").document(uidSigned)
                                            .set(data, SetOptions.merge())
                                        com.intu.taxi.ui.debug.DebugLog.log("Migrated Firestore data from ${oldUid} to ${uidSigned}")
                                    }
                                }
                            }
                            val existingPhone = phone
                            val phoneLinkedNow = FirebaseAuth.getInstance().currentUser?.phoneNumber?.isNotBlank() == true
                            if (!phoneLinkedNow && existingPhone.isNotBlank()) {
                                com.intu.taxi.ui.debug.DebugLog.log("Trigger phone link for ${existingPhone}")
                                onVerifyPhone(existingPhone)
                            }
                        }.addOnFailureListener { signErr ->
                            linkStatus = "Error: ${signErr.message}"
                            com.intu.taxi.ui.debug.DebugLog.log("Google collision sign-in failed: ${signErr.message}")
                        }
                    } else {
                        linkStatus = "Error: ${e.message}"
                        com.intu.taxi.ui.debug.DebugLog.log("Google link error: ${e.message}")
                    }
                }
            }
        }
    }

    LaunchedEffect(linkStatus) {
        if (linkStatus.isNotEmpty()) {
            delay(2000)
            linkStatus = ""
        }
    }

    val storedCountry = (profile?.get("country") as? String)?.lowercase()
    var country by remember {
        mutableStateOf(
            when (storedCountry) {
                "peru" -> "peru"
                "usa" -> "usa"
                else -> "peru"
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

    fun saveEmail(newEmail: String) {
        val u = uid ?: return
        FirebaseFirestore.getInstance().collection("users").document(u)
            .set(mapOf("email" to newEmail), SetOptions.merge())
        com.intu.taxi.ui.debug.DebugLog.log("Email saved in Firestore uid=${u} email=${newEmail}")
        FirebaseAuth.getInstance().currentUser?.updateEmail(newEmail)?.addOnSuccessListener {
            com.intu.taxi.ui.debug.DebugLog.log("FirebaseAuth email updated")
        }?.addOnFailureListener { e ->
            com.intu.taxi.ui.debug.DebugLog.log("FirebaseAuth email update failed: ${e.message}")
        }
    }

    var headerVisible by remember { mutableStateOf(false) }
    var contentVisible by remember { mutableStateOf(false) }
    LaunchedEffect(uid) {
        headerVisible = true
        delay(200)
        contentVisible = true
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
                AnimatedVisibility(visible = headerVisible, enter = fadeIn() + slideInVertically(initialOffsetY = { -it })) {
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
            }
            
            item {
                AnimatedVisibility(visible = contentVisible, enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 })) {
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
                        linkStatus = linkStatus,
                        onEditEmail = { saveEmail(it) },
                        paymentMethod = paymentMethod,
                        onChangePayment = { savePayment(it) },
                        profile = profile,
                        onAddPlace = {
                            DebugLog.log("Account: botón Agregar pulsado")
                            showAddPlace = true
                        },
                        country = country,
                        onSelectCountry = { saveCountry(it) },
                        onOpenDebug = onDebugClick,
                        onStartDriver = onStartDriver
                    )
                }
            }
            if (((profile?.get("driverApproved") as? Boolean) == true) && ((profile?.get("driverMode") as? Boolean) == true)) {
                val driverData = profile?.get("driver") as? Map<String, Any> ?: emptyMap()
                val vehiclePhoto = (driverData["vehiclePhotoUrl"] as? String) ?: ""
                val vehicleType = (driverData["vehicleType"] as? String) ?: ""
                val vehicleBrand = (driverData["vehicleBrand"] as? String) ?: (driverData["vehicleMake"] as? String ?: "")
                val vehicleModel = (driverData["vehicleModel"] as? String) ?: ""
                val vehicleYear = (driverData["vehicleYear"] as? String) ?: ""
                val vehiclePlate = (driverData["vehiclePlate"] as? String) ?: ""
                item { AnimatedVisibility(visible = contentVisible, enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 })) { DriverBalanceCardLive(uid = uid, onRecharge = onStartTopUp) } }
                item { AnimatedVisibility(visible = contentVisible, enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 })) { VehicleCard(photoUrl = vehiclePhoto, type = vehicleType, brand = vehicleBrand, model = vehicleModel, year = vehicleYear, plate = vehiclePlate) } }
                item { AnimatedVisibility(visible = contentVisible, enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 })) { DriverStatsCard() } }
                item { AnimatedVisibility(visible = contentVisible, enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 })) { DriverRecentTripsCard() } }
            }
            
            
            
            
            item { AnimatedVisibility(visible = contentVisible, enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 })) { LogoutCard(onLogout = onLogout) } }
        }
    }
    if (showAddPlace) {
        AddPlaceScreen(defaultType = "other") { place ->
            DebugLog.log("AddPlaceScreen: seleccionado ${place.name}")
            savedPlaceQueued = place
            showAddPlace = false
        }
    }
    // Topups: ahora se procesan en el server via Cloud Functions (processTopup)
}

@Composable
private fun ClientHeader(title: String, photoUrl: String, onChangePhoto: () -> Unit, showDriverToggle: Boolean = false, driverMode: Boolean = false, onToggleDriver: (Boolean) -> Unit = {}) {
    val gradient = Brush.horizontalGradient(listOf(Color(0xFF0D9488), Color(0xFF0F172A)))
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.14f)),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f))
    ) {
        Box(modifier = Modifier.fillMaxWidth().background(gradient)) {
            if (showDriverToggle) {
                Row(modifier = Modifier.align(Alignment.TopEnd).padding(top = 12.dp, end = 12.dp)) {
                    RolePill(text = if (driverMode) "Conductor" else "Pasajero", onClick = { onToggleDriver(!driverMode) })
                }
            }
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .clip(CircleShape)
                        .background(Color.Transparent)
                        .border(3.dp, Color.White.copy(alpha = 0.9f), CircleShape)
                        .clickable { onChangePhoto() },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(74.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Color(0xFF0D9488), Color(0xFF22D3EE))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (photoUrl.isNotBlank()) {
                            AsyncImage(model = photoUrl, contentDescription = null, modifier = Modifier.size(74.dp).clip(CircleShape))
                        } else {
                            Icon(Icons.Filled.Person, contentDescription = null, tint = Color.White, modifier = Modifier.size(36.dp))
                        }
                    }
                }
                Text(
                    title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                )
            }
        }
    }
}

@Composable
private fun ModeToggle(checked: Boolean, onToggle: (Boolean) -> Unit) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(50.dp)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(Color.White.copy(alpha = 0.18f))
            .border(1.dp, Color.White.copy(alpha = 0.35f), shape)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val driverSelected = checked
        val passengerSelected = !checked
        Box(
            modifier = Modifier
                .clip(shape)
                .background(if (driverSelected) Color.White.copy(alpha = 0.22f) else Color.Transparent)
                .clickable { onToggle(true) }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = if (driverSelected) Color(0xFF10B981) else Color.White)
                Text(text = "Conductor", color = if (driverSelected) Color.White else Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
            }
        }
        Box(
            modifier = Modifier
                .clip(shape)
                .background(if (passengerSelected) Color.White.copy(alpha = 0.22f) else Color.Transparent)
                .clickable { onToggle(false) }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Filled.Person, contentDescription = null, tint = if (passengerSelected) Color(0xFF3B82F6) else Color.White)
                Text(text = "Pasajero", color = if (passengerSelected) Color.White else Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun RolePill(text: String, onClick: (() -> Unit)? = null) {
    val shape = androidx.compose.foundation.shape.RoundedCornerShape(50.dp)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(Color.White.copy(alpha = 0.22f))
            .border(1.dp, Color.White.copy(alpha = 0.35f), shape)
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .let { if (onClick != null) it.clickable { onClick() } else it },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, color = Color.White, style = MaterialTheme.typography.bodySmall)
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
    linkStatus: String,
    onEditEmail: (String) -> Unit,
    paymentMethod: String,
    onChangePayment: (String) -> Unit,
    profile: Map<String, Any>?,
    onAddPlace: () -> Unit,
    country: String,
    onSelectCountry: (String) -> Unit,
    onOpenDebug: () -> Unit,
    onStartDriver: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.98f)),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        var paymentExpanded by remember { mutableStateOf(false) }
        var placesExpanded by remember { mutableStateOf(false) }
        var countryExpanded by remember { mutableStateOf(false) }
        var languageExpanded by remember { mutableStateOf(false) }
        var helpPrivacyExpanded by remember { mutableStateOf(false) }
        var showProfilePhotoDialog by remember { mutableStateOf(false) }
        var showDeletePlaceDialog by remember { mutableStateOf(false) }
        var placeToDelete by remember { mutableStateOf<Map<String, Any>?>(null) }
        val hasProfilePhoto = ((profile?.get("photoUrl") as? String)?.isNotBlank() == true)
        var emailExpanded by remember { mutableStateOf(false) }
        var emailInput by remember { mutableStateOf(email) }
        Column(Modifier.fillMaxWidth().padding(8.dp)) {
            ListItem(
                modifier = Modifier.let { if (!isGoogleLinked) it.clickable { emailExpanded = true } else it },
                leadingContent = {
                    Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFE6F2FF)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Email, contentDescription = null, tint = Color(0xFF0F172A))
                    }
                },
                headlineContent = { Text(stringResource(R.string.email_label)) },
                supportingContent = { Text(email.ifEmpty { stringResource(R.string.no_email) }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val verified = email.isNotEmpty()
                        Text(if (verified) stringResource(R.string.verified) else stringResource(R.string.add_label), color = if (verified) Color(0xFF10B981) else Color.Gray, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.size(4.dp))
                        Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
                    }
                }
            )
            if (emailExpanded && !isGoogleLinked) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = emailInput, onValueChange = { emailInput = it.trim() }, label = { Text(stringResource(R.string.email_label)) }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = {
                            onEditEmail(emailInput)
                            emailExpanded = false
                        }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488), contentColor = Color.White)) { Text(stringResource(R.string.save)) }
                        OutlinedButton(onClick = {
                            emailExpanded = false
                            emailInput = email
                        }) { Text(stringResource(R.string.cancel)) }
                    }
                }
            }
            HorizontalDivider()
            ListItem(
                modifier = Modifier.clickable { if (!isPhoneLinked && phone.isNotEmpty()) onVerifyPhone(phone) },
                leadingContent = {
                    Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFE7FFF8)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Phone, contentDescription = null, tint = Color(0xFF0D9488))
                    }
                },
                headlineContent = { Text(stringResource(R.string.phone_label)) },
                supportingContent = { Text(phone.ifEmpty { stringResource(R.string.no_phone) }, color = Color.Gray) },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (isPhoneLinked) stringResource(R.string.verified) else stringResource(R.string.verify), color = if (isPhoneLinked) Color(0xFF10B981) else Color.Gray, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.size(4.dp))
                        Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
                    }
                }
            )
            HorizontalDivider()
            ListItem(
                modifier = Modifier.clickable { if (!isGoogleLinked) onLinkGoogle() },
                leadingContent = {
                    Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFEFF0FF)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Link, contentDescription = null, tint = Color(0xFF4F46E5))
                    }
                },
                headlineContent = { Text(stringResource(R.string.google_account_label)) },
                supportingContent = { Text(if (isGoogleLinked) stringResource(R.string.linked) else stringResource(R.string.not_linked), color = Color.Gray) },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(if (isGoogleLinked) stringResource(R.string.linked) else stringResource(R.string.link_action), color = if (isGoogleLinked) Color(0xFF10B981) else Color(0xFF0F172A), style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.size(4.dp))
                        Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
                    }
                }
            )
            HorizontalDivider()
            ListItem(
                modifier = Modifier.clickable { paymentExpanded = !paymentExpanded },
                leadingContent = {
                    Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFE6F2FF)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.CreditCard, contentDescription = null, tint = Color(0xFF0F172A))
                    }
                },
                headlineContent = { Text(stringResource(R.string.payment_label)) },
                supportingContent = {
                    Text(
                        when (paymentMethod) {
                            "yape_plin" -> stringResource(R.string.method_selected_yape_plin)
                            else -> stringResource(R.string.method_selected_cash)
                        },
                        color = Color.Gray
                    )
                },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.see), color = Color(0xFF10B981), style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.size(4.dp))
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = Color.Gray)
                    }
                }
            )
            if (paymentExpanded) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PaymentPill(
                            label = stringResource(R.string.method_yape_plin),
                            selected = paymentMethod == "yape_plin",
                            accent = Color(0xFF0D9488),
                            leading = {
                                YapePlinBubbleIcon(selected = paymentMethod == "yape_plin")
                            }
                        ) { onChangePayment("yape_plin") }
                        PaymentPill(
                            label = stringResource(R.string.method_cash),
                            selected = paymentMethod == "efectivo",
                            accent = Color(0xFF0F172A),
                            leading = {
                                Icon(
                                    Icons.Filled.AttachMoney,
                                    contentDescription = null,
                                    tint = if (paymentMethod == "efectivo") Color.White else Color(0xFF0F172A)
                                )
                            }
                        ) { onChangePayment("efectivo") }
                    }
                }
            }
            HorizontalDivider()
            val sp = profile?.get("savedPlaces") as? Map<String, Any> ?: emptyMap()
            val places = (sp["places"] as? List<*>) ?: emptyList<Any>()
            ListItem(
                modifier = Modifier.clickable { placesExpanded = !placesExpanded },
                leadingContent = {
                    Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFE7F3EE)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFF0D9488))
                    }
                },
                headlineContent = { Text(stringResource(R.string.saved_places_label)) },
                supportingContent = { Text(if (places.isEmpty()) "No hay lugares guardados" else "${places.size} guardados", color = Color.Gray) },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.see), color = Color(0xFF10B981), style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.size(4.dp))
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = Color.Gray)
                    }
                }
            )
            if (placesExpanded) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (places.isEmpty()) {
                        Text(stringResource(R.string.no_saved_places), style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                    } else {
                        places.forEach { any ->
                            val m = any as? Map<String, Any> ?: emptyMap()
                            val name = (m["label"] as? String)?.takeIf { it.isNotBlank() } ?: (m["name"] as? String ?: "")
                            val iconStr = (m["icon"] as? String) ?: "marker"
                            val leading = when (iconStr) {
                                "home" -> Icons.Filled.Home
                                "work" -> Icons.Filled.Work
                                "school" -> Icons.Filled.School
                                "shopping" -> Icons.Filled.LocalMall
                                "food" -> Icons.Filled.Restaurant
                                "cafe" -> Icons.Filled.LocalCafe
                                "hospital" -> Icons.Filled.LocalHospital
                                "package" -> Icons.Filled.LocalShipping
                                "star" -> Icons.Filled.Star
                                "favorite" -> Icons.Filled.Favorite
                                else -> Icons.Filled.DirectionsCar
                            }
                            ListItem(
                                headlineContent = { Text(name) },
                                leadingContent = { Icon(leading, contentDescription = null, tint = Color(0xFF0D9488)) },
                                trailingContent = {
                                    OutlinedButton(onClick = {
                                        placeToDelete = m
                                        showDeletePlaceDialog = true
                                    }) { Text(stringResource(R.string.delete)) }
                                }
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onAddPlace) { Text(stringResource(R.string.add)) }
                    }
                    if (showDeletePlaceDialog) {
                        AlertDialog(
                            onDismissRequest = {
                                showDeletePlaceDialog = false
                                placeToDelete = null
                            },
                            title = { Text("Eliminar lugar") },
                            text = { Text("¿Deseas eliminar este lugar guardado?") },
                            confirmButton = {
                                Button(onClick = {
                                    val uid = FirebaseAuth.getInstance().currentUser?.uid
                                    val m = placeToDelete
                                    if (uid != null && m != null) {
                                        FirebaseFirestore.getInstance().collection("users").document(uid)
                                            .update(FieldPath.of("savedPlaces", "places"), FieldValue.arrayRemove(m))
                                        DebugLog.log("Account: lugar eliminado -> ${m["name"]}")
                                    }
                                    showDeletePlaceDialog = false
                                    placeToDelete = null
                                }) { Text("Eliminar") }
                            },
                            dismissButton = {
                                OutlinedButton(onClick = {
                                    showDeletePlaceDialog = false
                                    placeToDelete = null
                                }) { Text("Cancelar") }
                            }
                        )
                    }
                }
            }
            HorizontalDivider()
            ListItem(
                modifier = Modifier.clickable { countryExpanded = !countryExpanded },
                leadingContent = {
                    Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFE6F2FF)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Work, contentDescription = null, tint = Color(0xFF0F172A))
                    }
                },
                headlineContent = { Text(stringResource(R.string.country_label)) },
                supportingContent = { Text(if (country == "peru") stringResource(R.string.country_peru) else stringResource(R.string.country_usa), color = Color.Gray) },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.see), color = Color(0xFF10B981), style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.size(4.dp))
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = Color.Gray)
                    }
                }
            )
            if (countryExpanded) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PaymentPill(label = stringResource(R.string.country_peru), selected = country == "peru", accent = Color(0xFF0D9488)) { onSelectCountry("peru") }
                    PaymentPill(label = stringResource(R.string.country_usa), selected = country == "usa", accent = Color(0xFF0F172A), enabled = false) { }
                }
            }
            HorizontalDivider()
            val ctx = LocalContext.current
            ListItem(
                modifier = Modifier.clickable { languageExpanded = !languageExpanded },
                leadingContent = {
                    Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFEFF0FF)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFF4F46E5))
                    }
                },
                headlineContent = { Text(stringResource(R.string.language_label)) },
                supportingContent = { Text(stringResource(R.string.language_label), color = Color.Gray) },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.see), color = Color(0xFF10B981), style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.size(4.dp))
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = Color.Gray)
                    }
                }
            )
            if (languageExpanded) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PaymentPill(label = stringResource(R.string.language_es), selected = true, accent = Color(0xFF0D9488)) {
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("es"))
                        ctx.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putBoolean("lang_user_set", true).apply()
                        (ctx as? Activity)?.recreate()
                    }
                    PaymentPill(label = stringResource(R.string.language_en), selected = false, accent = Color(0xFF0F172A)) {
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
                        ctx.getSharedPreferences("settings", Context.MODE_PRIVATE).edit().putBoolean("lang_user_set", true).apply()
                        (ctx as? Activity)?.recreate()
                    }
                }
            }
            HorizontalDivider()
            val approved = ((profile?.get("driverApproved") as? Boolean) == true)
            ListItem(
                modifier = Modifier.clickable {
                    if (!approved) {
                        if (!hasProfilePhoto) showProfilePhotoDialog = true else onStartDriver()
                    }
                },
                leadingContent = {
                    Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFE7FFF8)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = Color(0xFF0D9488))
                    }
                },
                headlineContent = { Text(stringResource(R.string.drive_and_earn_section)) },
                supportingContent = { if (!approved) Text(stringResource(R.string.driver_complete_data), color = Color.Gray) },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (!approved) {
                            Text(stringResource(R.string.earn_action), color = Color(0xFF10B981), style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.size(4.dp))
                            Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
                        } else {
                            Text(stringResource(R.string.linked), color = Color(0xFF10B981), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            )
            HorizontalDivider()
            ListItem(
                modifier = Modifier.clickable { helpPrivacyExpanded = !helpPrivacyExpanded },
                leadingContent = {
                    Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFFDF2E9)), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFF0F172A))
                    }
                },
                headlineContent = { Text(stringResource(R.string.support_privacy_label)) },
                supportingContent = { Text(stringResource(R.string.support_privacy_desc), color = Color.Gray) },
                trailingContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.see), color = Color(0xFF10B981), style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.size(4.dp))
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, tint = Color.Gray)
                    }
                }
            )
            if (helpPrivacyExpanded) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { }) { Text(stringResource(R.string.help_center)) }
                    OutlinedButton(onClick = { }) { Text(stringResource(R.string.privacy)) }
                    OutlinedButton(onClick = onOpenDebug) { Text(stringResource(R.string.debug)) }
                }
            }
            if (showProfilePhotoDialog) {
                AlertDialog(
                    onDismissRequest = { showProfilePhotoDialog = false },
                    title = { Text(stringResource(R.string.profile_photo_required_title)) },
                    text = { Text(stringResource(R.string.profile_photo_required_message)) },
                    confirmButton = {
                        Button(onClick = { showProfilePhotoDialog = false }) { Text(stringResource(R.string.ok)) }
                    }
                )
            }
            
            if (linkStatus.isNotEmpty()) Text(linkStatus, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp))
            Text("Versión app: " + BuildConfig.APP_VERSION_TAG, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(8.dp))
        }
    }
}

@Composable
private fun PaymentPill(label: String, selected: Boolean, accent: Color, leading: (@Composable () -> Unit)? = null, enabled: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    val bg = if (selected) Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0.85f))) else Brush.horizontalGradient(listOf(Color.White, Color.White))
    val borderColor = if (selected) Color.Transparent else Color(0xFFE5E7EB)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(bg)
            .border(1.dp, borderColor, shape)
            .let { if (enabled) it.clickable { onClick() } else it }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (leading != null) leading()
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium.copy(color = if (!enabled) Color(0xFF9CA3AF) else if (selected) Color.White else Color(0xFF374151), fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium)
        )
    }
}

@Composable
private fun YapePlinBubbleIcon(selected: Boolean) {
    val bubbleColor = Color(0xFF18D6C7)
    Box(
        modifier = Modifier
            .size(22.dp)
            .drawBehind {
                val tail = Path().apply {
                    moveTo(0f, size.height * 0.55f)
                    lineTo(0f, size.height)
                    lineTo(size.width * 0.25f, size.height * 0.8f)
                    close()
                }
                drawPath(tail, bubbleColor)
            }
            .clip(CircleShape)
            .background(bubbleColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "S/",
            style = MaterialTheme.typography.labelSmall.copy(
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        )
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
    onSetWork: () -> Unit,
    onAddPlace: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.98f)),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.saved_places_label), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                OutlinedButton(onClick = onAddPlace) { Text("Agregar") }
            }
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            val sp = profile?.get("savedPlaces") as? Map<String, Any> ?: emptyMap()
            val places = (sp["places"] as? List<*>) ?: emptyList<Any>()
            if (places.isEmpty()) {
                Text("No hay lugares guardados", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            } else {
                places.forEach { any ->
                    val m = any as? Map<String, Any> ?: emptyMap()
                    val name = (m["label"] as? String)?.takeIf { it.isNotBlank() } ?: (m["name"] as? String ?: "")
                    val iconStr = (m["icon"] as? String) ?: "marker"
                    val leading = when (iconStr) {
                        "home" -> Icons.Filled.Home
                        "work" -> Icons.Filled.Work
                        "school" -> Icons.Filled.School
                        "shopping" -> Icons.Filled.LocalMall
                        "food" -> Icons.Filled.Restaurant
                        "cafe" -> Icons.Filled.LocalCafe
                        "hospital" -> Icons.Filled.LocalHospital
                        "package" -> Icons.Filled.LocalShipping
                        "star" -> Icons.Filled.Star
                        "favorite" -> Icons.Filled.Favorite
                        else -> Icons.Filled.DirectionsCar
                    }
                    ListItem(
                        headlineContent = { Text(name) },
                        leadingContent = { Icon(leading, contentDescription = null, tint = Color(0xFF0D9488)) }
                    )
                }
            }
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
private fun DriverBalanceCardLive(uid: String?, onRecharge: () -> Unit) {
    var balance by remember { mutableStateOf(0.0) }
    DisposableEffect(uid) {
        val db = FirebaseFirestore.getInstance()
        val reg = if (uid != null) db.collection("users").document(uid).addSnapshotListener { doc, _ ->
            val b = (doc?.get("balance") as? Number)?.toDouble() ?: 0.0
            balance = b
        } else null
        onDispose { reg?.remove() }
    }
    DriverBalanceCard(balance = balance, onRecharge = onRecharge)
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
                            val current = auth.currentUser
                            val hasPhone = current?.phoneNumber?.isNotBlank() == true
                            val task = if (hasPhone) current?.updatePhoneNumber(credential) else current?.linkWithCredential(credential)
                            task?.addOnSuccessListener {
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
                                com.intu.taxi.ui.debug.DebugLog.log("Phone link success uid=${uidSafe} full=${full}")
                            }
                        }
                        override fun onVerificationFailed(e: FirebaseException) { status = "Error: ${e.message ?: "verificación"}"; com.intu.taxi.ui.debug.DebugLog.log("Phone verify error: ${e.message}") }
                        override fun onCodeSent(vid: String, token: PhoneAuthProvider.ForceResendingToken) { verificationId = vid; status = context.getString(R.string.code_sent); com.intu.taxi.ui.debug.DebugLog.log("SMS code sent to ${full}") }
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
                        val current = auth.currentUser
                        val hasPhone = current?.phoneNumber?.isNotBlank() == true
                        val task = if (hasPhone) current?.updatePhoneNumber(cred) else current?.linkWithCredential(cred)
                        task?.addOnSuccessListener {
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
                            com.intu.taxi.ui.debug.DebugLog.log("Phone link success (code) uid=${uidSafe} full=${full}")
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

@Composable
private fun LogoutCard(onLogout: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.98f)), shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Button(onClick = onLogout, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A), contentColor = Color.White)) {
                Text(stringResource(R.string.logout))
            }
        }
    }
}
