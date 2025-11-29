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

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { mapView }
        )

        var headerVisible by remember { mutableStateOf(false) }
        var firstName by remember { mutableStateOf("") }
        LaunchedEffect(Unit) {
            headerVisible = true
            val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            if (uid != null) {
                FirebaseFirestore.getInstance().collection("users").document(uid).get().addOnSuccessListener { doc ->
                    firstName = doc.getString("firstName") ?: ""
                }
            }
        }

        val headerShiftFraction = 0f
        var isSearching by remember { mutableStateOf(false) }
        val buttonTravelFraction by animateFloatAsState(targetValue = if (isSearching) 0.95f else 0f, animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing), label = "buttonTravelFraction")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.6f)
                .padding(top = 0.dp)
                .then(
                    Modifier
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
                                        blendMode = BlendMode.DstIn
                                    )
                                }
                            }
                        }
                ),
            contentAlignment = Alignment.TopCenter
        ) {
            AnimatedVisibility(
                visible = headerVisible,
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
                AnimatedGradientButton(
                    isSearching = isSearching,
                    onClick = { isSearching = !isSearching }
                )
            }
        }
    }

    val mapboxMap = mapView.mapboxMap
    mapboxMap.loadStyleUri(Style.STANDARD) { _ ->
        DebugLog.log("Mapa cargó estilo MAPBOX_STANDARD")
    }

    val onFirstIndicator = remember {
        object : com.mapbox.maps.plugin.locationcomponent.OnIndicatorPositionChangedListener {
            override fun onIndicatorPositionChanged(point: Point) {
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
