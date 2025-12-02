package com.intu.taxi.ui.screens

import android.annotation.SuppressLint
import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
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
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.ScreenCoordinate
import com.mapbox.geojson.Point
import com.mapbox.geojson.Feature
import com.mapbox.geojson.LineString
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.logo.logo
import com.mapbox.maps.plugin.attribution.attribution
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.layers.generated.circleLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.layers.addLayer
import androidx.compose.material3.Text
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max
import kotlin.math.cos
import kotlin.math.PI
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.draw.scale
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.net.URLEncoder
import java.net.HttpURLConnection
import java.util.Locale
import org.json.JSONObject
import org.json.JSONArray
import androidx.compose.ui.graphics.vector.ImageVector
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.intu.taxi.ui.screens.SavedPlace

@Composable
fun SearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit = {},
    onPinClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    placeholderText: String = "¿A dónde quieres ir?",
    showClearButton: Boolean = false,
    onClearClick: () -> Unit = {},
    showMicButton: Boolean = true
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(50))
            .clip(RoundedCornerShape(50))
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholderText, color = Color(0xFF7A7F87)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Color(0xFF8E8E93)) },
            trailingIcon = {
                if (showClearButton && value.isNotEmpty()) {
                    IconButton(onClick = onClearClick, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.Clear, contentDescription = "Limpiar", tint = Color(0xFF8E8E93))
                    }
                } else if (showMicButton) {
                    IconButton(onClick = onPinClick, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Color(0xFF8E8E93))
                    }
                }
            },
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedContainerColor = Color.White.copy(alpha = 0.85f),
                unfocusedContainerColor = Color.White.copy(alpha = 0.85f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { onFocusChange(it.isFocused) }
        )
    }
}

