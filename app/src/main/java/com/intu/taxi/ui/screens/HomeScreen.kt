package com.intu.taxi.ui.screens

import android.annotation.SuppressLint
import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
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
import androidx.compose.runtime.SideEffect
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.maps.plugin.locationcomponent.location
import androidx.compose.material3.OutlinedTextField
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

    // Hide ScaleBar overlay (top-left)
    mapView.scalebar.enabled = false

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

    if (!grantedFine && !grantedCoarse) {
        SideEffect {
            locationPermissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    } else {
        mapView.location.updateSettings { enabled = true }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { mapView }
        )

        var headerVisible by remember { mutableStateOf(false) }
        var searchQuery by remember { mutableStateOf("") }
        var isSearchFocused by remember { mutableStateOf(false) }
        var firstName by remember { mutableStateOf("") }
        LaunchedEffect(Unit) {
            val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) FirebaseFirestore.getInstance().collection("users").document(uid).get().addOnSuccessListener { doc ->
                firstName = doc.getString("firstName") ?: ""
            }
        }
        LaunchedEffect(Unit) { headerVisible = true }

        val headerShiftFraction by animateFloatAsState(
            targetValue = if (isSearchFocused) 0.5f else 0f,
            animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
            label = "headerShiftFraction"
        )
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
                visible = headerVisible,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it })
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 20.dp, start = 16.dp, end = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
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
                    HeaderSearchBar(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        onFocusChange = { focused -> isSearchFocused = focused },
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(25.dp))
                    // Dropdown de sugerencias bajo el campo de búsqueda
                    val suggestions = listOf(
                        Pair("Aeropuerto Jorge Chávez", Point.fromLngLat(-77.1144, -12.0219)),
                        Pair("Centro de Lima", Point.fromLngLat(-77.0311, -12.0464))
                    ).filter { searchQuery.length >= 2 }
                    if (suggestions.isNotEmpty()) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .offset(y = 56.dp)
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
                                            },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = s.first, color = Color(0xFF111827))
                                    }
                                }
                            }
                        }
                    }
                    RowQuickActions()
                }
            }
        }
    }

    // Load style once and center on first location update
    val mapboxMap = mapView.mapboxMap
    mapboxMap.loadStyleUri(Style.STANDARD) { _ ->
        DebugLog.log("Mapa cargó estilo MAPBOX_STANDARD")
    }

    val onFirstIndicator = remember {
        object : com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener {
            override fun onIndicatorPositionChanged(point: Point) {
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
private fun RowQuickActions() {
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
private fun QuickActionButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
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

@Composable
private fun HeaderSearchBar(
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
                    Icon(Icons.Filled.Mic, contentDescription = null, tint = Color(0xFF8E8E93))
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
