package com.intu.taxi.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Locale
import com.google.firebase.Timestamp
import androidx.compose.ui.res.stringResource
import com.intu.taxi.R
import java.net.URL
import org.json.JSONObject
import android.os.Handler
import android.os.Looper
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.ui.text.style.TextOverflow

data class ClientTrip(
    val id: String,
    val date: String,
    val from: String,
    val to: String,
    val price: String,
    val status: String
)

data class DriverTrip(
    val id: String,
    val passenger: String,
    val pickup: String,
    val dropoff: String,
    val fare: String,
    val status: String
)

data class DriverStats(
    val todayEarnings: Double,
    val todayTrips: Int,
    val weeklyEarnings: List<Float>,
    val acceptanceRate: Float,
    val completionRate: Float,
    val demandMatrix: List<List<Float>>
)

private fun formatDate(ts: Timestamp?): String {
    val d = ts?.toDate() ?: java.util.Date()
    return SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(d)
}

private fun formatCoord(lat: Double?, lon: Double?): String {
    val la = lat ?: 0.0
    val lo = lon ?: 0.0
    return String.format(Locale.getDefault(), "%.5f, %.5f", la, lo)
}

private fun reverseGeocodeStreetCity(mapboxToken: String, lon: Double?, lat: Double?): String? {
    val lo = lon ?: return null
    val la = lat ?: return null
    if (mapboxToken.isBlank()) return null
    return try {
        val url = "https://api.mapbox.com/geocoding/v5/mapbox.places/${lo},${la}.json?language=es&limit=1&access_token=${mapboxToken}"
        val json = URL(url).openStream().bufferedReader().use { it.readText() }
        val obj = JSONObject(json)
        val features = obj.optJSONArray("features")
        if (features != null && features.length() > 0) {
            val f = features.getJSONObject(0)
            val street = f.optString("text").trim()
            val number = f.optString("address").trim()
            var city: String? = null
            val ctx = f.optJSONArray("context")
            if (ctx != null) {
                for (i in 0 until ctx.length()) {
                    val c = ctx.getJSONObject(i)
                    val id = c.optString("id")
                    if (id.startsWith("place") || id.startsWith("locality")) {
                        city = c.optString("text").trim()
                        break
                    }
                }
            }
            val firstPart = if (number.isNotBlank() && street.isNotBlank()) "$number $street" else if (street.isNotBlank()) street else null
            val parts = listOfNotNull(firstPart, city)
            if (parts.isNotEmpty()) parts.joinToString(", ") else null
        } else null
    } catch (_: Exception) { null }
}

private fun formatPriceSoles(p: Double?): String {
    val v = p ?: 0.0
    return "S/ " + String.format(Locale.getDefault(), "%.2f", v)
}

private fun anyToDate(any: Any?): java.util.Date? {
    return when (any) {
        is com.google.firebase.Timestamp -> any.toDate()
        is java.util.Date -> any
        is Number -> {
            val ms = any.toLong()
            val millis = if (ms < 1_000_000_000_000L) ms * 1000L else ms
            java.util.Date(millis)
        }
        else -> null
    }
}

private fun formatDateAny(any: Any?): String {
    val d = anyToDate(any) ?: java.util.Date()
    return SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(d)
}