@SuppressLint("MissingPermission")
@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val mapView = remember { MapView(context) }

    DisposableEffect(Unit) {
        mapView.onStart()
        onDispose {
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    LaunchedEffect(mapView) {
        mapView.scalebar.enabled = false
        mapView.logo.enabled = false
        mapView.attribution.enabled = false
    }

    // Location Component: enable if permission granted
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

    LaunchedEffect(Unit) {
        if (!grantedFine && !grantedCoarse) {
            locationPermissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }
    LaunchedEffect(grantedFine, grantedCoarse) {
        if (grantedFine || grantedCoarse) {
            mapView.location.updateSettings { enabled = true }
        }
    }

    var headerVisible by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }
    var firstName by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<Pair<String, Point>>>(emptyList()) }
    var showSearchBar by remember { mutableStateOf(true) }
    var selectedDestination by remember { mutableStateOf<Point?>(null) }
    var isRouteMode by remember { mutableStateOf(false) }
    var isPinMode by remember { mutableStateOf(false) }
    var pinCenter by remember { mutableStateOf<Point?>(null) }
    var lastUserLocation by remember { mutableStateOf<Point?>(null) }
    var routeDurationMinutes by remember { mutableStateOf<Double?>(null) }
    var routeDistanceKm by remember { mutableStateOf<Double?>(null) }
    var selectedRide by remember { mutableStateOf<String?>(null) }
    var paymentMethod by remember { mutableStateOf("efectivo") }
    var isSearchingDriver by remember { mutableStateOf(false) }
    var currentRideRequestId by remember { mutableStateOf<String?>(null) }
    var currentRideId by remember { mutableStateOf<String?>(null) }
    var driverLiveLocation by remember { mutableStateOf<Point?>(null) }
    var isCurrentRide by remember { mutableStateOf(false) }
    val mapboxPublicToken = stringResource(id = com.intu.taxi.R.string.mapbox_access_token)
    val rootView = LocalView.current
    val focusManager = LocalFocusManager.current
    var imeVisible by remember { mutableStateOf(false) }
    var savedPlaces by remember { mutableStateOf<List<SavedPlace>>(emptyList()) }
    var showAddPlace by remember { mutableStateOf(false) }
    var savedPlaceQueued by remember { mutableStateOf<SavedPlace?>(null) }
    val density = LocalDensity.current
    DisposableEffect(rootView) {
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { _, insets ->
            imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime())
            insets
        }
        onDispose { ViewCompat.setOnApplyWindowInsetsListener(rootView, null) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { mapView }
        )

        DisposableEffect(Unit) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            var reg: com.google.firebase.firestore.ListenerRegistration? = null
            if (uid != null) {
                val docRef = FirebaseFirestore.getInstance().collection("users").document(uid)
                reg = docRef.addSnapshotListener { doc, _ ->
                    val newFirstName = doc?.getString("firstName") ?: ""
                    if (newFirstName != firstName) firstName = newFirstName
                    val sp = doc?.get("savedPlaces") as? Map<*, *>
                    val placesAny = sp?.get("places") as? List<*> ?: emptyList<Any>()
                    val parsed = placesAny.mapNotNull { any ->
                        val m = any as? Map<*, *> ?: return@mapNotNull null
                        val name = (m["name"] as? String) ?: ""
                        val lat = (m["lat"] as? Number)?.toDouble() ?: return@mapNotNull null
                        val lon = (m["lon"] as? Number)?.toDouble() ?: return@mapNotNull null
                        val label = m["label"] as? String
                        val icon = m["icon"] as? String
                        SavedPlace(type = "other", name = name, lat = lat, lon = lon, label = label, icon = icon)
                    }
                    if (parsed != savedPlaces) savedPlaces = parsed
                    val pm = doc?.getString("paymentMethod")
                    if (!pm.isNullOrBlank() && pm != paymentMethod) paymentMethod = pm
                }
            }
            onDispose { reg?.remove() }
        }
        LaunchedEffect(Unit) { headerVisible = true }
        LaunchedEffect(imeVisible) {
            if (showSearchBar && !isRouteMode) {
                isSearchFocused = imeVisible
                if (!imeVisible) suggestions = emptyList()
            }
        }
        LaunchedEffect(savedPlaceQueued) {
            val p = savedPlaceQueued
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (p != null && uid != null) {
                val placeMap = mutableMapOf<String, Any>(
                    "name" to p.name,
                    "lat" to p.lat,
                    "lon" to p.lon
                )
                if (!p.label.isNullOrBlank()) placeMap["label"] = p.label!!
                if (!p.icon.isNullOrBlank()) placeMap["icon"] = p.icon!!
                FirebaseFirestore.getInstance().collection("users").document(uid)
                    .update(com.google.firebase.firestore.FieldPath.of("savedPlaces", "places"), com.google.firebase.firestore.FieldValue.arrayUnion(placeMap))
                DebugLog.log("Home: lugar guardado -> ${p.name}")
                savedPlaceQueued = null
            }
        }
        LaunchedEffect(searchQuery) {
            if (mapboxPublicToken.isBlank()) {
                suggestions = emptyList()
            } else if (!isPinMode && searchQuery.trim().length >= 2) {
                try {
                    delay(250)
                    val q = URLEncoder.encode(searchQuery.trim(), "UTF-8")
                    val centerPoint = mapView.mapboxMap.cameraState.center
                    val lat = centerPoint.latitude()
                    val lon = centerPoint.longitude()
                    val miles = SEARCH_RADIUS_MILES
                    val latDelta = miles / 69.0
                    val latRad = lat * PI / 180.0
                    val lonDelta = miles / (69.0 * cos(latRad))
                    val minLon = lon - lonDelta
                    val minLat = lat - latDelta
                    val maxLon = lon + lonDelta
                    val maxLat = lat + latDelta
                    val bbox = "$minLon,$minLat,$maxLon,$maxLat"
                    val result = withContext(Dispatchers.IO) {
                        geocodeSuggestions(mapboxPublicToken, q, centerPoint, bbox)
                    }
                    suggestions = result
                } catch (e: Exception) {
                    DebugLog.log("Error geocoding: ${e.message}")
                    suggestions = emptyList()
                }
            } else {
                suggestions = emptyList()
            }
        }

        val headerShiftFraction by animateFloatAsState(
            targetValue = if (isSearchFocused || isPinMode) 0.5f else 0f,
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            label = "headerShiftFraction"
        )
        if (!isRouteMode) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.55f)
                    .then(
                        if (!isCurrentRide) Modifier.drawBehind {
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
                                        blendMode = androidx.compose.ui.graphics.BlendMode.DstIn
                                    )
                                }
                            }
                        } else Modifier
                    ),
                contentAlignment = Alignment.TopCenter
            ) {
            AnimatedVisibility(
                visible = headerVisible && !isRouteMode && !isCurrentRide,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it })
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 20.dp, start = 16.dp, end = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AnimatedVisibility(
                        visible = !(isSearchFocused || isPinMode || isCurrentRide),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "intu",
                                color = Color.White,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                fontSize = 32.sp
                            )
                            Spacer(modifier = Modifier.height(15.dp))
                            Text(
                                text = if (firstName.isNotBlank()) "¡Hola $firstName!" else "¡Hola!",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge,
                                fontSize = 24.sp
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                        }
                    }
                    if (showSearchBar && !isCurrentRide) {
                          SearchBar(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            onFocusChange = { focused -> isSearchFocused = focused },
                            onPinClick = {
                                isPinMode = true
                                isSearchFocused = false
                                suggestions = emptyList()
                                focusManager.clearFocus()
                                pinCenter = mapView.mapboxMap.cameraState.center
                            },
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                    if (showSearchBar && !isCurrentRide && !isPinMode && suggestions.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                                .shadow(4.dp, RoundedCornerShape(12.dp)),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(vertical = 6.dp)) {
                                suggestions.take(6).forEach { s ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                            .clickable {
                                                mapView.mapboxMap.setCamera(
                                                    CameraOptions.Builder()
                                                        .center(s.second)
                                                        .zoom(14.0)
                                                        .build()
                                                )
                                                selectedDestination = s.second
                                                showSearchBar = false
                                                isSearchFocused = false
                                                searchQuery = ""
                                                suggestions = emptyList()
                                                isRouteMode = true
                                            },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = s.first, color = Color(0xFF111827))
                                    }
                                }
                            }
                        }
                    }
                    if (!isSearchFocused) {
                        Spacer(modifier = Modifier.height(25.dp))
                    }
                    AnimatedVisibility(
                        visible = !(isSearchFocused || isPinMode || isCurrentRide),
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        RowQuickActions(
                            places = savedPlaces,
                            onAddPlace = { showAddPlace = true },
                            onPlaceSelected = { p ->
                                val destPoint = Point.fromLngLat(p.lon, p.lat)
                                mapView.mapboxMap.setCamera(
                                    CameraOptions.Builder()
                                        .center(destPoint)
                                        .zoom(14.0)
                                        .build()
                                )
                                selectedDestination = destPoint
                                showSearchBar = false
                                isSearchFocused = false
                                searchQuery = ""
                                suggestions = emptyList()
                                isRouteMode = true
                            }
                        )
                    }
                }
            }
        }
        }
        // Route mode: back button to initial mode
        if (isRouteMode) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 16.dp, top = 12.dp),
                contentAlignment = Alignment.TopStart
            ) {
                if (!isSearchingDriver && !isCurrentRide) {
                    Button(
                        onClick = {
                            mapView.mapboxMap.getStyle { style ->
                                try { style.removeStyleLayer("route-layer") } catch (_: Exception) {}
                                try { style.removeStyleSource("route-src") } catch (_: Exception) {}
                                try { style.removeStyleLayer("dest-layer") } catch (_: Exception) {}
                                try { style.removeStyleSource("dest-src") } catch (_: Exception) {}
                            }
                            isRouteMode = false
                            showSearchBar = true
                            headerVisible = true
                            selectedDestination = null
                            searchQuery = ""
                            suggestions = emptyList()
                            isSearchingDriver = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.95f)),
                        shape = RoundedCornerShape(50)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = Color(0xFF111827))
                    }
                }
            }
            // Ride options overlay at bottom (dynamic prices)
            val eta = routeDurationMinutes ?: 5.0
            val km = routeDistanceKm ?: 2.0
            val priceHonda = calcFare(km, eta, base = 2.0, perKm = 1.2, perMin = 0.25)
            val priceBajaj = calcFare(km, eta, base = 1.5, perKm = 0.9, perMin = 0.22)
            val priceColectivo = calcFare(km, eta, base = 1.2, perKm = 0.7, perMin = 0.20)
            val priceEsperaYAhorra = calcFare(km, eta, base = 1.5, perKm = 0.9, perMin = 0.22, multiplier = 0.90)
            val priceEnvioPaquete = calcFare(km, eta, base = 2.2, perKm = 1.4, perMin = 0.30, multiplier = 1.10)
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                val options = listOf(
                    RideOptionData("Intu Colectivo", priceColectivo, eta, null, listOf(Color(0xFF27AE60), Color(0xFF2ECC71))),
                    RideOptionData("espera y ahorra", priceEsperaYAhorra, eta, null, listOf(Color(0xFFF39C12), Color(0xFFE67E22))),
                    RideOptionData("Intu Honda", priceHonda, eta, null, listOf(Color(0xFF08817E), Color(0xFF0FB9B1))),
                    RideOptionData("Intu Bajaj", priceBajaj, eta, null, listOf(Color(0xFF1E1F47), Color(0xFF3A3B7B))),
                    RideOptionData("envio de paquete", priceEnvioPaquete, eta, null, listOf(Color(0xFF8E44AD), Color(0xFF9B59B6)))
                )
                if (!isSearchingDriver && !isCurrentRide) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Brush.linearGradient(listOf(Color(0xFF0D9488), Color(0xFF0F172A)))) ,
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = Color.White)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = stringResource(com.intu.taxi.R.string.choose_your_trip), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1C1C1E))
                                    Text(text = stringResource(com.intu.taxi.R.string.swipe_more_options), style = MaterialTheme.typography.bodySmall, color = Color(0xFF6E6E73))
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            RideOptionsSlider(
                                options = options,
                                selectedOptionName = selectedRide,
                                onOptionSelected = { selectedRide = it }
                            )
                            Spacer(Modifier.height(8.dp))
                            var paymentExpanded by remember { mutableStateOf(false) }
                            PaymentMethodRow(current = paymentMethod, onClick = { paymentExpanded = true })
                            if (paymentExpanded) {
                                AlertDialog(
                                    onDismissRequest = { paymentExpanded = false },
                                    title = { Text(stringResource(com.intu.taxi.R.string.payment_method_label)) },
                                    text = {
                                        PaymentMethodCompact(current = paymentMethod) { method ->
                                            val uidNow = FirebaseAuth.getInstance().currentUser?.uid
                                            paymentMethod = method
                                            if (uidNow != null) FirebaseFirestore.getInstance().collection("users").document(uidNow).update(mapOf("paymentMethod" to method))
                                        }
                                    },
                                    confirmButton = {
                                        Button(onClick = { paymentExpanded = false }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488), contentColor = Color.White)) { Text(stringResource(com.intu.taxi.R.string.ok)) }
                                    }
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    val uidNow = FirebaseAuth.getInstance().currentUser?.uid
                                    val origin = lastUserLocation
                                    val dest = selectedDestination
                                    val rideType = selectedRide ?: "Intu Honda"
                                    val priceSelected = when (rideType) {
                                        "Intu Colectivo" -> priceColectivo
                                        "espera y ahorra" -> priceEsperaYAhorra
                                        "Intu Honda" -> priceHonda
                                        "Intu Bajaj" -> priceBajaj
                                        "envio de paquete" -> priceEnvioPaquete
                                        else -> priceHonda
                                    }
                                    if (uidNow != null && origin != null && dest != null) {
                                        val ref = FirebaseDatabase.getInstance().reference.child("rideRequests").push()
                                        val key = ref.key
                                        val data = mapOf(
                                            "userId" to uidNow,
                                            "status" to "searching",
                                            "paymentMethod" to paymentMethod,
                                            "rideType" to rideType,
                                            "price" to priceSelected,
                                            "originLat" to origin.latitude(),
                                            "originLon" to origin.longitude(),
                                            "destLat" to dest.latitude(),
                                            "destLon" to dest.longitude(),
                                            "createdAt" to ServerValue.TIMESTAMP
                                        )
                                        ref.setValue(data).addOnSuccessListener {
                                            currentRideRequestId = key
                                            isSearchingDriver = true
                                            DebugLog.log("Ride request creado id=${key}")
                                        }.addOnFailureListener { e ->
                                            DebugLog.log("Error creando ride request: ${e.message}")
                                        }
                                    } else {
                                        DebugLog.log("No se puede crear ride request: uid/origen/dest nulos")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488), contentColor = Color.White),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(stringResource(com.intu.taxi.R.string.confirm_ride), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else if (isSearchingDriver) {
                    CreativeDriverSearchIndicator(
                        isVisible = true,
                        onCancel = {
                            val key = currentRideRequestId
                            if (key != null) {
                                FirebaseDatabase.getInstance().reference.child("rideRequests").child(key).removeValue().addOnSuccessListener {
                                    DebugLog.log("Ride request cancelado id=${key}")
                                }.addOnFailureListener { e ->
                                    DebugLog.log("Error cancelando ride request: ${e.message}")
                                }
                            }
                            currentRideRequestId = null
                            isSearchingDriver = false
                            showSearchBar = true
                            headerVisible = true
                        }
                    )
                } else if (isCurrentRide) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(text = "Viaje en curso", style = MaterialTheme.typography.titleMedium)
                            Text(text = "Viendo ubicación del conductor", style = MaterialTheme.typography.bodySmall, color = Color(0xFF6E6E73))
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = {},
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F), contentColor = Color.White),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text("Cancelar viaje", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Button(
                            onClick = {},
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F), contentColor = Color.White),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Cancelar viaje", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        BackHandler(enabled = isRouteMode) {
            mapView.mapboxMap.getStyle { style ->
                try { style.removeStyleLayer("route-layer") } catch (_: Exception) {}
                try { style.removeStyleSource("route-src") } catch (_: Exception) {}
                try { style.removeStyleLayer("dest-layer") } catch (_: Exception) {}
                try { style.removeStyleSource("dest-src") } catch (_: Exception) {}
            }
            isRouteMode = false
            showSearchBar = true
            headerVisible = true
            selectedDestination = null
            searchQuery = ""
            suggestions = emptyList()
        }
        // Pin mode: show red pin at screen center
        if (isPinMode) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(40.dp))
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                        .align(Alignment.BottomCenter),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = {
                            val center = pinCenter
                            if (center != null) {
                                selectedDestination = center
                                isPinMode = false
                                isRouteMode = true
                                showSearchBar = false
                                suggestions = emptyList()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.95f)),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("Confirmar", color = Color(0xFF111827))
                    }
                }
            }
        }
        // Update pin center by polling while pin mode is active
        LaunchedEffect(isPinMode) {
            while (isPinMode) {
                pinCenter = mapView.mapboxMap.cameraState.center
                delay(300)
            }
        }
        // Reverse geocode pin center into search field text
        LaunchedEffect(pinCenter, isPinMode) {
            val p = pinCenter
            if (isPinMode && p != null && mapboxPublicToken.isNotBlank()) {
                try {
                    delay(250)
                    val name = withContext(Dispatchers.IO) { reverseGeocode(mapboxPublicToken, p) }
                    if (!name.isNullOrBlank()) searchQuery = name
                } catch (e: Exception) {
                    DebugLog.log("Error reverse geocoding pin: ${e.message}")
                }
            }
        }
        // Back to normal from pin mode on system back
        BackHandler(enabled = isPinMode) {
            isPinMode = false
        }
        BackHandler(enabled = isSearchFocused && !isRouteMode) {
            focusManager.clearFocus()
        }
        if (showAddPlace) {
            AddPlaceScreen(defaultType = "other") { place ->
                DebugLog.log("Home(AddPlace): seleccionado ${place.name}")
                savedPlaceQueued = place
                showAddPlace = false
            }
        }
    }

    // Load style once and center on first location update
    val mapboxMap = mapView.mapboxMap
    LaunchedEffect(mapView) {
        mapboxMap.loadStyleUri(Style.STANDARD) { _ ->
            DebugLog.log("Mapa cargó estilo MAPBOX_STANDARD")
        }
    }

    // Update destination marker on selection
    LaunchedEffect(selectedDestination) {
        val srcId = "dest-src"
        val layerId = "dest-layer"
        mapboxMap.getStyle { style ->
            try {
                style.removeStyleLayer(layerId)
            } catch (_: Exception) {}
            try {
                style.removeStyleSource(srcId)
            } catch (_: Exception) {}
            val point = selectedDestination
            if (point != null) {
                style.addSource(geoJsonSource(srcId) { feature(Feature.fromGeometry(point)) })
                style.addLayer(
                    circleLayer(layerId, srcId) {
                        circleRadius(8.0)
                        circleColor("#1E88E5")
                        circleStrokeColor("#FFFFFF")
                        circleStrokeWidth(2.0)
                    }
                )
                DebugLog.log("Marcador de destino actualizado")
            } else {
                DebugLog.log("Sin destino seleccionado, no se añade marcador")
            }
        }
    }

    // Calculate and render route when both user location and destination are available
    LaunchedEffect(selectedDestination, lastUserLocation) {
        val origin = lastUserLocation
        val dest = selectedDestination
        if (origin != null && dest != null && mapboxPublicToken.isNotBlank()) {
            try {
                val url = "https://api.mapbox.com/directions/v5/mapbox/driving-traffic/${origin.longitude()},${origin.latitude()};${dest.longitude()},${dest.latitude()}?alternatives=false&geometries=geojson&overview=full&language=es&access_token=${mapboxPublicToken}"
                val json = withContext(Dispatchers.IO) {
                    URL(url).openStream().bufferedReader().use { it.readText() }
                }
                val obj = JSONObject(json)
                val routes = obj.optJSONArray("routes") ?: JSONArray()
                if (routes.length() > 0) {
                    val route = routes.getJSONObject(0)
                    val geometry = route.optJSONObject("geometry")
                    val durationSec = route.optDouble("duration", Double.NaN)
                    if (!durationSec.isNaN()) routeDurationMinutes = kotlin.math.max(1.0, durationSec / 60.0)
                    val distanceMeters = route.optDouble("distance", Double.NaN)
                    if (!distanceMeters.isNaN()) routeDistanceKm = kotlin.math.max(0.1, distanceMeters / 1000.0)
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
                        val routeSrcId = "route-src"
                        val routeLayerId = "route-layer"
                        mapboxMap.getStyle { style ->
                            try { style.removeStyleLayer(routeLayerId) } catch (_: Exception) {}
                            try { style.removeStyleSource(routeSrcId) } catch (_: Exception) {}
                            style.addSource(geoJsonSource(routeSrcId) { feature(Feature.fromGeometry(line)) })
                            style.addLayer(
                                lineLayer(routeLayerId, routeSrcId) {
                                    lineColor("#1E88E5")
                                    lineWidth(5.0)
                                    lineOpacity(0.9)
                                }
                            )
                        }
                        val topPad = with(density) { 40.dp.toPx() }.toDouble()
                        val bottomPad = with(density) { 420.dp.toPx() }.toDouble()
                        val sidePad = 60.0
                        val cam = mapboxMap.cameraForCoordinates(
                            pts,
                            EdgeInsets(topPad, sidePad, bottomPad, sidePad),
                            null,
                            null
                        )
                        mapboxMap.setCamera(cam)
                        val centerPx = mapboxMap.pixelForCoordinate(mapboxMap.cameraState.center)
                        val offsetY = with(density) { 140.dp.toPx() }.toDouble()
                        val shiftedCenter = mapboxMap.coordinateForPixel(
                            com.mapbox.maps.ScreenCoordinate(centerPx.x, centerPx.y + offsetY)
                        )
                        mapboxMap.setCamera(
                            CameraOptions.Builder()
                                .center(shiftedCenter)
                                .zoom(kotlin.math.max(1.0, mapboxMap.cameraState.zoom - 0.6))
                                .build()
                        )
                        DebugLog.log("Ruta calculada y renderizada")
                    } else {
                        DebugLog.log("Sin coordenadas de ruta válidas")
                    }
                } else {
                    DebugLog.log("Sin rutas devueltas por Directions API")
                }
            } catch (e: Exception) {
                DebugLog.log("Error calculando ruta: ${e.message}")
            }
        }
    }

    val onFirstIndicator = remember {
        object : com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener {
            override fun onIndicatorPositionChanged(point: Point) {
                lastUserLocation = point
                mapboxMap.setCamera(CameraOptions.Builder().center(point).zoom(15.0).build())
                mapView.location.removeOnIndicatorPositionChangedListener(this)
                DebugLog.log("Centrado en posición inicial: ${'$'}point")
            }
        }
    }
    DisposableEffect(mapView) {
        mapView.location.addOnIndicatorPositionChangedListener(onFirstIndicator)
        onDispose {
            mapView.location.removeOnIndicatorPositionChangedListener(onFirstIndicator)
        }
    }

    // Suscripción al current ride del cliente
    DisposableEffect(Unit) {
        val uidNow = FirebaseAuth.getInstance().currentUser?.uid
        var listener: com.google.firebase.database.ValueEventListener? = null
        if (uidNow != null) {
            val q = FirebaseDatabase.getInstance().reference.child("currentRides").orderByChild("userId").equalTo(uidNow)
            listener = object : com.google.firebase.database.ValueEventListener {
                override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                    val first = snapshot.children.firstOrNull()
                    currentRideId = first?.key
                    isCurrentRide = currentRideId != null
                    val dLoc = first?.child("driverLocation")
                    val dlat = dLoc?.child("lat")?.getValue(Double::class.java)
                    val dlon = dLoc?.child("lon")?.getValue(Double::class.java)
                    if (dlat != null && dlon != null) driverLiveLocation = Point.fromLngLat(dlon, dlat)
                    if (currentRideId != null) {
                        val reqId = currentRideRequestId
                        if (reqId != null) {
                            FirebaseDatabase.getInstance().reference.child("rideRequests").child(reqId)
                                .removeValue()
                                .addOnSuccessListener { DebugLog.log("Ride request eliminado al entrar a current ride id=${reqId}") }
                                .addOnFailureListener { e -> DebugLog.log("Error eliminando ride request (current ride): ${e.message}") }
                        }
                        currentRideRequestId = null
                        isSearchingDriver = false
                        isRouteMode = false
                        showSearchBar = true
                        headerVisible = true
                    }
                }
                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
            }
            q.addValueEventListener(listener!!)
        }
        onDispose { if (listener != null) FirebaseDatabase.getInstance().reference.child("currentRides").removeEventListener(listener!!) }
    }

    // Renderizar icono del conductor
    LaunchedEffect(driverLiveLocation) {
        val p = driverLiveLocation
        if (p != null) {
            val srcId = "driver-src"
            val layerId = "driver-layer"
            mapboxMap.getStyle { style ->
                try { style.removeStyleLayer(layerId) } catch (_: Exception) {}
                try { style.removeStyleSource(srcId) } catch (_: Exception) {}
                style.addSource(geoJsonSource(srcId) { feature(Feature.fromGeometry(p)) })
                style.addLayer(
                    circleLayer(layerId, srcId) {
                        circleRadius(7.0)
                        circleColor("#0D9488")
                        circleStrokeColor("#FFFFFF")
                        circleStrokeWidth(2.0)
                    }
                )
            }
        }
    }

    // Actualizar posición del cliente en current ride
    DisposableEffect(currentRideId) {
        val rid = currentRideId
        val listener = object : com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener {
            override fun onIndicatorPositionChanged(point: Point) {
                if (rid != null) {
                    FirebaseDatabase.getInstance().reference.child("currentRides").child(rid)
                        .child("clientLocation").setValue(mapOf("lat" to point.latitude(), "lon" to point.longitude()))
                }
            }
        }
        mapView.location.addOnIndicatorPositionChangedListener(listener)
        onDispose { mapView.location.removeOnIndicatorPositionChangedListener(listener) }
    }
}

