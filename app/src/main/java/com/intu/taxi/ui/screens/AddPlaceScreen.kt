package com.intu.taxi.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.ui.res.stringResource
import com.mapbox.maps.plugin.locationcomponent.location
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.intu.taxi.R
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.maps.plugin.logo.logo
import com.mapbox.maps.plugin.attribution.attribution
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.layers.generated.circleLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.layers.addLayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import com.intu.taxi.ui.debug.DebugLog

@SuppressLint("MissingPermission")
@Composable
fun AddPlaceScreen(
    defaultType: String = "other",
    onPlacePicked: (SavedPlace) -> Unit,
) {
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

    val grantedFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val grantedCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val locationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        val ok = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true || perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (ok) mapView.location.updateSettings { enabled = true }
    }
    LaunchedEffect(Unit) {
        if (!grantedFine && !grantedCoarse) {
            locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        } else {
            mapView.location.updateSettings { enabled = true }
        }
    }

    var headerVisible by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var suggestions by remember { mutableStateOf<List<Pair<String, Point>>>(emptyList()) }
    var selectedPoint by remember { mutableStateOf<Point?>(null) }
    var addressName by remember { mutableStateOf("") }
    var label by remember { mutableStateOf("") }
    var iconType by remember { mutableStateOf("marker") }
    val mapboxToken = stringResource(id = R.string.mapbox_access_token)
    var isSearchFocused by remember { mutableStateOf(false) }
    var isPinMode by remember { mutableStateOf(false) }
    var pinCenter by remember { mutableStateOf<Point?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(modifier = Modifier.fillMaxSize(), factory = { mapView })

        AnimatedVisibility(visible = headerVisible, enter = fadeIn() + slideInVertically(initialOffsetY = { -it })) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 20.dp, start = 16.dp, end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Agregar lugar", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                SearchBar(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    onFocusChange = { focused -> isSearchFocused = focused },
                    placeholderText = "Buscar dirección",
                    showClearButton = true,
                    onClearClick = { searchQuery = ""; suggestions = emptyList() },
                    showMicButton = true,
                    onPinClick = {
                        isPinMode = true
                        isSearchFocused = false
                        suggestions = emptyList()
                        pinCenter = mapView.mapboxMap.cameraState.center
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                if (!isPinMode && suggestions.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(4.dp, RoundedCornerShape(12.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            suggestions.take(6).forEach { s ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp)
                                    .clickable {
                                        selectedPoint = s.second
                                        addressName = s.first
                                        mapView.mapboxMap.setCamera(CameraOptions.Builder().center(s.second).zoom(15.0).build())
                                        suggestions = emptyList()
                                    },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = s.first, color = Color(0xFF111827))
                                }
                            }
                        }
                    }
                }
            }
        }

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
                                selectedPoint = center
                                addressName = searchQuery
                                isPinMode = false
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

        if (selectedPoint != null) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = "Detalles del lugar", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("Etiqueta (ej. Casa, Trabajo)") }, modifier = Modifier.fillMaxWidth())
                    IconPickerGrid(selected = iconType, onSelect = { iconType = it })
                    Button(onClick = {
                        val p = selectedPoint
                        if (p != null) {
                            onPlacePicked(SavedPlace(type = defaultType, name = addressName.ifBlank { label }, lat = p.latitude(), lon = p.longitude(), label = label.ifBlank { null }, icon = iconType))
                        }
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text("Guardar")
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        mapView.mapboxMap.loadStyleUri(Style.STANDARD) {}
    }

    val onFirstIndicator = remember {
        object : OnIndicatorPositionChangedListener {
            override fun onIndicatorPositionChanged(point: Point) {
                mapView.mapboxMap.setCamera(
                    CameraOptions.Builder()
                        .center(point)
                        .zoom(15.0)
                        .build()
                )
                mapView.location.removeOnIndicatorPositionChangedListener(this)
            }
        }
    }
    DisposableEffect(mapView) {
        mapView.location.addOnIndicatorPositionChangedListener(onFirstIndicator)
        onDispose { mapView.location.removeOnIndicatorPositionChangedListener(onFirstIndicator) }
    }

    LaunchedEffect(selectedPoint) {
        val srcId = "addplace-dest-src"
        val layerId = "addplace-dest-layer"
        val point = selectedPoint
        if (point != null) {
            mapView.mapboxMap.getStyle { style ->
                try { style.removeStyleLayer(layerId) } catch (_: Exception) {}
                try { style.removeStyleSource(srcId) } catch (_: Exception) {}
                style.addSource(geoJsonSource(srcId) { feature(com.mapbox.geojson.Feature.fromGeometry(point)) })
                style.addLayer(
                    circleLayer(layerId, srcId) {
                        circleRadius(8.0)
                        circleColor("#EF4444")
                        circleStrokeColor("#FFFFFF")
                        circleStrokeWidth(2.0)
                    }
                )
            }
        }
    }

    LaunchedEffect(searchQuery) {
        val q = searchQuery.trim()
        if (!isPinMode && q.length >= 2 && mapboxToken.isNotBlank()) {
            try {
                delay(250)
                val enc = URLEncoder.encode(q, "UTF-8")
                val center = mapView.mapboxMap.cameraState.center
                val url = "https://api.mapbox.com/geocoding/v5/mapbox.places/${enc}.json?types=address&autocomplete=true&limit=6&language=es&proximity=${center.longitude()},${center.latitude()}&access_token=${mapboxToken}"
                val json = httpGet(url)
                val obj = JSONObject(json)
                val feats = obj.optJSONArray("features") ?: JSONArray()
                val result = mutableListOf<Pair<String, Point>>()
                for (i in 0 until feats.length()) {
                    val f = feats.getJSONObject(i)
                    val parsed = parseAddressFeature(f)
                    if (parsed != null) result.add(parsed)
                }
                suggestions = result
                DebugLog.log("AddPlace: sugerencias ${result.size} para '${q}'")
            } catch (_: Exception) {
                DebugLog.log("AddPlace: error geocoding para '${q}'")
                suggestions = emptyList()
            }
        } else {
            if (q.length < 2) DebugLog.log("AddPlace: query demasiado corta")
            if (mapboxToken.isBlank()) DebugLog.log("AddPlace: token Mapbox vacío")
            suggestions = emptyList()
        }
    }

    LaunchedEffect(isPinMode) {
        while (isPinMode) {
            pinCenter = mapView.mapboxMap.cameraState.center
            delay(300)
        }
    }

    LaunchedEffect(pinCenter, isPinMode) {
        val p = pinCenter
        if (isPinMode && p != null && mapboxToken.isNotBlank()) {
            try {
                delay(200)
                val url = "https://api.mapbox.com/geocoding/v5/mapbox.places/${p.longitude()},${p.latitude()}.json?types=address&language=es&limit=1&access_token=${mapboxToken}"
                val json = httpGet(url)
                val obj = JSONObject(json)
                val feats = obj.optJSONArray("features") ?: JSONArray()
                if (feats.length() > 0) {
                    val parsed = parseAddressFeature(feats.getJSONObject(0))
                    val name = parsed?.first
                    if (!name.isNullOrBlank()) {
                        searchQuery = name
                        DebugLog.log("AddPlace: pin -> ${name}")
                    }
                }
            } catch (e: Exception) {
                DebugLog.log("AddPlace: error reverse geocoding pin ${e.message}")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IconPickerGrid(selected: String, onSelect: (String) -> Unit) {
    val options = listOf(
        "home" to Icons.Filled.Home,
        "work" to Icons.Filled.Work,
        "marker" to Icons.Filled.LocationOn,
        "school" to Icons.Filled.School,
        "shopping" to Icons.Filled.LocalMall,
        "food" to Icons.Filled.Restaurant,
        "cafe" to Icons.Filled.LocalCafe,
        "hospital" to Icons.Filled.LocalHospital,
        "package" to Icons.Filled.LocalShipping,
        "star" to Icons.Filled.Star,
        "favorite" to Icons.Filled.Favorite
    )
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (value, icon) ->
            val isSel = selected == value
            OutlinedButton(
                onClick = { onSelect(value) },
                colors = if (isSel) ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF0D9488)) else ButtonDefaults.outlinedButtonColors()
            ) {
                Icon(icon, contentDescription = null)
            }
        }
    }
}

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

private suspend fun httpGet(url: String): String = withContext(Dispatchers.IO) {
    val conn = URL(url).openConnection() as HttpURLConnection
    conn.connectTimeout = 6000
    conn.readTimeout = 6000
    conn.requestMethod = "GET"
    conn.doInput = true
    try {
        conn.inputStream.bufferedReader().use { it.readText() }
    } finally { conn.disconnect() }
}
