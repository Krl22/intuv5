package com.intu.taxi.ui.screens

import android.annotation.SuppressLint
import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.intu.taxi.ui.debug.DebugLog
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.CameraOptions
import com.mapbox.geojson.Point
import com.mapbox.geojson.Feature
import com.mapbox.geojson.LineString
import androidx.compose.runtime.DisposableEffect
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.maps.plugin.locationcomponent.location
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.scale
import kotlin.math.max
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.navigationBarsPadding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.net.HttpURLConnection
import org.json.JSONObject
import org.json.JSONArray
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloatAsState
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.Query
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.layers.generated.circleLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.layers.addLayer
import kotlin.random.Random
import com.google.firebase.ktx.Firebase
import com.google.firebase.functions.ktx.functions
import coil.compose.AsyncImage
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings

@SuppressLint("MissingPermission")
@Composable
fun DriverHomeScreen() {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }

    DisposableEffect(Unit) {
        mapView.onStart()
        onDispose {
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    mapView.scalebar.enabled = false

    val grantedFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val grantedCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val ok = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true || perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (ok) {
            mapView.location.updateSettings { enabled = true }
            DebugLog.log("Permiso ubicación concedido, location puck habilitado")
        } else {
            DebugLog.log("Permiso ubicación denegado")
        }
    }

    if (!grantedFine && !grantedCoarse) {
        SideEffect {
            locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    } else {
        mapView.location.updateSettings { enabled = true }
    }

    var headerVisible by remember { mutableStateOf(false) }
    var firstName by remember { mutableStateOf("") }
    var driverVehicleType by remember { mutableStateOf("") }
    var availabilityKey by remember { mutableStateOf<String?>(null) }
    var lastDriverLocation by remember { mutableStateOf<Point?>(null) }
    var isSearching by remember { mutableStateOf(false) }
    var rideRequests by remember { mutableStateOf<List<RideRequestItem>>(emptyList()) }
    var currentRideId by remember { mutableStateOf<String?>(null) }
    var clientLiveLocation by remember { mutableStateOf<Point?>(null) }
    var isCurrentRide by remember { mutableStateOf(false) }
    var isArrived by remember { mutableStateOf(false) }
    var isInProgress by remember { mutableStateOf(false) }
    var showCodeDialog by remember { mutableStateOf(false) }
    var codeInput by remember { mutableStateOf("") }
    var clientName by remember { mutableStateOf<String?>(null) }
    var clientPhone by remember { mutableStateOf<String?>(null) }
    var clientPhotoUrl by remember { mutableStateOf<String?>(null) }
    var currentRidePaymentMethod by remember { mutableStateOf<String?>(null) }
    var currentRidePrice by remember { mutableStateOf<Double?>(null) }
    var currentRideRideType by remember { mutableStateOf<String?>(null) }
    var currentRideOriginLabel by remember { mutableStateOf<String?>(null) }
    var currentRideDestLabel by remember { mutableStateOf<String?>(null) }
    var currentRideDestPoint by remember { mutableStateOf<Point?>(null) }
    var showChat by remember { mutableStateOf(false) }
    var currentRideUserId by remember { mutableStateOf<String?>(null) }
    var showRatingDialog by remember { mutableStateOf(false) }
    var lastCompletedRideId by remember { mutableStateOf<String?>(null) }
    var lastCompletedTargetUserId by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { mapView }
        )

        
        LaunchedEffect(Unit) {
            headerVisible = true
            val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                FirebaseFirestore.getInstance().collection("users").document(uid).get().addOnSuccessListener { doc ->
                    firstName = doc.getString("firstName") ?: ""
                    val driverData = doc.get("driver") as? Map<String, Any> ?: emptyMap()
                    driverVehicleType = (driverData["vehicleType"] as? String) ?: ""
                }
            }
        }

        val headerShiftFraction = 0f
        val buttonTravelFraction by animateFloatAsState(targetValue = if (isSearching) 0.95f else 0f, animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing), label = "buttonTravelFraction")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .padding(top = 0.dp)
                .then(
                    if (!isSearching && !isCurrentRide) Modifier.drawBehind {
                        val teal = Color(0xFF08817E)
                        val indigo = Color(0xFF1E1F47)
                        val shiftY = size.height * headerShiftFraction
                        withTransform({ translate(left = 0f, top = -shiftY) }) {
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
                                        radius = max(size.width, size.height)
                                    ),
                                    size = Size(width = size.width, height = size.height),
                                    blendMode = BlendMode.DstIn
                                )
                            }
                        }
                    } else Modifier
                ),
            contentAlignment = Alignment.TopCenter
        ) {
            AnimatedVisibility(
                visible = headerVisible && !isSearching && !isCurrentRide,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it })
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 45.dp, start = 16.dp, end = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "intu",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    )
                    Spacer(modifier = Modifier.height(30.dp))
                    Text(
                        text = if (firstName.isNotBlank()) "¡Hola $firstName!" else "¡Hola!",
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        fontSize = 24.sp
                    )
                }
            }
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val headerHeight = maxHeight * 0.5f
            val initialY = headerHeight * 0.35f
            val finalY = maxHeight - 172.dp - 56.dp
            val animatedY = lerp(initialY, finalY, buttonTravelFraction)

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = animatedY)
            ) {
                if (!isCurrentRide) {
                    AnimatedGradientButton(
                        isSearching = isSearching,
                        onClick = {
                            val uidNow = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                            if (!isSearching) {
                                if (uidNow != null) {
                                    val loc = lastDriverLocation
                                    val data = mapOf(
                                        "driverId" to uidNow,
                                        "status" to "available",
                                        "rideType" to driverVehicleType.ifBlank { "Intu Honda" },
                                        "lat" to (loc?.latitude() ?: 0.0),
                                        "lon" to (loc?.longitude() ?: 0.0),
                                        "updatedAt" to ServerValue.TIMESTAMP
                                    )
                                    FirebaseDatabase.getInstance().reference.child("driverAvailability").child(uidNow)
                                        .setValue(data)
                                        .addOnSuccessListener {
                                            availabilityKey = uidNow
                                            isSearching = true
                                            com.intu.taxi.ui.debug.DebugLog.log("Driver availability creado uid=${uidNow}")
                                        }
                                        .addOnFailureListener { e ->
                                            com.intu.taxi.ui.debug.DebugLog.log("Error creando availability: ${e.message}")
                                        }
                                }
                            } else {
                                val key = availabilityKey ?: uidNow
                                if (key != null) {
                                    FirebaseDatabase.getInstance().reference.child("driverAvailability").child(key)
                                        .removeValue()
                                        .addOnSuccessListener {
                                            com.intu.taxi.ui.debug.DebugLog.log("Driver availability eliminado uid=${key}")
                                        }
                                        .addOnFailureListener { e ->
                                            com.intu.taxi.ui.debug.DebugLog.log("Error eliminando availability: ${e.message}")
                                        }
                                }
                                availabilityKey = null
                                isSearching = false
                            }
                        }
                    )
                }
            }

            AnimatedVisibility(
                visible = isSearching,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it })
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 12.dp, start = 16.dp, end = 16.dp)
                ) {
                    val list = rideRequests
                    if (list.isEmpty()) {
                        androidx.compose.material3.Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f)),
                            elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 10.dp),
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text("Solicitudes disponibles", style = MaterialTheme.typography.titleMedium, color = Color(0xFF1C1C1E))
                                Spacer(Modifier.height(8.dp))
                                Text("Sin solicitudes por ahora", style = MaterialTheme.typography.bodySmall, color = Color(0xFF6E6E73))
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().height(300.dp)
                        ) {
                            items(list.size) { idx ->
                                val r = list[idx]
                                RideRequestFancyCard(
                                    item = r,
                                    onAccept = {
                                        com.google.firebase.ktx.Firebase.functions(context.getString(com.intu.taxi.R.string.functions_region))
                                            .getHttpsCallable("acceptRide")
                                            .call(mapOf("rideRequestId" to r.id))
                                            .addOnSuccessListener {
                                                currentRideId = r.id
                                                isSearching = false
                                                com.intu.taxi.ui.debug.DebugLog.log("AcceptRide OK id=${r.id}")
                                            }
                                            .addOnFailureListener { e ->
                                                com.intu.taxi.ui.debug.DebugLog.log("AcceptRide error: ${e.message}")
                                            }
                                    }
                                )
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
            if (isCurrentRide) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .padding(bottom = 16.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    androidx.compose.material3.Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.98f)),
                        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 14.dp),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val photo = clientPhotoUrl
                                if (!photo.isNullOrBlank()) {
                                    AsyncImage(
                                        model = photo,
                                        contentDescription = null,
                                        modifier = Modifier.size(56.dp).background(Color(0xFFE5E7EB), CircleShape).clip(CircleShape)
                                    )
                                } else {
                                    Box(modifier = Modifier.size(56.dp).background(Color(0xFFE5E7EB), CircleShape)) {}
                                }
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    val nm = clientName ?: "Cliente"
                                    val rt = currentRideRideType ?: driverVehicleType.ifBlank { "Intu Honda" }
                                    Text(nm, style = MaterialTheme.typography.titleMedium, color = Color(0xFF111827))
                                    Text(rt, style = MaterialTheme.typography.bodySmall, color = Color(0xFF6E6E73))
                                }
                                val statusStr = if (isArrived) "Llegado" else "En curso"
                                InfoChip(text = statusStr, bg = Color(0xFFE6F2FF), fg = Color(0xFF0F172A))
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(10.dp).background(Color(0xFF10B981), CircleShape)) {}
                                        val oLbl = currentRideOriginLabel ?: "Origen: —"
                                        Text(oLbl, style = MaterialTheme.typography.bodySmall, color = Color(0xFF111827))
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Box(modifier = Modifier.size(10.dp).background(Color(0xFFEF4444), CircleShape)) {}
                                        val dLbl = currentRideDestLabel ?: "Destino: —"
                                        Text(dLbl, style = MaterialTheme.typography.bodySmall, color = Color(0xFF111827))
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val pm = currentRidePaymentMethod ?: "—"
                                val prStr = currentRidePrice?.let { String.format("$%.2f", it) } ?: "—"
                                InfoChip(text = "Pago: $pm")
                                InfoChip(text = "Precio: $prStr")
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                androidx.compose.material3.OutlinedButton(onClick = { showChat = true }, modifier = Modifier.weight(1f)) {
                                    androidx.compose.material3.Text("Chat")
                                }
                                androidx.compose.material3.IconButton(onClick = {}) {
                                    androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Filled.Phone, contentDescription = null)
                                }
                                androidx.compose.material3.IconButton(onClick = {}) {
                                    androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Filled.Settings, contentDescription = null)
                                }
                            }
                            if (!isArrived && !isInProgress) {
                                androidx.compose.foundation.layout.Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                                ) {
                                    androidx.compose.material3.Button(
                                        onClick = {
                                            val rid = currentRideId
                                            if (rid != null) {
                                                com.google.firebase.ktx.Firebase.functions(context.getString(com.intu.taxi.R.string.functions_region))
                                                    .getHttpsCallable("driverArrived")
                                                    .call(mapOf("currentRideId" to rid))
                                                    .addOnSuccessListener { isArrived = true; com.intu.taxi.ui.debug.DebugLog.log("DriverArrived OK") }
                                                    .addOnFailureListener { e -> com.intu.taxi.ui.debug.DebugLog.log("DriverArrived error: ${e.message}") }
                                                showCodeDialog = true
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp),
                                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488), contentColor = Color.White),
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                                    ) {
                                        androidx.compose.material3.Text(text = "Llegué al cliente", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    }
                                    androidx.compose.material3.Button(
                                        onClick = {
                                            val rid = currentRideId
                                            if (rid != null) {
                                                com.google.firebase.ktx.Firebase.functions(context.getString(com.intu.taxi.R.string.functions_region))
                                                    .getHttpsCallable("cancelRide")
                                                    .call(mapOf("currentRideId" to rid))
                                                    .addOnSuccessListener {
                                                        isCurrentRide = false
                                                        currentRideId = null
                                                        isArrived = false
                                                        clientLiveLocation = null
                                                        showCodeDialog = false
                                                        mapView.mapboxMap.getStyle { style ->
                                                            try { style.removeStyleLayer("driver-route-layer") } catch (_: Exception) {}
                                                            try { style.removeStyleSource("driver-route-src") } catch (_: Exception) {}
                                                            try { style.removeStyleLayer("client-layer") } catch (_: Exception) {}
                                                            try { style.removeStyleSource("client-src") } catch (_: Exception) {}
                                                        }
                                                    }
                                                    .addOnFailureListener { e -> com.intu.taxi.ui.debug.DebugLog.log("CancelRide error: ${e.message}") }
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp),
                                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F), contentColor = Color.White),
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                                    ) {
                                        androidx.compose.material3.Text(text = "Cancelar viaje", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                if (isInProgress) {
                                    androidx.compose.foundation.layout.Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                                    ) {
                                        androidx.compose.material3.Button(
                                            onClick = {
                                                val rid = currentRideId
                                                val targetUserIdSnapshot = currentRideUserId
                                                if (rid != null) {
                                                    com.google.firebase.ktx.Firebase.functions(context.getString(com.intu.taxi.R.string.functions_region))
                                                        .getHttpsCallable("completeRide")
                                                        .call(mapOf("currentRideId" to rid, "finalPrice" to (currentRidePrice ?: 0.0)))
                                                        .addOnSuccessListener { res ->
                                                            com.intu.taxi.ui.debug.DebugLog.log("CompleteRide OK")
                                                            val fs = FirebaseFirestore.getInstance()
                                                            val driverUidNow = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                                                            if (!driverUidNow.isNullOrBlank()) {
                                                                fs.collection("users").document(driverUidNow!!).collection("services").limit(1).get()
                                                                    .addOnSuccessListener { qs -> com.intu.taxi.ui.debug.DebugLog.log("FS: driver services encontrados=" + qs.size()) }
                                                                    .addOnFailureListener { e -> com.intu.taxi.ui.debug.DebugLog.log("FS: error leyendo services driver: ${e.message}") }
                                                            }
                                                            val dataMap = (res.data as? Map<*, *>)
                                                            val userIdFromResult = (dataMap?.get("userId") as? String)
                                                            val uidUser = userIdFromResult ?: targetUserIdSnapshot
                                                            if (!uidUser.isNullOrBlank()) {
                                                                fs.collection("users").document(uidUser!!).collection("trips").limit(1).get()
                                                                    .addOnSuccessListener { qs -> com.intu.taxi.ui.debug.DebugLog.log("FS: user trips encontrados=" + qs.size()) }
                                                                    .addOnFailureListener { e -> com.intu.taxi.ui.debug.DebugLog.log("FS: error leyendo trips user: ${e.message}") }
                                                                lastCompletedRideId = rid
                                                                lastCompletedTargetUserId = uidUser
                                                                com.intu.taxi.ui.debug.DebugLog.log("RATING-DRIVER: abrir dialog rid=${rid} target=${uidUser}")
                                                                showRatingDialog = true
                                                            } else {
                                                                com.intu.taxi.ui.debug.DebugLog.log("RATING-DRIVER: userId vacío, no se puede abrir dialog")
                                                            }
                                                            isCurrentRide = false
                                                            currentRideId = null
                                                            isArrived = false
                                                            isInProgress = false
                                                            clientLiveLocation = null
                                                            showCodeDialog = false
                                                            com.intu.taxi.ui.debug.DebugLog.log("RATING-DRIVER: estados reseteados tras complete")
                                                            mapView.mapboxMap.getStyle { style ->
                                                                try { style.removeStyleLayer("driver-route-layer") } catch (_: Exception) {}
                                                                try { style.removeStyleSource("driver-route-src") } catch (_: Exception) {}
                                                                try { style.removeStyleLayer("client-layer") } catch (_: Exception) {}
                                                                try { style.removeStyleSource("client-src") } catch (_: Exception) {}
                                                                try { style.removeStyleLayer("driver-to-dest-layer") } catch (_: Exception) {}
                                                                try { style.removeStyleSource("driver-to-dest-src") } catch (_: Exception) {}
                                                                try { style.removeStyleLayer("driver-dest-layer") } catch (_: Exception) {}
                                                                try { style.removeStyleSource("driver-dest-src") } catch (_: Exception) {}
                                                            }
                                                        }
                                                        .addOnFailureListener { e -> com.intu.taxi.ui.debug.DebugLog.log("CompleteRide error: ${e.message}") }
                                                }
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp),
                                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488), contentColor = Color.White),
                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                                        ) {
                                            androidx.compose.material3.Text(text = "Completar viaje", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                        }
                                        androidx.compose.material3.Button(
                                            onClick = {
                                                val rid = currentRideId
                                                if (rid != null) {
                                                    com.google.firebase.ktx.Firebase.functions(context.getString(com.intu.taxi.R.string.functions_region))
                                                        .getHttpsCallable("cancelRide")
                                                        .call(mapOf("currentRideId" to rid))
                                                        .addOnSuccessListener {
                                                            isCurrentRide = false
                                                            currentRideId = null
                                                            isArrived = false
                                                            isInProgress = false
                                                            clientLiveLocation = null
                                                            showCodeDialog = false
                                                            mapView.mapboxMap.getStyle { style ->
                                                                try { style.removeStyleLayer("driver-route-layer") } catch (_: Exception) {}
                                                                try { style.removeStyleSource("driver-route-src") } catch (_: Exception) {}
                                                                try { style.removeStyleLayer("client-layer") } catch (_: Exception) {}
                                                                try { style.removeStyleSource("client-src") } catch (_: Exception) {}
                                                                try { style.removeStyleLayer("driver-to-dest-layer") } catch (_: Exception) {}
                                                                try { style.removeStyleSource("driver-to-dest-src") } catch (_: Exception) {}
                                                                try { style.removeStyleLayer("driver-dest-layer") } catch (_: Exception) {}
                                                                try { style.removeStyleSource("driver-dest-src") } catch (_: Exception) {}
                                                            }
                                                        }
                                                        .addOnFailureListener { e -> com.intu.taxi.ui.debug.DebugLog.log("CancelRide error: ${e.message}") }
                                                }
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(44.dp),
                                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F), contentColor = Color.White),
                                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                                        ) {
                                            androidx.compose.material3.Text(text = "Cancelar viaje", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                } else {
                                    androidx.compose.material3.Button(
                                        onClick = {
                                            val rid = currentRideId
                                            if (rid != null) {
                                                com.google.firebase.ktx.Firebase.functions(context.getString(com.intu.taxi.R.string.functions_region))
                                                    .getHttpsCallable("cancelRide")
                                                    .call(mapOf("currentRideId" to rid))
                                                    .addOnSuccessListener {
                                                        isCurrentRide = false
                                                        currentRideId = null
                                                        isArrived = false
                                                        clientLiveLocation = null
                                                        showCodeDialog = false
                                                        mapView.mapboxMap.getStyle { style ->
                                                            try { style.removeStyleLayer("driver-route-layer") } catch (_: Exception) {}
                                                            try { style.removeStyleSource("driver-route-src") } catch (_: Exception) {}
                                                            try { style.removeStyleLayer("client-layer") } catch (_: Exception) {}
                                                            try { style.removeStyleSource("client-src") } catch (_: Exception) {}
                                                        }
                                                    }
                                                    .addOnFailureListener { e -> com.intu.taxi.ui.debug.DebugLog.log("CancelRide error: ${e.message}") }
                                            }
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(44.dp),
                                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F), contentColor = Color.White),
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                                    ) {
                                        androidx.compose.material3.Text(text = "Cancelar viaje", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
                if (showCodeDialog) {
                    androidx.compose.material3.AlertDialog(
                        onDismissRequest = { showCodeDialog = false },
                        title = { androidx.compose.material3.Text("Validar código de inicio") },
                        text = {
                            androidx.compose.material3.OutlinedTextField(
                                value = codeInput,
                                onValueChange = { codeInput = it.filter { ch -> ch.isDigit() }.take(4) },
                                singleLine = true,
                                label = { androidx.compose.material3.Text("Código de 4 dígitos") }
                            )
                        },
                        confirmButton = {
                            androidx.compose.material3.Button(onClick = {
                                val rid = currentRideId
                                val codeNum = codeInput.toIntOrNull()
                                if (rid != null && codeNum != null) {
                                    com.google.firebase.ktx.Firebase.functions(context.getString(com.intu.taxi.R.string.functions_region))
                                        .getHttpsCallable("verifyStartCode")
                                        .call(mapOf("currentRideId" to rid, "code" to codeNum))
                                        .addOnSuccessListener { showCodeDialog = false; com.intu.taxi.ui.debug.DebugLog.log("VerifyStartCode OK") }
                                        .addOnFailureListener { e -> com.intu.taxi.ui.debug.DebugLog.log("VerifyStartCode error: ${e.message}") }
                                }
                            }) { androidx.compose.material3.Text("Validar") }
                        },
                        dismissButton = {
                            androidx.compose.material3.OutlinedButton(onClick = { showCodeDialog = false }) { androidx.compose.material3.Text("Cancelar") }
                        }
                    )
                }
            }
        }
    }

    RatingDialog(
        title = "Calificar pasajero",
        show = showRatingDialog,
        allowComment = false,
        onDismiss = {
            com.intu.taxi.ui.debug.DebugLog.log("RATING-DRIVER: dialog dismiss")
            showRatingDialog = false
        },
        onSubmit = { stars, _ ->
            val rideId = lastCompletedRideId
            val targetUserId = lastCompletedTargetUserId
            if (!rideId.isNullOrBlank() && !targetUserId.isNullOrBlank()) {
                com.google.firebase.ktx.Firebase.functions(context.getString(com.intu.taxi.R.string.functions_region))
                    .getHttpsCallable("submitRating")
                    .call(mapOf(
                        "rideId" to rideId!!,
                        "targetUserId" to targetUserId!!,
                        "role" to "passenger",
                        "stars" to stars
                    ))
                    .addOnSuccessListener {
                        com.intu.taxi.ui.debug.DebugLog.log("RATING-DRIVER: submit OK rid=${rideId} stars=${stars}")
                        showRatingDialog = false
                    }
                    .addOnFailureListener { e ->
                        com.intu.taxi.ui.debug.DebugLog.log("RATING-DRIVER: submit error ${e.message}")
                    }
            } else {
                com.intu.taxi.ui.debug.DebugLog.log("RATING-DRIVER: datos inválidos para submit (rideId/target)")
                showRatingDialog = false
            }
        }
    )

    val mapboxMap = mapView.mapboxMap
    LaunchedEffect(mapView) {
        mapboxMap.loadStyleUri(Style.STANDARD) { _ ->
            DebugLog.log("Mapa cargó estilo MAPBOX_STANDARD")
        }
    }

    val onFirstIndicator = remember {
        object : com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener {
            override fun onIndicatorPositionChanged(point: Point) {
                lastDriverLocation = point
                mapboxMap.setCamera(CameraOptions.Builder().center(point).zoom(15.0).build())
                mapView.location.removeOnIndicatorPositionChangedListener(this)
                DebugLog.log("Centrado en posición inicial: $point")
            }
        }
    }
    DisposableEffect(mapView) {
        mapView.location.addOnIndicatorPositionChangedListener(onFirstIndicator)
        onDispose {
            mapView.location.removeOnIndicatorPositionChangedListener(onFirstIndicator)
        }
    }

    DisposableEffect(isSearching) {
        var listener: ValueEventListener? = null
        if (isSearching) {
            val q: Query = FirebaseDatabase.getInstance().reference.child("rideRequests").orderByChild("status").equalTo("searching")
            listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val out = mutableListOf<RideRequestItem>()
                    snapshot.children.forEach { child ->
                        val id = child.key ?: return@forEach
                        val pm = child.child("paymentMethod").getValue(String::class.java) ?: ""
                        val rt = child.child("rideType").getValue(String::class.java) ?: ""
                        val oLat = child.child("originLat").getValue(Double::class.java) ?: 0.0
                        val oLon = child.child("originLon").getValue(Double::class.java) ?: 0.0
                        val dLat = child.child("destLat").getValue(Double::class.java) ?: 0.0
                        val dLon = child.child("destLon").getValue(Double::class.java) ?: 0.0
                        val price = child.child("price").getValue(Double::class.java)
                        out.add(RideRequestItem(id, pm, rt, oLat, oLon, dLat, dLon, price = price))
                    }
                    rideRequests = out
                }
                override fun onCancelled(error: DatabaseError) {
                    com.intu.taxi.ui.debug.DebugLog.log("DB rideRequests error: ${error.message}")
                }
            }
            q.addValueEventListener(listener!!)
        }
        onDispose {
            if (listener != null) {
                FirebaseDatabase.getInstance().reference.child("rideRequests").removeEventListener(listener!!)
            }
        }
    }

    val mapboxToken = stringResource(id = com.intu.taxi.R.string.mapbox_access_token)
    LaunchedEffect(rideRequests, mapboxToken) {
        if (mapboxToken.isNotBlank()) {
            val needs = rideRequests.any { it.originCity == null || it.destCity == null }
            if (needs) {
                val enriched = rideRequests.map { item ->
                    val oc = item.originCity ?: reverseGeocodeCity(mapboxToken, item.originLon, item.originLat)
                    val dc = item.destCity ?: reverseGeocodeCity(mapboxToken, item.destLon, item.destLat)
                    item.copy(originCity = oc, destCity = dc)
                }
                rideRequests = enriched
            }
        }
    }

    // Suscripción al current ride del conductor
    DisposableEffect(Unit) {
        val uidNow = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        var listener: ValueEventListener? = null
        if (uidNow != null) {
            val q = FirebaseDatabase.getInstance().reference.child("currentRides").orderByChild("driverId").equalTo(uidNow)
            listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val first = snapshot.children.firstOrNull()
                    currentRideId = first?.key
                    isCurrentRide = currentRideId != null
                    val statusStr = first?.child("status")?.getValue(String::class.java)
                    isArrived = (statusStr == "arrived")
                    isInProgress = (statusStr == "in_progress")
                    val cLoc = first?.child("clientLocation")
                    val clat = cLoc?.child("lat")?.getValue(Double::class.java)
                    val clon = cLoc?.child("lon")?.getValue(Double::class.java)
                    if (clat != null && clon != null) clientLiveLocation = Point.fromLngLat(clon, clat)
                    clientName = first?.child("clientName")?.getValue(String::class.java)
                    clientPhone = first?.child("clientPhone")?.getValue(String::class.java)
                    currentRideUserId = first?.child("userId")?.getValue(String::class.java)
                    currentRidePaymentMethod = first?.child("paymentMethod")?.getValue(String::class.java)
                    currentRidePrice = first?.child("price")?.getValue(Double::class.java)
                    currentRideRideType = first?.child("rideType")?.getValue(String::class.java)
                    val oLblDb = first?.child("originLabel")?.getValue(String::class.java)
                    if (oLblDb != null) currentRideOriginLabel = oLblDb
                    val dLblDb = first?.child("destLabel")?.getValue(String::class.java)
                    if (dLblDb != null) currentRideDestLabel = dLblDb
                    val oCityDb = first?.child("originCity")?.getValue(String::class.java)
                    if (oCityDb != null) currentRideOriginLabel = "Origen: $oCityDb"
                    val dCityDb = first?.child("destCity")?.getValue(String::class.java)
                    if (dCityDb != null) currentRideDestLabel = "Destino: $dCityDb"
                    val dLatDb = first?.child("destLat")?.getValue(Double::class.java)
                    val dLonDb = first?.child("destLon")?.getValue(Double::class.java)
                    currentRideDestPoint = if (dLatDb != null && dLonDb != null) Point.fromLngLat(dLonDb, dLatDb) else null
                    clientPhotoUrl = first?.child("clientPhoto")?.getValue(String::class.java)
                }
                override fun onCancelled(error: DatabaseError) {}
            }
            q.addValueEventListener(listener!!)
        }
        onDispose {
            if (listener != null) FirebaseDatabase.getInstance().reference.child("currentRides").removeEventListener(listener!!)
        }
    }

    // Renderizar icono del cliente en el mapa durante current ride
    LaunchedEffect(clientLiveLocation, isInProgress) {
        val p = clientLiveLocation
        if (p != null && !isInProgress) {
            val srcId = "client-src"
            val layerId = "client-layer"
            mapboxMap.getStyle { style ->
                try { style.removeStyleLayer(layerId) } catch (_: Exception) {}
                try { style.removeStyleSource(srcId) } catch (_: Exception) {}
                style.addSource(geoJsonSource(srcId) { feature(Feature.fromGeometry(p)) })
                style.addLayer(
                    circleLayer(layerId, srcId) {
                        circleRadius(7.0)
                        circleColor("#EF4444")
                        circleStrokeColor("#FFFFFF")
                        circleStrokeWidth(2.0)
                    }
                )
            }
        }
    }

    LaunchedEffect(clientLiveLocation, mapboxToken) {
        val p = clientLiveLocation
        if (p != null && mapboxToken.isNotBlank()) {
            try {
                val city = reverseGeocodeCity(mapboxToken, p.longitude(), p.latitude())
                currentRideOriginLabel = city?.let { "Origen: $it" }
                currentRideDestLabel = city?.let { "Destino: $it" }
            } catch (e: Exception) {
                DebugLog.log("Error geocodificando ciudad cliente: ${e.message}")
            }
        }
    }

    LaunchedEffect(isInProgress) {
        if (isInProgress) {
            mapView.mapboxMap.getStyle { style ->
                try { style.removeStyleLayer("driver-route-layer") } catch (_: Exception) {}
                try { style.removeStyleSource("driver-route-src") } catch (_: Exception) {}
                try { style.removeStyleLayer("client-layer") } catch (_: Exception) {}
                try { style.removeStyleSource("client-src") } catch (_: Exception) {}
            }
        }
    }

    LaunchedEffect(currentRideDestPoint, mapboxToken) {
        val p = currentRideDestPoint
        if (p != null && mapboxToken.isNotBlank()) {
            try {
                val city = reverseGeocodeCity(mapboxToken, p.longitude(), p.latitude())
                if (city != null) currentRideDestLabel = "Destino: $city"
            } catch (e: Exception) {
                DebugLog.log("Error geocodificando ciudad destino: ${e.message}")
            }
        }
    }

    LaunchedEffect(isInProgress, currentRideDestPoint) {
        if (isInProgress) {
            val dest = currentRideDestPoint
            if (dest != null) {
                val srcId = "driver-dest-src"
                val layerId = "driver-dest-layer"
                mapView.mapboxMap.getStyle { style ->
                    try { style.removeStyleLayer(layerId) } catch (_: Exception) {}
                    try { style.removeStyleSource(srcId) } catch (_: Exception) {}
                    style.addSource(geoJsonSource(srcId) { feature(Feature.fromGeometry(dest)) })
                    style.addLayer(
                        circleLayer(layerId, srcId) {
                            circleRadius(8.0)
                            circleColor("#1E88E5")
                            circleStrokeColor("#FFFFFF")
                            circleStrokeWidth(2.0)
                        }
                    )
                }
            }
        }
    }

    LaunchedEffect(isInProgress, currentRideDestPoint, lastDriverLocation, mapboxToken) {
        val origin = lastDriverLocation
        val dest = currentRideDestPoint
        if (isInProgress && origin != null && dest != null && mapboxToken.isNotBlank()) {
            try {
                val url = "https://api.mapbox.com/directions/v5/mapbox/driving-traffic/${origin.longitude()},${origin.latitude()};${dest.longitude()},${dest.latitude()}?alternatives=false&geometries=geojson&overview=full&language=es&access_token=${mapboxToken}"
                val json = withContext(Dispatchers.IO) {
                    URL(url).openStream().bufferedReader().use { it.readText() }
                }
                val obj = JSONObject(json)
                val routes = obj.optJSONArray("routes") ?: JSONArray()
                if (routes.length() > 0) {
                    val route = routes.getJSONObject(0)
                    val geometry = route.optJSONObject("geometry")
                    val coords = geometry?.optJSONArray("coordinates")
                    if (coords != null && coords.length() > 1) {
                        val pts = mutableListOf<Point>()
                        for (i in 0 until coords.length()) {
                            val c = coords.getJSONArray(i)
                            val lon = c.optDouble(0)
                            val lat = c.optDouble(1)
                            pts.add(Point.fromLngLat(lon, lat))
                        }
                        val line = LineString.fromLngLats(pts)
                        val routeSrcId = "driver-to-dest-src"
                        val routeLayerId = "driver-to-dest-layer"
                        mapboxMap.getStyle { style ->
                            try { style.removeStyleLayer(routeLayerId) } catch (_: Exception) {}
                            try { style.removeStyleSource(routeSrcId) } catch (_: Exception) {}
                            style.addSource(geoJsonSource(routeSrcId) { feature(Feature.fromGeometry(line)) })
                            style.addLayer(
                                com.mapbox.maps.extension.style.layers.generated.lineLayer(routeLayerId, routeSrcId) {
                                    lineColor("#1E88E5")
                                    lineWidth(5.0)
                                    lineOpacity(0.9)
                                }
                            )
                        }
                        DebugLog.log("Ruta conductor→destino renderizada")
                    }
                }
            } catch (e: Exception) {
                DebugLog.log("Error calculando ruta driver→destino: ${e.message}")
            }
        }
    }

    // Ruta entre conductor y pasajero durante current ride
    LaunchedEffect(clientLiveLocation, lastDriverLocation, mapboxToken, isInProgress) {
        val origin = lastDriverLocation
        val dest = clientLiveLocation
        if (!isInProgress && origin != null && dest != null && mapboxToken.isNotBlank()) {
            try {
                val url = "https://api.mapbox.com/directions/v5/mapbox/driving-traffic/${origin.longitude()},${origin.latitude()};${dest.longitude()},${dest.latitude()}?alternatives=false&geometries=geojson&overview=full&language=es&access_token=${mapboxToken}"
                val json = withContext(Dispatchers.IO) {
                    URL(url).openStream().bufferedReader().use { it.readText() }
                }
                val obj = JSONObject(json)
                val routes = obj.optJSONArray("routes") ?: JSONArray()
                if (routes.length() > 0) {
                    val route = routes.getJSONObject(0)
                    val geometry = route.optJSONObject("geometry")
                    val coords = geometry?.optJSONArray("coordinates")
                    if (coords != null && coords.length() > 1) {
                        val pts = mutableListOf<Point>()
                        for (i in 0 until coords.length()) {
                            val c = coords.getJSONArray(i)
                            val lon = c.optDouble(0)
                            val lat = c.optDouble(1)
                            pts.add(Point.fromLngLat(lon, lat))
                        }
                        val line = LineString.fromLngLats(pts)
                        val routeSrcId = "driver-route-src"
                        val routeLayerId = "driver-route-layer"
                        mapboxMap.getStyle { style ->
                            try { style.removeStyleLayer(routeLayerId) } catch (_: Exception) {}
                            try { style.removeStyleSource(routeSrcId) } catch (_: Exception) {}
                            style.addSource(geoJsonSource(routeSrcId) { feature(Feature.fromGeometry(line)) })
                            style.addLayer(
                                com.mapbox.maps.extension.style.layers.generated.lineLayer(routeLayerId, routeSrcId) {
                                    lineColor("#10B981")
                                    lineWidth(5.0)
                                    lineOpacity(0.9)
                                }
                            )
                        }
                        DebugLog.log("Ruta conductor→pasajero renderizada")
                    }
                }
            } catch (e: Exception) {
                DebugLog.log("Error calculando ruta driver→cliente: ${e.message}")
            }
        }
    }

    // Actualizar posición del conductor en current ride
    DisposableEffect(currentRideId) {
        val rid = currentRideId
        val uidNow = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        val listener = object : com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener {
            override fun onIndicatorPositionChanged(point: Point) {
                if (rid != null && uidNow != null) {
                    FirebaseDatabase.getInstance().reference.child("currentRides").child(rid)
                        .child("driverLocation").setValue(mapOf("lat" to point.latitude(), "lon" to point.longitude()))
                }
            }
        }
        mapView.location.addOnIndicatorPositionChangedListener(listener)
        onDispose { mapView.location.removeOnIndicatorPositionChangedListener(listener) }
    }

    LaunchedEffect(isCurrentRide) {
        if (!isCurrentRide) {
            isArrived = false
            clientLiveLocation = null
            showCodeDialog = false
            mapView.mapboxMap.getStyle { style ->
                try { style.removeStyleLayer("driver-route-layer") } catch (_: Exception) {}
                try { style.removeStyleSource("driver-route-src") } catch (_: Exception) {}
                try { style.removeStyleLayer("client-layer") } catch (_: Exception) {}
                try { style.removeStyleSource("client-src") } catch (_: Exception) {}
            }
        }
    }
    if (isCurrentRide && showChat) {
        val rid = currentRideId
        val me = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (!rid.isNullOrBlank() && !me.isNullOrBlank()) {
            RideChatSheet(rideId = rid, meUid = me, onClose = { showChat = false })
        }
    }
}

