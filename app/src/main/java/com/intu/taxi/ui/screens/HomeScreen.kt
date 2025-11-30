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
import com.mapbox.geojson.Point
import com.mapbox.geojson.Feature
import com.mapbox.geojson.LineString
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.maps.plugin.locationcomponent.location
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalFocusManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL
import java.net.URLEncoder
import org.json.JSONObject
import org.json.JSONArray

@Composable
fun SearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit = {},
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
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Color(0xFF8E8E93))
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
    var lastUserLocation by remember { mutableStateOf<Point?>(null) }
    val mapboxPublicToken = stringResource(id = com.intu.taxi.R.string.mapbox_access_token)
    val rootView = LocalView.current
    val focusManager = LocalFocusManager.current
    var imeVisible by remember { mutableStateOf(false) }
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

        LaunchedEffect(Unit) {
            val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) FirebaseFirestore.getInstance().collection("users").document(uid).get().addOnSuccessListener { doc ->
                firstName = doc.getString("firstName") ?: ""
            }
        }
        LaunchedEffect(Unit) { headerVisible = true }
        LaunchedEffect(imeVisible) {
            if (showSearchBar && !isRouteMode) {
                isSearchFocused = imeVisible
                if (!imeVisible) suggestions = emptyList()
            }
        }
        LaunchedEffect(searchQuery) {
            if (mapboxPublicToken.isBlank()) {
                suggestions = emptyList()
            } else if (searchQuery.trim().length >= 2) {
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
                    val url = "https://api.mapbox.com/geocoding/v5/mapbox.places/${q}.json?types=address&autocomplete=true&limit=${SEARCH_SUGGESTIONS_LIMIT}&language=es&proximity=${lon},${lat}&bbox=${bbox}&access_token=${mapboxPublicToken}"
                    val json = withContext(Dispatchers.IO) {
                        URL(url).openStream().bufferedReader().use { it.readText() }
                    }
                    val obj = JSONObject(json)
                    val feats = obj.optJSONArray("features") ?: JSONArray()
                    val result = mutableListOf<Pair<String, Point>>()
                    for (i in 0 until feats.length()) {
                        val f = feats.getJSONObject(i)
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
                        if (name.isNotBlank() && center != null && center.length() >= 2) {
                            val lon = center.optDouble(0)
                            val lat = center.optDouble(1)
                            result.add(name to Point.fromLngLat(lon, lat))
                        }
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
            targetValue = if (isSearchFocused) 0.5f else 0f,
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            label = "headerShiftFraction"
        )
        if (!isRouteMode) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.55f)
                    .drawBehind {
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
                    },
                contentAlignment = Alignment.TopCenter
            ) {
            AnimatedVisibility(
                visible = headerVisible && !isRouteMode,
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
                        visible = !isSearchFocused,
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
                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                text = if (firstName.isNotBlank()) "¡Hola $firstName!" else "¡Hola!",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge,
                                fontSize = 24.sp
                            )
                            Spacer(modifier = Modifier.height(40.dp))
                        }
                    }
                    if (showSearchBar) {
                          SearchBar(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            onFocusChange = { focused -> isSearchFocused = focused },
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                    }
                    if (showSearchBar && suggestions.isNotEmpty()) {
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
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    AnimatedVisibility(
                        visible = !isSearchFocused,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        RowQuickActions()
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
                Button(
                    onClick = {
                        // Remove route and destination visuals
                        mapView.mapboxMap.getStyle { style ->
                            try { style.removeStyleLayer("route-layer") } catch (_: Exception) {}
                            try { style.removeStyleSource("route-src") } catch (_: Exception) {}
                            try { style.removeStyleLayer("dest-layer") } catch (_: Exception) {}
                            try { style.removeStyleSource("dest-src") } catch (_: Exception) {}
                        }
                        // Reset UI state
                        isRouteMode = false
                        showSearchBar = true
                        headerVisible = true
                        selectedDestination = null
                        searchQuery = ""
                        suggestions = emptyList()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.95f)),
                    shape = RoundedCornerShape(50)
                ) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = null, tint = Color(0xFF111827))
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
        BackHandler(enabled = isSearchFocused && !isRouteMode) {
            focusManager.clearFocus()
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
}

@Composable
fun RowQuickActions() {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.padding(horizontal = 8.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
        ) {
            QuickActionButton(icon = androidx.compose.material.icons.Icons.Filled.Home, label = stringResource(id = com.intu.taxi.R.string.quick_home))
            QuickActionButton(icon = androidx.compose.material.icons.Icons.Filled.Work, label = stringResource(id = com.intu.taxi.R.string.quick_work))
            QuickActionButton(icon = androidx.compose.material.icons.Icons.Filled.LocationOn, label = stringResource(id = com.intu.taxi.R.string.quick_marker))
        }
    }
}

@Composable
fun QuickActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Box(
        modifier = Modifier
            .size(width = 76.dp, height = 68.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.18f))
            .clickable { },
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

private const val SEARCH_RADIUS_MILES = 20.0
private const val SEARCH_SUGGESTIONS_LIMIT = 6