@Composable
fun RowQuickActions(places: List<SavedPlace>, onAddPlace: () -> Unit = {}, onPlaceSelected: (SavedPlace) -> Unit = {}) {
    val count = places.size
    Box(modifier = Modifier.fillMaxWidth(0.7f), contentAlignment = Alignment.Center) {
        if (count == 0) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.padding(horizontal = 8.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
            ) {
                QuickActionButton(icon = Icons.Filled.Add, label = "Agregar", onClick = onAddPlace)
            }
        } else if (count <= 3) {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.padding(horizontal = 8.dp),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
            ) {
                places.forEach { p ->
                    QuickActionButton(icon = quickIcon(p.icon), label = displayLabel(p), onClick = { onPlaceSelected(p) })
                }
            }
        } else {
            QuickActionsSlider(places, onPlaceSelected)
        }
    }
}

@Composable
fun QuickActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit = {}) {
    Box(
        modifier = Modifier
            .size(width = 76.dp, height = 68.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.18f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.height(6.dp))
            Text(label, color = Color.White, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun quickIcon(iconStr: String?): ImageVector {
    return when (iconStr) {
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
        else -> Icons.Filled.LocationOn
    }
}

@Composable
private fun displayLabel(place: SavedPlace?): String {
    val ctx = LocalContext.current
    val label = place?.label?.takeIf { it.isNotBlank() }
    val name = place?.name?.takeIf { it.isNotBlank() }
    return label ?: name ?: ctx.getString(com.intu.taxi.R.string.quick_marker)
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun QuickActionsSlider(places: List<SavedPlace>, onPlaceSelected: (SavedPlace) -> Unit) {
    val baseCount = places.size
    val loopFactor = 50
    val totalCount = baseCount * loopFactor
    val listState = rememberLazyListState()
    val snappingLayout = remember(listState) { SnapLayoutInfoProvider(listState) }
    val flingBehavior = rememberSnapFlingBehavior(snappingLayout)
    LazyRow(
        state = listState,
        flingBehavior = flingBehavior,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
    ) {
        items(totalCount) { idx ->
            val baseIndex = idx % baseCount
            val p = places[baseIndex]
            QuickActionButton(icon = quickIcon(p.icon), label = displayLabel(p), onClick = { onPlaceSelected(p) })
        }
    }
}

@Composable
private fun PaymentMethodCompact(current: String, onChange: (String) -> Unit) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val isYape = current == "yape_plin"
        val isCash = current == "efectivo"
        Box(
            modifier = Modifier
                .clip(shape)
                .background(
                    if (isYape) Brush.horizontalGradient(listOf(Color(0xFF0D9488), Color(0xFF14B8A6)))
                    else Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.92f), Color.White.copy(alpha = 0.92f)))
                )
                .border(if (isYape) 0.dp else 1.dp, Color(0xFFE5E7EB), shape)
                .clickable(enabled = !isYape) { onChange("yape_plin") }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(com.intu.taxi.R.string.method_yape_plin),
                color = if (isYape) Color.White else Color(0xFF374151),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isYape) FontWeight.SemiBold else FontWeight.Medium
            )
        }
        Box(
            modifier = Modifier
                .clip(shape)
                .background(
                    if (isCash) Brush.horizontalGradient(listOf(Color(0xFF0F172A), Color(0xFF1F2937)))
                    else Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.92f), Color.White.copy(alpha = 0.92f)))
                )
                .border(if (isCash) 0.dp else 1.dp, Color(0xFFE5E7EB), shape)
                .clickable(enabled = !isCash) { onChange("efectivo") }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(com.intu.taxi.R.string.method_cash),
                color = if (isCash) Color.White else Color(0xFF374151),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isCash) FontWeight.SemiBold else FontWeight.Medium
            )
        }
    }
}

