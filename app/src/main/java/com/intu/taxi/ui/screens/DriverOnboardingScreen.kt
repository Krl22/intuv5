package com.intu.taxi.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverOnboardingScreen(onFinished: () -> Unit) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

    val vehicleType = remember { mutableStateOf("") }
    val vehicleBrand = remember { mutableStateOf("") }
    val vehicleModel = remember { mutableStateOf("") }
    val vehicleYear = remember { mutableStateOf("") }
    val licensePlate = remember { mutableStateOf("") }
    val driverLicense = remember { mutableStateOf("") }
    val photoUrl = remember { mutableStateOf("") }
    val status = remember { mutableStateOf("") }

    val expandedType = remember { mutableStateOf(false) }
    val expandedBrand = remember { mutableStateOf(false) }
    val expandedYear = remember { mutableStateOf(false) }
    val expandedModel = remember { mutableStateOf(false) }
    val isCustomModel = remember { mutableStateOf(false) }

    val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
    val years = (1990..currentYear + 1).map { it.toString() }.reversed()

    val vehicleTypes = listOf("Carro", "Moto", "Mototaxi")
    val vehicleBrandsByType = mapOf(
        "Carro" to listOf("Toyota", "Honda", "Nissan", "Chevrolet", "Ford", "Hyundai", "Kia", "Volkswagen", "Mazda", "Otra"),
        "Moto" to listOf("Honda", "Yamaha", "Suzuki", "Kawasaki", "Bajaj", "TVS", "Otra"),
        "Mototaxi" to listOf("Honda", "Bajaj")
    )
    val vehicleModelsByBrand = mapOf(
        "Honda" to listOf("Wave 125", "GL150", "XR 150", "CB 125", "CB 190", "Otro"),
        "Yamaha" to listOf("XTZ 125", "FZ 25", "MT 15", "Otro"),
        "Suzuki" to listOf("GN 125", "GSX 150", "V-Strom 250", "Otro"),
        "Kawasaki" to listOf("KLX 150", "Ninja 300", "Versys 300", "Otro"),
        "Bajaj" to listOf("Pulsar 135", "Pulsar 180", "Boxer 150", "Otro"),
        "TVS" to listOf("Apache RTR 160", "Star City 125", "Otro"),
        "Toyota" to listOf("Corolla", "Yaris", "Hilux", "RAV4", "Otro"),
        "Nissan" to listOf("Sentra", "Versa", "March", "X-Trail", "Otro"),
        "Chevrolet" to listOf("Spark", "Sail", "Onix", "Otro"),
        "Ford" to listOf("Fiesta", "Focus", "Ranger", "Otro"),
        "Hyundai" to listOf("Accent", "Elantra", "Tucson", "Otro"),
        "Kia" to listOf("Rio", "Cerato", "Sportage", "Otro"),
        "Volkswagen" to listOf("Jetta", "Vento", "Tiguan", "Otro"),
        "Mazda" to listOf("Mazda 2", "Mazda 3", "CX-5", "Otro"),
        "Otra" to listOf("Otro")
    )

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val ref = FirebaseStorage.getInstance().reference.child("users/$uid/vehicle.jpg")
            ref.putFile(uri).addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { url -> photoUrl.value = url.toString() }
            }.addOnFailureListener { e -> status.value = "Error subiendo foto: ${e.message}" }
        }
    }

    val bg = Brush.linearGradient(listOf(Color(0xFF1E1F47), Color(0xFF08817E), Color(0xFF0FB9B1)))

    Box(Modifier.fillMaxSize().background(bg)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val cardVisible = remember { mutableStateOf(false) }
            val titleVisible = remember { mutableStateOf(false) }
            val contentVisible = remember { mutableStateOf(false) }
            val buttonsVisible = remember { mutableStateOf(false) }
            LaunchedEffect(Unit) {
                delay(150); cardVisible.value = true
                delay(200); titleVisible.value = true
                delay(150); contentVisible.value = true
                delay(100); buttonsVisible.value = true
            }

            AnimatedVisibility(
                visible = cardVisible.value,
                enter = fadeIn(tween(1000, easing = FastOutSlowInEasing)) +
                        slideInVertically(initialOffsetY = { it / 6 }, animationSpec = tween(1000, easing = FastOutSlowInEasing)),
                exit = fadeOut() + slideOutVertically()
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(0.92f),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                    elevation = CardDefaults.cardElevation(12.dp)
                ) {
                    Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.heightIn(min = 60.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.heightIn(min = 60.dp))
                                Box(
                                    modifier = Modifier
                                        .heightIn(min = 60.dp)
                                        .background(Brush.radialGradient(colors = listOf(Color(0xFF08817E), Color(0xFF0FB9B1))), shape = CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = Color.White)
                                }
                            }
                        }

                        AnimatedVisibility(visible = titleVisible.value, enter = fadeIn(tween(800, 150, FastOutSlowInEasing)) + slideInVertically(initialOffsetY = { it / 8 }, animationSpec = tween(800, 150, FastOutSlowInEasing))) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Convertirse en conductor", fontWeight = FontWeight.Bold, color = Color(0xFF1E1F47), textAlign = TextAlign.Center)
                                Text("Completa tu información para comenzar a generar ingresos", color = Color(0xFF08817E), textAlign = TextAlign.Center)
                            }
                        }

                        AnimatedVisibility(visible = contentVisible.value, enter = fadeIn(tween(700, 350, FastOutSlowInEasing)) + slideInVertically(initialOffsetY = { it / 10 }, animationSpec = tween(700, 350, FastOutSlowInEasing))) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                // Tipo de vehículo
                                ExposedDropdownMenuBox(expanded = expandedType.value, onExpandedChange = { expandedType.value = !expandedType.value }) {
                                    TextField(
                                        value = vehicleType.value,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Tipo de vehículo") },
                                        leadingIcon = { Icon(Icons.Filled.DirectionsCar, contentDescription = null, tint = Color(0xFF08817E)) },
                                        trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            focusedIndicatorColor = Color(0xFF08817E),
                                            unfocusedIndicatorColor = Color(0xFF08817E).copy(alpha = 0.5f)
                                        ),
                                        modifier = Modifier.menuAnchor().fillMaxWidth()
                                    )
                                    DropdownMenu(expanded = expandedType.value, onDismissRequest = { expandedType.value = false }) {
                                        vehicleTypes.forEach { type -> DropdownMenuItem(text = { Text(type) }, onClick = { vehicleType.value = type; vehicleBrand.value = ""; expandedType.value = false }) }
                                    }
                                }

                                // Marca según tipo
                                ExposedDropdownMenuBox(expanded = expandedBrand.value, onExpandedChange = { expandedBrand.value = !expandedBrand.value }) {
                                    TextField(
                                        value = vehicleBrand.value,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text("Marca del vehículo") },
                                        trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) },
                                        colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color(0xFF08817E), unfocusedIndicatorColor = Color(0xFF08817E).copy(alpha = 0.5f)),
                                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                                        enabled = vehicleType.value.isNotBlank()
                                    )
                                    DropdownMenu(expanded = expandedBrand.value, onDismissRequest = { expandedBrand.value = false }) {
                                        (vehicleBrandsByType[vehicleType.value] ?: emptyList()).forEach { brand ->
                                            DropdownMenuItem(text = { Text(brand) }, onClick = { vehicleBrand.value = brand; vehicleModel.value = ""; isCustomModel.value = false; expandedBrand.value = false })
                                        }
                                    }
                                }

                                // Modelo o personalizado
                                if (vehicleBrand.value.isNotBlank()) {
                                    val models = vehicleModelsByBrand[vehicleBrand.value] ?: emptyList()
                                    if (isCustomModel.value) {
                                        TextField(value = vehicleModel.value, onValueChange = { vehicleModel.value = it }, label = { Text("Modelo personalizado") }, modifier = Modifier.fillMaxWidth(), colors = TextFieldDefaults.colors(focusedIndicatorColor = Color(0xFF08817E), unfocusedIndicatorColor = Color(0xFF08817E).copy(alpha = 0.5f)))
                                    } else {
                                        ExposedDropdownMenuBox(expanded = expandedModel.value, onExpandedChange = { expandedModel.value = !expandedModel.value }) {
                                            TextField(value = vehicleModel.value, onValueChange = {}, readOnly = true, label = { Text("Modelo del vehículo") }, trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) }, colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color(0xFF08817E), unfocusedIndicatorColor = Color(0xFF08817E).copy(alpha = 0.5f)), modifier = Modifier.menuAnchor().fillMaxWidth())
                                            DropdownMenu(expanded = expandedModel.value, onDismissRequest = { expandedModel.value = false }) {
                                                models.forEach { m -> DropdownMenuItem(text = { Text(m) }, onClick = { vehicleModel.value = if (m == "Otro") "" else m; isCustomModel.value = (m == "Otro"); expandedModel.value = false }) }
                                            }
                                        }
                                    }
                                }

                                // Año
                                ExposedDropdownMenuBox(expanded = expandedYear.value, onExpandedChange = { expandedYear.value = !expandedYear.value }) {
                                    TextField(value = vehicleYear.value, onValueChange = {}, readOnly = true, label = { Text("Año del vehículo") }, trailingIcon = { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) }, colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = Color(0xFF08817E), unfocusedIndicatorColor = Color(0xFF08817E).copy(alpha = 0.5f)), modifier = Modifier.menuAnchor().fillMaxWidth())
                                    DropdownMenu(expanded = expandedYear.value, onDismissRequest = { expandedYear.value = false }, modifier = Modifier.fillMaxWidth(0.9f).heightIn(max = 280.dp)) {
                                        years.forEach { year -> DropdownMenuItem(text = { Text(year) }, onClick = { vehicleYear.value = year; expandedYear.value = false }) }
                                    }
                                }

                                // Placa y licencia
                                TextField(value = licensePlate.value, onValueChange = { licensePlate.value = it.uppercase() }, label = { Text("Placa del vehículo") }, modifier = Modifier.fillMaxWidth(), colors = TextFieldDefaults.colors(focusedIndicatorColor = Color(0xFF08817E), unfocusedIndicatorColor = Color(0xFF08817E).copy(alpha = 0.5f)))
                                TextField(value = driverLicense.value, onValueChange = { driverLicense.value = it }, label = { Text("Licencia de conducir") }, modifier = Modifier.fillMaxWidth(), colors = TextFieldDefaults.colors(focusedIndicatorColor = Color(0xFF08817E), unfocusedIndicatorColor = Color(0xFF08817E).copy(alpha = 0.5f)))

                                OutlinedButton(onClick = { imagePicker.launch("image/*") }) { Text(if (photoUrl.value.isEmpty()) "Subir foto del vehículo" else "Foto seleccionada") }
                            }
                        }

                        AnimatedVisibility(visible = buttonsVisible.value, enter = fadeIn(tween(600, 500, FastOutSlowInEasing)) + slideInVertically(initialOffsetY = { it / 4 }, animationSpec = tween(600, 500, FastOutSlowInEasing))) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Button(onClick = { onFinished() }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C1C1E).copy(alpha = 0.8f), contentColor = Color.White)) { Text("Cancelar") }
                                val enabled = vehicleType.value.isNotBlank() && vehicleBrand.value.isNotBlank() && vehicleModel.value.isNotBlank() && vehicleYear.value.isNotBlank() && licensePlate.value.isNotBlank() && driverLicense.value.isNotBlank()
                                Button(onClick = {
                                    val data = mapOf(
                                        "driver" to mapOf(
                                            "vehicleType" to vehicleType.value,
                                            "vehicleBrand" to vehicleBrand.value,
                                            "vehicleModel" to vehicleModel.value,
                                            "vehicleYear" to vehicleYear.value,
                                            "vehiclePlate" to licensePlate.value,
                                            "driverLicense" to driverLicense.value,
                                            "vehiclePhotoUrl" to photoUrl.value
                                        ),
                                        "driverApproved" to false,
                                        "driverMode" to false,
                                        "balance" to 0.0
                                    )
                                    FirebaseFirestore.getInstance().collection("users").document(uid)
                                        .set(data, SetOptions.merge())
                                        .addOnSuccessListener { onFinished() }
                                        .addOnFailureListener { e -> status.value = "Error: ${e.message}" }
                                }, enabled = enabled, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF08817E), contentColor = Color.White)) { Text("Convertirse en conductor") }
                            }
                        }
                        if (status.value.isNotEmpty()) Text(status.value, color = Color.White)
                    }
                }
            }
        }
    }
}
