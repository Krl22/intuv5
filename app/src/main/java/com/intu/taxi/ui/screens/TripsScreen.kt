package com.intu.taxi.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PriceChange
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class TripItem(
    val date: String,
    val from: String,
    val to: String,
    val price: String,
    val status: String
)

private val sampleTrips = listOf(
    TripItem("Hoy 10:30", "Av. Primavera 123", "Aeropuerto", "S/ 28.50", "Completado"),
    TripItem("Ayer 19:05", "Calle Sol 456", "Centro Comercial", "S/ 15.40", "Completado"),
    TripItem("Mar 14:20", "Universidad", "Casa", "S/ 21.10", "Cancelado"),
)

@Composable
fun TripsScreen() {
    Column(Modifier.fillMaxSize()) {
        Header()
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(sampleTrips) { trip ->
                TripCard(trip)
            }
        }
    }
}

@Composable
private fun Header() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = "Tus viajes",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            Text(
                text = "Historial y próximos",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun TripCard(item: TripItem) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.AccessTime, contentDescription = null)
                Text(item.date, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                AssistChip(
                    onClick = {},
                    label = { Text(item.status) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocationOn, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(item.from, style = MaterialTheme.typography.bodyLarge)
                    Text(item.to, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.DirectionsCar, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Económico", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.weight(1f))
                Icon(Icons.Filled.PriceChange, contentDescription = null)
                Spacer(Modifier.size(4.dp))
                Text(item.price, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
