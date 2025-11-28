package com.intu.taxi.ui.screens

import android.annotation.SuppressLint
import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
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

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { mapView }
    )

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
