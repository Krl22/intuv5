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
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.HorizontalDivider
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
import com.mapbox.maps.EdgeInsets
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
import java.io.IOException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@SuppressLint("MissingPermission")
@Composable
fun AddPlaceScreen(
    defaultType: String = "other",
    onCancel: () -> Unit = {},
    onPlacePicked: (SavedPlace) -> Unit
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
    var userCountryCode by remember { mutableStateOf("") }

    DisposableEffect(Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        var reg: com.google.firebase.firestore.ListenerRegistration? = null
        if (uid != null) {
            val docRef = FirebaseFirestore.getInstance().collection("users").document(uid)
            reg = docRef.addSnapshotListener { doc, _ ->
                val countryStr = doc?.getString("country")?.lowercase()
                val code = when (countryStr) {
                    "peru" -> "PE"
                    "usa" -> "US"
                    else -> ""
                }
                if (code != userCountryCode) userCountryCode = code
            }
        }
        onDispose { reg?.remove() }
    }

    // Professional Gradient Background
    val bgBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF0D1B2A), // Dark Navy
            Color(0xFF1B263B), // Deep Blue Grey
            Color(0xFF004D40)  // Deep Teal
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
    ) {
        AndroidView(modifier = Modifier.fillMaxSize(), factory = { mapView })

        // Top Bar Gradient Overlay for readability
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF0D1B2A).copy(alpha = 0.9f),
                            Color.Transparent
                        )
                    )
                )
        )

        AnimatedVisibility(visible = headerVisible, enter = fadeIn() + slideInVertically(initialOffsetY = { -it })) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header with Back Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                            tint = Color.White
                        )
                    }
                    Text(
                        text = stringResource(R.string.add_place_title),
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                SearchBar(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    onFocusChange = { focused -> isSearchFocused = focused },
                    placeholderText = stringResource(R.string.search_address_hint),
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
                    SolidCard {
                        Column(modifier = Modifier.padding(8.dp)) {
                            suggestions.take(6).forEach { s ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedPoint = s.second
                                            addressName = s.first
                                            suggestions = emptyList()
                                        }
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = Color(0xFF4DB6AC),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = s.first, 
                                        color = Color.White.copy(alpha = 0.9f),
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                if (s != suggestions.take(6).last()) {
                                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
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
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(48.dp))
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
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
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4DB6AC), // Professional Teal
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(50),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                        modifier = Modifier.height(50.dp)
                    ) {
                        Text(stringResource(R.string.confirm_location), fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        if (selectedPoint != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                SolidCard {
                    Column(
                        modifier = Modifier.padding(20.dp), 
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.place_details_title), 
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = { selectedPoint = null }) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close), tint = Color.White.copy(alpha = 0.7f))
                            }
                        }
                        
                        OutlinedTextField(
                            value = label, 
                            onValueChange = { label = it }, 
                            label = { Text(stringResource(R.string.label_hint), color = Color.White.copy(alpha = 0.7f)) }, 
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White.copy(alpha = 0.05f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF4DB6AC),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                cursorColor = Color(0xFF4DB6AC),
                                focusedLabelColor = Color(0xFF4DB6AC),
                                unfocusedLabelColor = Color.White.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        
                        Text(
                            text = stringResource(R.string.select_icon),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        
                        IconPickerGrid(selected = iconType, onSelect = { iconType = it })
                        
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = { selectedPoint = null },
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                            ) {
                                Text(stringResource(R.string.cancel))
                            }
                            Button(
                                onClick = {
                                    val p = selectedPoint
                                    if (p != null) {
                                        onPlacePicked(SavedPlace(type = defaultType, name = addressName.ifBlank { label }, lat = p.latitude(), lon = p.longitude(), label = label.ifBlank { null }, icon = iconType))
                                    }
                                }, 
                                modifier = Modifier.weight(1f).height(50.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF4DB6AC),
                                    contentColor = Color.White
                                )
                            ) {
                                Text(stringResource(R.string.save), fontWeight = FontWeight.Bold)
                            }
                        }
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
            val h = mapView.height.toDouble()
            val padding = if (h > 0) EdgeInsets(0.0, 0.0, h * 0.5, 0.0) else EdgeInsets(0.0, 0.0, 0.0, 0.0)
            
            mapView.mapboxMap.setCamera(
                CameraOptions.Builder()
                    .center(point)
                    .padding(padding)
                    .zoom(16.5)
                    .build()
            )

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
                val countryParam = if (userCountryCode.isNotBlank()) "&country=${userCountryCode}" else ""
                val url = "https://api.mapbox.com/geocoding/v5/mapbox.places/${enc}.json?types=address,poi&autocomplete=true&limit=6&language=es&proximity=${center.longitude()},${center.latitude()}${countryParam}&access_token=${mapboxToken}"
                val json = httpGet(url)
                val obj = JSONObject(json)
                val feats = obj.optJSONArray("features") ?: JSONArray()
                var result = mutableListOf<Pair<String, Point>>()
                for (i in 0 until feats.length()) {
                    val f = feats.getJSONObject(i)
                    val parsed = parseAddressFeature(f, userCountryCode)
                    if (parsed != null) result.add(parsed)
                }
                if (result.isEmpty()) {
                    val raw = q.lowercase()
                    val wantsSupermarket = listOf("plaza vea", "super", "supermarket", "mercado", "wong", "tienda", "shopping").any { raw.contains(it) }
                    if (wantsSupermarket) {
                        val url2 = "https://api.mapbox.com/geocoding/v5/mapbox.places/${enc}.json?types=poi&autocomplete=true&fuzzyMatch=true&categories=supermarket,grocery,shopping&limit=6&language=es&proximity=${center.longitude()},${center.latitude()}${countryParam}&access_token=${mapboxToken}"
                        val json2 = httpGet(url2)
                        val obj2 = JSONObject(json2)
                        val feats2 = obj2.optJSONArray("features") ?: JSONArray()
                        val result2 = mutableListOf<Pair<String, Point>>()
                        for (i in 0 until feats2.length()) {
                            val f = feats2.getJSONObject(i)
                            val parsed = parseAddressFeature(f, userCountryCode)
                            if (parsed != null) result2.add(parsed)
                        }
                        result = result2
                        DebugLog.log("AddPlace: fallback categorías ${result.size} para '${q}'")
                    }
                    // visible features fallback removed
                }
                suggestions = result
                DebugLog.log("AddPlace: sugerencias ${result.size} para '${q}'")
            } catch (e: kotlinx.coroutines.CancellationException) {
                DebugLog.log("AddPlace: geocoding cancelado para '${q}'")
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
                    val parsed = parseAddressFeature(feats.getJSONObject(0), userCountryCode)
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



@Composable
fun SolidCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0D1B2A)) // Solid Dark Navy
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.1f),
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        content()
    }
}

private fun parseAddressFeature(f: JSONObject, country: String): Pair<String, Point>? {
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
    val name = if (country == "PE") {
        listOf(streetName, addressNum, city).filter { it.isNotBlank() }.joinToString(", ")
    } else {
        listOf(addressNum, streetName, city).filter { it.isNotBlank() }.joinToString(", ")
    }
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
        val code = conn.responseCode
        if (code in 200..299) {
            conn.inputStream.bufferedReader().use { it.readText() }
        } else {
            val err = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            throw IOException("HTTP ${code}: ${err}")
        }
    } finally { conn.disconnect() }
}

// removed visiblePoiSuggestions helper based on rendered features