@Composable
private fun PaymentMethodRow(current: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.96f))
            .border(1.dp, Color(0xFFE5E7EB), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF8E44AD)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Place, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
            Text(text = stringResource(com.intu.taxi.R.string.payment_method_label), style = MaterialTheme.typography.bodyMedium, color = Color(0xFF1C1C1E))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(6.dp)) {
            Text(text = if (current == "yape_plin") stringResource(com.intu.taxi.R.string.method_yape_plin) else stringResource(com.intu.taxi.R.string.method_cash), style = MaterialTheme.typography.bodyMedium, color = Color(0xFF374151))
            Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = Color(0xFF6E6E73))
        }
    }
}

@Composable
private fun SearchingDriverCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE5E7EB))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF0D9488), Color(0xFF0F172A)))) ,
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = Color.White)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(com.intu.taxi.R.string.searching_driver_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1C1C1E))
                    Text(text = stringResource(com.intu.taxi.R.string.searching_driver_subtitle), style = MaterialTheme.typography.bodySmall, color = Color(0xFF6E6E73))
                }
            }
            CircularProgressIndicator(color = Color(0xFF0D9488))
        }
    }
}

private const val SEARCH_RADIUS_MILES = 20.0
private const val SEARCH_SUGGESTIONS_LIMIT = 6
private const val HTTP_TIMEOUT_MS = 6000