@Composable
fun TripsScreen() {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val mapboxToken = stringResource(id = R.string.mapbox_access_token)
    val driverApproved = remember { mutableStateOf(false) }
    val driverMode = remember { mutableStateOf(false) }
    val headerVisible = remember { mutableStateOf(false) }
    val contentVisible = remember { mutableStateOf(false) }
    val clientTripsState = remember { mutableStateOf<List<ClientTrip>>(emptyList()) }
    val driverTripsState = remember { mutableStateOf<List<DriverTrip>>(emptyList()) }
    var driverStats by remember { mutableStateOf<DriverStats?>(null) }
    DisposableEffect(uid) {
        val db = FirebaseFirestore.getInstance()
        var reg: com.google.firebase.firestore.ListenerRegistration? = null
        if (uid != null) {
            reg = db.collection("users").document(uid).addSnapshotListener { doc, _ ->
                driverApproved.value = doc?.getBoolean("driverApproved") == true
                driverMode.value = doc?.getBoolean("driverMode") == true
                com.intu.taxi.ui.debug.DebugLog.log("TripsScreen: snapshot driverApproved=" + driverApproved.value + ", driverMode=" + driverMode.value)
            }
        }
        onDispose { reg?.remove() }
    }
    LaunchedEffect(Unit) {
        headerVisible.value = true
        delay(200)
        contentVisible.value = true
    }
    LaunchedEffect(uid, driverApproved.value, driverMode.value) {
        val me = uid
        if (me != null) {
            val fs = FirebaseFirestore.getInstance()
            val isDriver = driverApproved.value && driverMode.value
            com.intu.taxi.ui.debug.DebugLog.log("TripsScreen: uid=" + me + ", isDriver=" + isDriver)
            if (mapboxToken.isBlank()) com.intu.taxi.ui.debug.DebugLog.log("TripsScreen: mapbox token vacío, usando coordenadas")
            if (isDriver) {
                com.intu.taxi.ui.debug.DebugLog.log("TripsScreen: consultando services para uid=" + me)
                fs.collection("users").document(me).collection("services").orderBy("completedAt", com.google.firebase.firestore.Query.Direction.DESCENDING).limit(50).get()
                    .addOnSuccessListener { qs ->
                        Thread {
                            val items = qs.documents.mapNotNull { d ->
                                runCatching {
                                    val id = d.id
                                    val passenger = d.getString("clientName") ?: "—"
                                    val oLat = d.getDouble("originLat")
                                    val oLon = d.getDouble("originLon")
                                    val dLat = d.getDouble("destLat")
                                    val dLon = d.getDouble("destLon")
                                    val pickupLabel = reverseGeocodeStreetCity(mapboxToken, oLon, oLat) ?: formatCoord(oLat, oLon)
                                    val dropoffLabel = reverseGeocodeStreetCity(mapboxToken, dLon, dLat) ?: formatCoord(dLat, dLon)
                                    val fare = formatPriceSoles(d.getDouble("price"))
                                    val status = d.getString("status") ?: "—"
                                    DriverTrip(id, passenger, pickupLabel, dropoffLabel, fare, status)
                                }.getOrNull()
                            }
                            com.intu.taxi.ui.debug.DebugLog.log("TripsScreen: services documentos=" + qs.documents.size)
                            val stats = runCatching { computeDriverStats(qs.documents) }.getOrNull()
                            Handler(Looper.getMainLooper()).post {
                                driverTripsState.value = items
                                driverStats = stats
                                if (stats == null) {
                                    com.intu.taxi.ui.debug.DebugLog.log("TripsScreen: computeDriverStats retornó null")
                                } else {
                                    com.intu.taxi.ui.debug.DebugLog.log(
                                        "TripsScreen: stats todayEarnings=" + String.format(Locale.getDefault(), "%.2f", stats.todayEarnings) +
                                                ", todayTrips=" + stats.todayTrips +
                                                ", acceptanceRate=" + String.format(Locale.getDefault(), "%.2f", stats.acceptanceRate) +
                                                ", completionRate=" + String.format(Locale.getDefault(), "%.2f", stats.completionRate)
                                    )
                                }
                            }
                        }.start()
                    }
                    .addOnFailureListener { e ->
                        Handler(Looper.getMainLooper()).post {
                            driverTripsState.value = emptyList()
                            driverStats = null
                        }
                        com.intu.taxi.ui.debug.DebugLog.log("TripsScreen: error leyendo services ${e.message}")
                    }
            } else {
                com.intu.taxi.ui.debug.DebugLog.log("TripsScreen: consultando trips para uid=" + me)
                fs.collection("users").document(me).collection("trips").orderBy("completedAt", com.google.firebase.firestore.Query.Direction.DESCENDING).limit(50).get()
                    .addOnSuccessListener { qs ->
                        Thread {
                            val items = qs.documents.mapNotNull { d ->
                                runCatching {
                                    val id = d.id
                                    val date = formatDateAny(d.get("completedAt") ?: d.get("createdAt"))
                                    val oLat = d.getDouble("originLat")
                                    val oLon = d.getDouble("originLon")
                                    val dLat = d.getDouble("destLat")
                                    val dLon = d.getDouble("destLon")
                                    val fromLabel = reverseGeocodeStreetCity(mapboxToken, oLon, oLat) ?: formatCoord(oLat, oLon)
                                    val toLabel = reverseGeocodeStreetCity(mapboxToken, dLon, dLat) ?: formatCoord(dLat, dLon)
                                    val price = formatPriceSoles(d.getDouble("price"))
                                    val status = d.getString("status") ?: "—"
                                    ClientTrip(id, date, fromLabel, toLabel, price, status)
                                }.getOrNull()
                            }
                            com.intu.taxi.ui.debug.DebugLog.log("TripsScreen: trips documentos=" + qs.documents.size)
                            Handler(Looper.getMainLooper()).post { clientTripsState.value = items }
                        }.start()
                    }
                    .addOnFailureListener { e ->
                        Handler(Looper.getMainLooper()).post { clientTripsState.value = emptyList() }
                        com.intu.taxi.ui.debug.DebugLog.log("TripsScreen: error leyendo trips ${e.message}")
                    }
            }
        } else {
            com.intu.taxi.ui.debug.DebugLog.log("TripsScreen: uid es null, no se consulta Firestore")
        }
    }
    val isDriver = driverApproved.value && driverMode.value
    val bg = Brush.verticalGradient(listOf(Color(0xFF08817E).copy(alpha = 0.1f), Color(0xFF1E1F47).copy(alpha = 0.05f), MaterialTheme.colorScheme.surface))
    Box(Modifier.fillMaxSize().background(bg)) {
        Column(Modifier.fillMaxSize()) {
            AnimatedVisibility(visible = headerVisible.value, enter = fadeIn() + slideInVertically(initialOffsetY = { -it })) {
                EnhancedHeader(isDriver)
            }
            AnimatedVisibility(visible = contentVisible.value, enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 })) {
                if (isDriver) DriverContent(driverTripsState.value, driverStats) else ClientContent(clientTripsState.value)
            }
        }
    }
}