@Composable
private fun AnimatedGradientButton(
    isSearching: Boolean,
    onClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val gradientShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = if (isSearching) 6000 else 8000, easing = LinearEasing)),
        label = "gradientShift"
    )
    val radarProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 1600, easing = LinearEasing)),
        label = "radarProgress"
    )
    val gradientColors = if (isSearching) {
        listOf(Color(0xFFFF5A5A), Color(0xFFD32F2F), Color(0xFFFF8A80))
    } else {
        listOf(Color(0xFF00E5C3), Color(0xFF00BFA5), Color(0xFF00695C))
    }
    val angleInRad = gradientShift * kotlin.math.PI.toFloat() / 180f
    val startOffset = Offset(x = kotlin.math.cos(angleInRad) * 300f, y = kotlin.math.sin(angleInRad) * 300f)
    val endOffset = Offset(x = -kotlin.math.cos(angleInRad) * 300f, y = -kotlin.math.sin(angleInRad) * 300f)
    val pulseScale by animateFloatAsState(targetValue = if (isSearching) 1.1f else 1f, animationSpec = tween(1000, easing = FastOutSlowInEasing), label = "pulseScale")

    Box(
        modifier = Modifier
            .size(140.dp)
            .drawBehind {
                if (isSearching) {
                    val center = this.center
                    val maxR = size.minDimension / 2f
                    val ringColor = Color(0xFFFF5A5A).copy(alpha = 0.45f)
                    val phases = listOf(0f, 0.33f, 0.66f)
                    phases.forEach { phase ->
                        val p = ((radarProgress + phase) % 1f)
                        val radius = 6f + p * maxR
                        val alpha = (1f - p).coerceIn(0f, 1f) * 0.45f
                        drawCircle(color = ringColor.copy(alpha = alpha), radius = radius, center = center, style = Stroke(width = 4f))
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .graphicsLayer {
                    scaleX = pulseScale
                    scaleY = pulseScale
                    shadowElevation = 12f
                    shape = CircleShape
                    clip = true
                }
                .background(brush = Brush.linearGradient(colors = gradientColors, start = startOffset, end = endOffset), shape = CircleShape)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Text(text = if (isSearching) "Parar" else "Buscar", color = Color.White, style = MaterialTheme.typography.titleMedium)
        }
    }
}
data class RideRequestItem(
    val id: String,
    val paymentMethod: String,
    val rideType: String,
    val originLat: Double,
    val originLon: Double,
    val destLat: Double,
    val destLon: Double,
    val originCity: String? = null,
    val destCity: String? = null,
    val price: Double? = null
)

@Composable
private fun InfoChip(text: String, bg: Color = Color(0xFFF3F4F6), fg: Color = Color(0xFF111827)) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(50.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text = text, color = fg, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun RideRequestFancyCard(item: RideRequestItem, onAccept: () -> Unit) {
    androidx.compose.material3.Card(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 12.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF0D9488).copy(alpha = 0.15f), Color.Transparent),
                        center = Offset(0.85f, 0.2f),
                        radius = 400f
                    )
                )
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(item.rideType, style = MaterialTheme.typography.titleMedium, color = Color(0xFF111827))
                    Spacer(Modifier.height(4.dp))
                    val priceStr = item.price?.let { String.format("$%.2f", it) } ?: "--"
                    Text("Pago: ${item.paymentMethod} · Precio: ${priceStr}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF6E6E73))
                    Spacer(Modifier.height(4.dp))
                    Text("Origen: ${item.originCity ?: "?"}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF6E6E73))
                    Text("Destino: ${item.destCity ?: "?"}", style = MaterialTheme.typography.labelSmall, color = Color(0xFF6E6E73))
                }
                androidx.compose.material3.Button(
                    onClick = onAccept,
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488), contentColor = Color.White),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ) {
                    Text("Aceptar")
                }
            }
        }
    }
}

