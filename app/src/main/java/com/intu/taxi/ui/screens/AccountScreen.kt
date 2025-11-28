package com.intu.taxi.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun AccountScreen(onDebugClick: () -> Unit = {}) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { ClientHeader() }
        item { ContactCard() }
        item { PaymentCard() }
        item { SavedPlacesCard() }
        item { SupportPrivacyCard(onDebugClick) }
    }
}

@Composable
private fun ClientHeader() {
    val gradient = Brush.horizontalGradient(
        listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary
        )
    )
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(gradient).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(56.dp).clip(CircleShape)
            )
            Spacer(Modifier.size(16.dp))
            Column {
                Text(
                    "María López",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                )
                Text(
                    "Cliente desde 2023",
                    style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onPrimary)
                )
            }
        }
    }
}

@Composable
private fun ContactCard() {
    Card {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Contacto", style = MaterialTheme.typography.titleMedium)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            ListItem(headlineContent = { Text("maria.lopez@example.com") }, leadingContent = { Icon(Icons.Filled.Email, contentDescription = null) })
            ListItem(headlineContent = { Text("+51 987 654 321") }, leadingContent = { Icon(Icons.Filled.Phone, contentDescription = null) })
        }
    }
}

@Composable
private fun PaymentCard() {
    Card {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Pago", style = MaterialTheme.typography.titleMedium)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            ListItem(headlineContent = { Text("Visa •••• 1234 (predeterminada)") }, leadingContent = { Icon(Icons.Filled.CreditCard, contentDescription = null) })
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {}) { Text("Gestionar métodos") }
                OutlinedButton(onClick = {}) { Text("Agregar tarjeta") }
            }
        }
    }
}

@Composable
private fun SavedPlacesCard() {
    Card {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Lugares guardados", style = MaterialTheme.typography.titleMedium)
            HorizontalDivider(Modifier.padding(vertical = 8.dp))
            ListItem(headlineContent = { Text("Casa: Av. Primavera 123") }, leadingContent = { Icon(Icons.Filled.Home, contentDescription = null) })
            ListItem(headlineContent = { Text("Trabajo: Calle Sol 456") }, leadingContent = { Icon(Icons.Filled.Work, contentDescription = null) })
            OutlinedButton(onClick = {}) { Text("Administrar lugares") }
        }
    }
}

@Composable
private fun SupportPrivacyCard(onDebugClick: () -> Unit) {
    Card {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Ayuda y privacidad", style = MaterialTheme.typography.titleMedium)
            HorizontalDivider()
            OutlinedButton(onClick = {}) { Text("Centro de ayuda") }
            OutlinedButton(onClick = {}) { Text("Privacidad") }
            OutlinedButton(onClick = onDebugClick) {
                Icon(Icons.Filled.BugReport, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text("Debug")
            }
        }
    }
}