private fun parseAddressFeature(f: JSONObject): Pair<String, Point>? {
    val addressNum = f.optString("address", "")
    val streetName = f.optString("text", "")
    var city = ""
    val ctx = f.optJSONArray("context")
    if (ctx != null) {
        for (j in 0 until ctx.length()) {
            val c = ctx.optJSONObject(j)
            val id = c?.optString("id", "") ?: ""
            if (id.startsWith("place.") || id.startsWith("locality.")) {
                city = c.optString("text", "")
                break
            }
        }
    }
    val name = listOf(addressNum, streetName, city).filter { it.isNotBlank() }.joinToString(", ")
    val center = f.optJSONArray("center")
    return if (name.isNotBlank() && center != null && center.length() >= 2) {
        val lon = center.optDouble(0)
        val lat = center.optDouble(1)
        name to Point.fromLngLat(lon, lat)
    } else null
}

private suspend fun httpGet(url: String): String {
    val conn = URL(url).openConnection() as HttpURLConnection
    conn.connectTimeout = HTTP_TIMEOUT_MS
    conn.readTimeout = HTTP_TIMEOUT_MS
    conn.requestMethod = "GET"
    conn.doInput = true
    return try {
        conn.inputStream.bufferedReader().use { it.readText() }
    } finally {
        conn.disconnect()
    }
}