private suspend fun httpGet(url: String): String {
    val conn = URL(url).openConnection() as HttpURLConnection
    conn.connectTimeout = 6000
    conn.readTimeout = 6000
    conn.requestMethod = "GET"
    conn.doInput = true
    return try {
        conn.inputStream.bufferedReader().use { it.readText() }
    } finally {
        conn.disconnect()
    }
}

private fun parseCityFromFeature(f: JSONObject): String? {
    val ctx = f.optJSONArray("context") ?: JSONArray()
    var city: String? = null
    for (j in 0 until ctx.length()) {
        val c = ctx.optJSONObject(j)
        val id = c?.optString("id", "") ?: ""
        if (id.startsWith("place.") || id.startsWith("locality.")) {
            city = c.optString("text", null)
            break
        }
    }
    return city ?: f.optString("text", null)
}

private suspend fun reverseGeocodeCity(token: String, lon: Double, lat: Double): String? {
    val url = "https://api.mapbox.com/geocoding/v5/mapbox.places/${lon},${lat}.json?types=place,locality&language=es&limit=1&access_token=${token}"
    val json = withContext(Dispatchers.IO) { httpGet(url) }
    val obj = JSONObject(json)
    val feats = obj.optJSONArray("features") ?: JSONArray()
    if (feats.length() == 0) return null
    return parseCityFromFeature(feats.getJSONObject(0))
}