@Composable
private fun EnhancedHeader(isDriver: Boolean) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)), shape = RoundedCornerShape(20.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(text = if (isDriver) stringResource(R.string.my_services) else stringResource(R.string.my_trips), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color(0xFF1C1C1E))
                Text(text = if (isDriver) stringResource(R.string.services_this_month) else stringResource(R.string.trips_this_month), style = MaterialTheme.typography.bodyMedium, color = Color(0xFF6B7280))
            }
            Box(modifier = Modifier.size(56.dp).clip(RoundedCornerShape(28.dp)).background(Brush.radialGradient(listOf(Color(0xFF08817E), Color(0xFF1E1F47)))), contentAlignment = Alignment.Center) {
                Icon(if (isDriver) Icons.Filled.DirectionsCar else Icons.Filled.Person, contentDescription = null, tint = Color.White)
            }
        }
    }
}

@Composable
private fun ClientContent(trips: List<ClientTrip>) {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        items(trips) { trip -> ClientTripCard(trip) }
    }
}

@Composable
private fun DriverContent(trips: List<DriverTrip>, stats: DriverStats?) {
    var selectedTab = remember { mutableStateOf(0) }
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.8f)), shape = RoundedCornerShape(16.dp)) {
                TabRow(selectedTabIndex = selectedTab.value, containerColor = Color.Transparent) {
                    Tab(selected = selectedTab.value == 0, onClick = { selectedTab.value = 0 }, text = { Text(stringResource(R.string.services_tab)) })
                    Tab(selected = selectedTab.value == 1, onClick = { selectedTab.value = 1 }, text = { Text(stringResource(R.string.dashboard_tab)) })
                }
            }
        }
        item { Spacer(Modifier.height(4.dp)) }
        if (selectedTab.value == 0) {
            items(trips) { trip -> DriverTripCard(trip) }
        } else {
            item { DriverDashboard(stats) }
        }
    }
}

@Composable
private fun ClientTripCard(trip: ClientTrip) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.animateContentSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(24.dp)).background(Brush.radialGradient(listOf(Color(0xFF08817E), Color(0xFF1E1F47)))), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = Color.White)
                }
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f)) {
                    val density = LocalDensity.current
                    var fromWidth by remember { mutableStateOf(0f) }
                    var toWidth by remember { mutableStateOf(0f) }
                    val maxWidthDp = with(density) { kotlin.math.max(fromWidth, toWidth).toDp() }
                    Text(trip.date, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1C1C1E))
                    Column {
                        Text(
                            trip.from,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6B7280),
                            onTextLayout = { fromWidth = it.size.width.toFloat() }
                        )
                        Box(Modifier.width(maxWidthDp), contentAlignment = Alignment.Center) {
                            Text("↓", style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B7280))
                        }
                        Text(
                            trip.to,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6B7280),
                            onTextLayout = { toWidth = it.size.width.toFloat() }
                        )
                    }
                }
                Text(trip.price, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
            }
        }
    }
}