private suspend fun geocodeSuggestions(token: String, q: String, center: Point, bbox: String): List<Pair<String, Point>> {
    val url = "https://api.mapbox.com/geocoding/v5/mapbox.places/${q}.json?types=address&autocomplete=true&limit=${SEARCH_SUGGESTIONS_LIMIT}&language=es&proximity=${center.longitude()},${center.latitude()}&bbox=${bbox}&access_token=${token}"
    val json = httpGet(url)
    val obj = JSONObject(json)
    val feats = obj.optJSONArray("features") ?: JSONArray()
    val out = mutableListOf<Pair<String, Point>>()
    for (i in 0 until feats.length()) {
        val f = feats.getJSONObject(i)
        val parsed = parseAddressFeature(f)
        if (parsed != null) out.add(parsed)
    }
    return out
}

private suspend fun reverseGeocode(token: String, p: Point): String? {
    val url = "https://api.mapbox.com/geocoding/v5/mapbox.places/${p.longitude()},${p.latitude()}.json?types=address&language=es&limit=1&access_token=${token}"
    val json = httpGet(url)
    val obj = JSONObject(json)
    val feats = obj.optJSONArray("features") ?: JSONArray()
    if (feats.length() == 0) return null
    val parsed = parseAddressFeature(feats.getJSONObject(0))
    return parsed?.first
}

