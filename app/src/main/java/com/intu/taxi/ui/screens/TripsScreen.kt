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
import androidx.compose.ui.res.stringResource
import com.intu.taxi.R

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

private val clientTrips = listOf(
    ClientTrip("1","Hoy 10:30","Av. Primavera 123","Aeropuerto","S/ 28.50","Completado"),
    ClientTrip("2","Ayer 19:05","Calle Sol 456","Centro Comercial","S/ 15.40","Completado"),
    ClientTrip("3","Mar 14:20","Universidad","Casa","S/ 21.10","Cancelado")
)

private val driverTrips = listOf(
    DriverTrip("1","Ana","Intu Plaza","Av. Centro 77","S/ 12.40","Completado"),
    DriverTrip("2","Luis","Parque Norte","Clínica Central","S/ 9.60","En curso"),
    DriverTrip("3","María","Mercado 9","Intu Mall","S/ 15.10","Pendiente")
)

@Composable
fun TripsScreen() {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val driverApproved = remember { mutableStateOf(false) }
    val driverMode = remember { mutableStateOf(false) }
    val headerVisible = remember { mutableStateOf(false) }
    val contentVisible = remember { mutableStateOf(false) }
    LaunchedEffect(uid) {
        if (uid != null) {
            FirebaseFirestore.getInstance().collection("users").document(uid).get().addOnSuccessListener { doc ->
                driverApproved.value = doc.getBoolean("driverApproved") == true
                driverMode.value = doc.getBoolean("driverMode") == true
            }
        }
        headerVisible.value = true
        delay(200)
        contentVisible.value = true
    }
    val isDriver = driverApproved.value && driverMode.value
    val bg = Brush.verticalGradient(listOf(Color(0xFF08817E).copy(alpha = 0.1f), Color(0xFF1E1F47).copy(alpha = 0.05f), MaterialTheme.colorScheme.surface))
    Box(Modifier.fillMaxSize().background(bg)) {
        Column(Modifier.fillMaxSize()) {
            AnimatedVisibility(visible = headerVisible.value, enter = fadeIn() + slideInVertically(initialOffsetY = { -it })) {
                EnhancedHeader(isDriver)
            }
            AnimatedVisibility(visible = contentVisible.value, enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 })) {
                if (isDriver) DriverContent() else ClientContent()
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
private fun ClientContent() {
    LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        items(clientTrips) { trip -> ClientTripCard(trip) }
    }
}

@Composable
private fun DriverContent() {
    var selectedTab = remember { mutableStateOf(0) }
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.8f)), shape = RoundedCornerShape(16.dp)) {
        TabRow(selectedTabIndex = selectedTab.value, containerColor = Color.Transparent) {
            Tab(selected = selectedTab.value == 0, onClick = { selectedTab.value = 0 }, text = { Text(stringResource(R.string.services_tab)) })
            Tab(selected = selectedTab.value == 1, onClick = { selectedTab.value = 1 }, text = { Text(stringResource(R.string.dashboard_tab)) })
        }
    }
    Spacer(Modifier.height(16.dp))
    if (selectedTab.value == 0) {
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(driverTrips) { trip -> DriverTripCard(trip) }
        }
    } else {
        DriverDashboard()
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
                    Text(trip.date, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1C1C1E))
                    Text("${trip.from} → ${trip.to}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B7280))
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
                    Text(trip.passenger, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1C1C1E))
                    Text("${trip.pickup} → ${trip.dropoff}", style = MaterialTheme.typography.bodySmall, color = Color(0xFF6B7280))
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
private fun DriverDashboard() {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.9f)), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.income_day), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                val data = listOf(5.8f,12.3f,9.4f,7.1f,15.2f,11.0f)
                val max = data.maxOrNull() ?: 1f
                data.forEachIndexed { i, v ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${8 + i}:00", modifier = Modifier.width(60.dp), color = Color(0xFF6B7280))
                        Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFE5E7EB))) {
                            Box(Modifier.fillMaxHeight().fillMaxWidth((v/max).coerceIn(0f,1f)).clip(RoundedCornerShape(4.dp)).background(Brush.horizontalGradient(listOf(Color(0xFF08817E), Color(0xFF1E1F47)))))
                        }
                        Spacer(Modifier.size(8.dp))
                        Text("S/ ${String.format("%.2f", v)}")
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}