@Composable
private fun DriverTripCard(trip: DriverTrip) {
    val statusColor = when (trip.status) {
        "Pendiente" -> Color(0xFF08817E)
        "En curso" -> Color(0xFF1E1F47)
        else -> Color(0xFF08817E)
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.animateContentSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(24.dp)).background(statusColor.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Person, contentDescription = null, tint = statusColor)
                }
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f)) {
                    val density = LocalDensity.current
                    var fromWidth by remember { mutableStateOf(0f) }
                    var toWidth by remember { mutableStateOf(0f) }
                    val maxWidthDp = with(density) { kotlin.math.max(fromWidth, toWidth).toDp() }
                    Text(trip.passenger, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1C1C1E))
                    Column {
                        Text(
                            trip.pickup,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6B7280),
                            onTextLayout = { fromWidth = it.size.width.toFloat() }
                        )
                        Box(Modifier.width(maxWidthDp), contentAlignment = Alignment.Center) {
                            Text("↓", style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B7280))
                        }
                        Text(
                            trip.dropoff,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF6B7280),
                            onTextLayout = { toWidth = it.size.width.toFloat() }
                        )
                    }
                }
                Text(trip.fare, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).clip(RoundedCornerShape(4.dp)).background(statusColor))
                Spacer(Modifier.size(4.dp))
                Text(trip.status, style = MaterialTheme.typography.bodySmall, color = statusColor)
            }
        }
    }
}

@Composable
private fun DriverDashboard(stats: DriverStats?) {
    val s = stats
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val todayValue = if (s != null) "S/ ${String.format("%.2f", s.todayEarnings)}" else "S/ —"
            val tripsValue = if (s != null) s.todayTrips.toString() else "—"
            MetricTile(modifier = Modifier.weight(1f), title = "Ingresos hoy", value = todayValue, accent = Color(0xFF10B981))
            MetricTile(modifier = Modifier.weight(1f), title = "Viajes", value = tripsValue, accent = Color(0xFF1E88E5))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GlowRing(modifier = Modifier.weight(1f), title = "Aceptación", progress = (s?.acceptanceRate ?: 0f), color = Color(0xFF7C4DFF))
            GlowRing(modifier = Modifier.weight(1f), title = "Finalización", progress = (s?.completionRate ?: 0f), color = Color(0xFFFF6D00))
        }
        Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Ingresos semanales", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
                Spacer(Modifier.height(8.dp))
                val week = s?.weeklyEarnings ?: listOf(0f,0f,0f,0f,0f,0f,0f)
                var selected by remember { mutableStateOf(6f) }
                AreaChart(values = week, accent = Brush.horizontalGradient(listOf(Color(0xFF00E5C3), Color(0xFF7C4DFF))))
                Spacer(Modifier.height(8.dp))
                Slider(value = selected, onValueChange = { selected = it }, valueRange = 0f..6f, steps = 5, colors = SliderDefaults.colors(thumbColor = Color(0xFF00E5C3), activeTrackColor = Color(0xFF7C4DFF)))
                val idx = selected.toInt().coerceIn(0, week.size - 1)
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Día ${idx + 1}", color = Color(0xFF6B7280))
                    Text("S/ ${String.format("%.2f", week[idx])}", fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                }
            }
        }
        Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text("Demanda por hora", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
                Spacer(Modifier.height(12.dp))
                DemandHeatmap(matrix = s?.demandMatrix ?: listOf(
                    listOf(0f,0f,0f,0f,0f,0f),
                    listOf(0f,0f,0f,0f,0f,0f),
                    listOf(0f,0f,0f,0f,0f,0f),
                    listOf(0f,0f,0f,0f,0f,0f),
                    listOf(0f,0f,0f,0f,0f,0f)
                ))
            }
        }
    }
}