private fun calcFare(distanceKm: Double?, durationMin: Double?, base: Double, perKm: Double, perMin: Double, multiplier: Double = 1.0): Double {
    val d = kotlin.math.max(0.1, distanceKm ?: 0.0)
    val t = kotlin.math.max(1.0, durationMin ?: 0.0)
    return (base + d * perKm + t * perMin) * multiplier
}
@Composable
private fun RideOptionCard(
    name: String,
    price: Double,
    minutes: Double,
    leadingContent: (@Composable () -> Unit)? = null,
    selected: Boolean = false,
    onClick: () -> Unit = {}
) {
    val priceStr = String.format(Locale.getDefault(), "$%.2f", price)
    val etaMin = kotlin.math.max(1.0, minutes)
    val etaStr = String.format(Locale.getDefault(), "~%.0f min", etaMin)

    val cardColors = when (name) {
        "Intu Honda" -> listOf(Color(0xFF08817E), Color(0xFF0FB9B1))
        "Intu Bajaj" -> listOf(Color(0xFF1E1F47), Color(0xFF3A3B7B))
        "Intu Colectivo" -> listOf(Color(0xFF27AE60), Color(0xFF2ECC71))
        "espera y ahorra" -> listOf(Color(0xFFF39C12), Color(0xFFE67E22))
        "entrega de paquete" -> listOf(Color(0xFF8E44AD), Color(0xFF9B59B6))
        else -> listOf(Color(0xFF08817E), Color(0xFF1E1F47))
    }

    val scale by animateFloatAsState(
        targetValue = if (selected) 1.02f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "cardScale"
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (selected) 0.3f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "glowAlpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = if (selected) 0.95f else 0.8f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (selected) 12.dp else 4.dp
        ),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            brush = Brush.linearGradient(
                colors = if (selected) cardColors else listOf(Color(0xFFE0E0E0), Color(0xFFE0E0E0))
            )
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            cardColors[0].copy(alpha = glowAlpha),
                            Color.Transparent
                        ),
                        center = Offset(0.8f, 0.2f),
                        radius = 200f
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    colors = cardColors,
                                    start = Offset(0f, 0f),
                                    end = Offset(100f, 100f)
                                )
                            )
                            .border(
                                width = 2.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.6f),
                                        Color.White.copy(alpha = 0.1f)
                                    )
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (leadingContent != null) {
                            leadingContent()
                        } else {
                            Icon(
                                Icons.Outlined.Place,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = name.replaceFirstChar { it.titlecase() },
                            style = MaterialTheme.typography.titleMedium,
                            color = Color(0xFF1C1C1E),
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Outlined.Schedule,
                                contentDescription = null,
                                tint = Color(0xFF6E6E73),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = etaStr,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF6E6E73)
                            )
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = priceStr,
                        style = MaterialTheme.typography.titleLarge,
                        color = cardColors[0],
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (selected) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = "Seleccionado",
                                tint = cardColors[0],
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Seleccionado",
                                style = MaterialTheme.typography.labelSmall,
                                color = cardColors[0],
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
data class RideOptionData(
    val name: String,
    val price: Double,
    val minutes: Double,
    val leadingContent: (@Composable () -> Unit)? = null,
    val colors: List<Color>
)

@Composable
private fun RideOptionSlideCard(
    option: RideOptionData,
    isSelected: Boolean,
    onClick: () -> Unit,
    index: Int
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 0.95f,
        animationSpec = tween(durationMillis = 300),
        label = "slideScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isSelected) 1f else 0.7f,
        animationSpec = tween(durationMillis = 300),
        label = "slideAlpha"
    )
    Card(
        modifier = Modifier
            .width(200.dp)
            .fillMaxHeight()
            .scale(scale)
            .alpha(alpha)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = if (isSelected) 0.95f else 0.8f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 16.dp else 8.dp
        ),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 3.dp else 1.dp,
            brush = Brush.linearGradient(
                colors = if (isSelected) option.colors else listOf(Color(0xFFE0E0E0), Color(0xFFE0E0E0))
            )
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    contentAlignment = Alignment.TopEnd
                ) {
                    if (isSelected) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = "Seleccionado",
                            tint = option.colors[0],
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                Text(
                    text = option.name.replaceFirstChar { it.titlecase() },
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF1C1C1E),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        brush = Brush.linearGradient(
                            colors = option.colors,
                            start = Offset(0f, 0f),
                            end = Offset(100f, 100f)
                        )
                    )
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.4f),
                                Color.White.copy(alpha = 0.1f)
                            )
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                option.leadingContent?.invoke() ?: Icon(
                    Icons.Outlined.Place,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val priceStr = String.format(Locale.getDefault(), "$%.2f", option.price)
                val etaMin = kotlin.math.max(1.0, option.minutes)
                val etaStr = String.format(Locale.getDefault(), "~%.0f min", etaMin)
                Text(
                    text = priceStr,
                    style = MaterialTheme.typography.headlineSmall,
                    color = option.colors[0],
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(
                        Icons.Outlined.Schedule,
                        contentDescription = null,
                        tint = Color(0xFF6E6E73),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = etaStr, style = MaterialTheme.typography.bodySmall, color = Color(0xFF6E6E73))
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RideOptionsSlider(
    options: List<RideOptionData>,
    selectedOptionName: String?,
    onOptionSelected: (String) -> Unit,
    initialSelectedIndex: Int = 0
) {
    val listState = rememberLazyListState()
    val snappingLayout = remember(listState) { SnapLayoutInfoProvider(listState) }
    val flingBehavior = rememberSnapFlingBehavior(snappingLayout)
    val density = LocalDensity.current
    val baseCount = options.size
    val loopFactor = 50
    val totalCount = baseCount * loopFactor
    val startIndex = (baseCount * loopFactor / 2) + (initialSelectedIndex % baseCount)
    LaunchedEffect(Unit) {
        if (baseCount > 0) {
            listState.scrollToItem(startIndex)
            val layoutInfo = listState.layoutInfo
            val viewportCenter = layoutInfo.viewportEndOffset / 2
            val itemWidthPx = with(density) { 200.dp.roundToPx() }
            val desiredOffset = viewportCenter - (itemWidthPx / 2)
            listState.scrollToItem(startIndex, desiredOffset)
            onOptionSelected(options[initialSelectedIndex % baseCount].name)
        }
    }
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val layoutInfo = listState.layoutInfo
            val viewportCenter = layoutInfo.viewportEndOffset / 2
            var closestItemIndex = 0
            var minDistance = Float.MAX_VALUE
            layoutInfo.visibleItemsInfo.forEach { itemInfo ->
                val itemCenter = itemInfo.offset + itemInfo.size / 2
                val distance = kotlin.math.abs(itemCenter - viewportCenter)
                if (distance < minDistance) {
                    minDistance = distance.toFloat()
                    closestItemIndex = itemInfo.index
                }
            }
            if (baseCount > 0 && closestItemIndex in 0 until totalCount) {
                val baseIndex = closestItemIndex % baseCount
                onOptionSelected(options[baseIndex].name)
            }
        }
    }
    Column {
        LazyRow(
            state = listState,
            flingBehavior = flingBehavior,
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
        ) {
            items(totalCount) { idx ->
                val baseIndex = idx % baseCount
                val option = options[baseIndex]
                RideOptionSlideCard(
                    option = option,
                    isSelected = (selectedOptionName == option.name),
                    onClick = { onOptionSelected(option.name) },
                    index = baseIndex
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center
        ) {
            options.forEachIndexed { index, _ ->
                val isSel = (selectedOptionName == options[index].name)
                val color by animateColorAsState(
                    targetValue = if (isSel) Color(0xFF08817E) else Color(0xFFE0E0E0),
                    animationSpec = tween(durationMillis = 300),
                    label = "indicatorColor"
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (isSel) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
    }
}