private fun computeDriverStats(docs: List<com.google.firebase.firestore.DocumentSnapshot>): DriverStats {
    val now = java.util.Calendar.getInstance()
    fun sameDay(a: java.util.Date, b: java.util.Date): Boolean {
        val ca = java.util.Calendar.getInstance().apply { time = a }
        val cb = java.util.Calendar.getInstance().apply { time = b }
        return ca.get(java.util.Calendar.YEAR) == cb.get(java.util.Calendar.YEAR) && ca.get(java.util.Calendar.DAY_OF_YEAR) == cb.get(java.util.Calendar.DAY_OF_YEAR)
    }
    val today = now.time
    var todayEarnings = 0.0
    var todayTrips = 0
    val last7Days = (0..6).map { d ->
        java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -d) }.time
    }.reversed()
    val dailySums = MutableList(last7Days.size) { 0.0 }
    var acceptedCount = 0
    var arrivedCount = 0
    var consideredCount = 0
    val bucketHours = listOf(0..3, 4..7, 8..11, 12..15, 16..19, 20..23)
    val heatDays = (0..4).map { d -> java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -d) }.time }.reversed()
    val heatCounts = Array(heatDays.size) { IntArray(bucketHours.size) { 0 } }
    docs.forEach { d ->
        val completed = anyToDate(d.get("completedAt")) ?: anyToDate(d.get("createdAt"))
        val price = ((d.get("price") as? Number)?.toDouble()) ?: (d.getDouble("price") ?: 0.0)
        if (completed != null) {
            if (sameDay(completed, today)) {
                todayEarnings += price
                todayTrips += 1
            }
            last7Days.forEachIndexed { idx, date ->
                if (sameDay(completed, date)) dailySums[idx] += price
            }
            val vid = anyToDate(d.get("verifiedAt"))
            val arr = anyToDate(d.get("arrivedAt"))
            acceptedCount += if (vid != null) 1 else 0
            arrivedCount += if (arr != null) 1 else 0
            consideredCount += 1
            heatDays.forEachIndexed { r, day ->
                if (sameDay(completed, day)) {
                    val cal = java.util.Calendar.getInstance().apply { time = completed }
                    val hour = cal.get(java.util.Calendar.HOUR_OF_DAY)
                    val cIdx = bucketHours.indexOfFirst { hour in it }
                    if (cIdx >= 0) heatCounts[r][cIdx]++
                }
            }
        }
    }
    val weekly = dailySums.map { it.toFloat() }
    val accRate = if (consideredCount > 0) acceptedCount.toFloat() / consideredCount else 0f
    val compRate = if (consideredCount > 0) arrivedCount.toFloat() / consideredCount else 0f
    var maxHeat = 1
    heatCounts.forEach { row -> row.forEach { v -> if (v > maxHeat) maxHeat = v } }
    val matrix = heatCounts.map { row -> row.map { v -> v.toFloat() / maxHeat } }
    return DriverStats(todayEarnings = todayEarnings, todayTrips = todayTrips, weeklyEarnings = weekly, acceptanceRate = accRate, completionRate = compRate, demandMatrix = matrix)
}

@Composable
private fun MetricTile(modifier: Modifier = Modifier, title: String, value: String, accent: Color) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B7280), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = accent)
        }
    }
}

@Composable
private fun GlowRing(modifier: Modifier = Modifier, title: String, progress: Float, color: Color) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)), shape = RoundedCornerShape(16.dp)) {
        Box(Modifier.padding(16.dp)) {
            val p = progress.coerceIn(0f, 1f)
            Canvas(Modifier.size(140.dp)) {
                val baseStroke = 12f
                val arcStroke = 14f
                val radius = size.minDimension / 2f - arcStroke / 2f
                val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
                drawCircle(color.copy(alpha = 0.08f), radius = radius + arcStroke / 2f, center = center)
                drawCircle(color.copy(alpha = 0.18f), radius = radius, center = center, style = Stroke(width = baseStroke))
                val topLeft = androidx.compose.ui.geometry.Offset(center.x - radius, center.y - radius)
                val sz = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f)
                drawArc(color, startAngle = -90f, sweepAngle = 360f * p, useCenter = false, topLeft = topLeft, size = sz, style = Stroke(width = arcStroke, cap = StrokeCap.Round))
            }
            Column(Modifier.align(Alignment.Center)) {
                Text(String.format("%d%%", (p * 100).toInt()), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
                Text(title, style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B7280))
            }
        }
    }
}

@Composable
private fun AreaChart(values: List<Float>, accent: Brush) {
    val max = (values.maxOrNull() ?: 1f).coerceAtLeast(1f)
    Canvas(Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF9FAFB))) {
        val w = size.width
        val h = size.height
        val stepX = if (values.size > 1) w / (values.size - 1) else w
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = i * stepX
            val y = h - (v / max) * (h * 0.85f)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        val fill = Path()
        fill.addPath(path)
        fill.lineTo(w, h)
        fill.lineTo(0f, h)
        drawPath(fill, brush = accent)
        drawPath(path, color = Color(0xFF7C4DFF), style = Stroke(width = 4f))
    }
}

@Composable
private fun DemandHeatmap(matrix: List<List<Float>>) {
    val rows = matrix.size
    val cols = matrix.firstOrNull()?.size ?: 0
    Canvas(Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFF9FAFB))) {
        val cellW = size.width / cols.coerceAtLeast(1)
        val cellH = size.height / rows.coerceAtLeast(1)
        matrix.forEachIndexed { r, row ->
            row.forEachIndexed { c, v ->
                val x = c * cellW
                val y = r * cellH
                val col = Brush.verticalGradient(listOf(Color(0xFF00E5C3).copy(alpha = 0.25f + 0.5f * v), Color(0xFF1E1F47).copy(alpha = 0.15f + 0.4f * v)))
                drawRect(brush = col, topLeft = androidx.compose.ui.geometry.Offset(x, y), size = androidx.compose.ui.geometry.Size(cellW - 4f, cellH - 4f))
            }
        }
    }
}
