package com.intu.taxi.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.intu.taxi.ui.debug.DebugLog
import com.intu.taxi.ui.theme.Indigo40
import com.intu.taxi.ui.theme.Teal40

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverTopUpScreen(onFinished: () -> Unit, onCancel: () -> Unit = onFinished) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    var amount by remember { mutableStateOf("") }
    var method by remember { mutableStateOf("yape") }
    var screenshotUri by remember { mutableStateOf<Uri?>(null) }
    var status by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        screenshotUri = uri
    }

    // Professional Gradient Background
    val bgBrush = Brush.linearGradient(
        colors = listOf(
            Color(0xFF0D1B2A), // Dark Navy
            Color(0xFF1B263B), // Deep Blue Grey
            Color(0xFF004D40)  // Deep Teal
        )
    )

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        stringResource(com.intu.taxi.R.string.recharge_balance), 
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp
                    ) 
                },
                navigationIcon = {
                     IconButton(onClick = onCancel) {
                         Icon(
                             imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                             contentDescription = stringResource(com.intu.taxi.R.string.back),
                             tint = Color.White
                         )
                     }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgBrush)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Header / Intro (Optional refinement)
                Text(
                    text = stringResource(com.intu.taxi.R.string.manage_wallet),
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodyMedium
                )

                // Amount Section
                GlassCard {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = stringResource(com.intu.taxi.R.string.amount_to_recharge),
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { input ->
                                if (input.all { it.isDigit() || it == '.' }) {
                                    amount = input
                                }
                            },
                            label = { Text(stringResource(com.intu.taxi.R.string.amount_hint), color = Color.White.copy(alpha = 0.6f)) },
                            leadingIcon = {
                                Icon(Icons.Default.AttachMoney, contentDescription = null, tint = Color(0xFF4DB6AC)) // Soft Teal
                            },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = androidx.compose.material3.TextFieldDefaults.colors(
                                focusedContainerColor = Color.White.copy(alpha = 0.05f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedIndicatorColor = Color(0xFF4DB6AC),
                                unfocusedIndicatorColor = Color.White.copy(alpha = 0.1f),
                                cursorColor = Color(0xFF4DB6AC),
                                focusedLabelColor = Color(0xFF4DB6AC)
                            )
                        )
                    }
                }

                // Payment Method Section
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = stringResource(com.intu.taxi.R.string.payment_method_label),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        PaymentMethodCard(
                            name = "Yape",
                            isSelected = method == "yape",
                            activeColor = Color(0xFFAB47BC), // Refined Purple
                            icon = Icons.Default.CreditCard,
                            onClick = { method = "yape" },
                            modifier = Modifier.weight(1f)
                        )
                        PaymentMethodCard(
                            name = "Plin",
                            isSelected = method == "plin",
                            activeColor = Color(0xFF26C6DA), // Refined Cyan/Teal
                            icon = Icons.Default.CreditCard,
                            onClick = { method = "plin" },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Screenshot Section
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = stringResource(com.intu.taxi.R.string.payment_proof),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { picker.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (screenshotUri != null) {
                            AsyncImage(
                                model = screenshotUri,
                                contentDescription = stringResource(com.intu.taxi.R.string.screenshot_desc),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.4f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddPhotoAlternate,
                                        contentDescription = stringResource(com.intu.taxi.R.string.change_photo),
                                        tint = Color.White
                                    )
                                    Text(stringResource(com.intu.taxi.R.string.change_photo), color = Color.White)
                                }
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = stringResource(com.intu.taxi.R.string.upload_proof),
                                    modifier = Modifier.size(40.dp),
                                    tint = Color(0xFF4DB6AC)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = stringResource(com.intu.taxi.R.string.upload_proof),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                            }
                        }
                    }
                }

                // Status Message
                AnimatedVisibility(visible = status.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (status.contains("Error") || status.contains("inválido") || status.contains("null")) 
                                    Color(0xFFEF5350).copy(alpha = 0.15f)
                                else 
                                    Color(0xFF66BB6A).copy(alpha = 0.15f)
                            )
                            .border(
                                1.dp,
                                if (status.contains("Error") || status.contains("inválido") || status.contains("null")) 
                                    Color(0xFFEF5350).copy(alpha = 0.5f)
                                else 
                                    Color(0xFF66BB6A).copy(alpha = 0.5f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (status.contains("Error") || status.contains("inválido")) Icons.Default.CreditCard else Icons.Default.CheckCircle, // Placeholder icon logic
                                contentDescription = null,
                                tint = if (status.contains("Error") || status.contains("inválido")) Color(0xFFEF5350) else Color(0xFF66BB6A),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = status,
                                color = Color.White.copy(alpha = 0.9f),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Submit Button
                Button(
                    onClick = {
                        if (isLoading) return@Button
                        
                        val u = uid
                        if (u == null) {
                            status = "Usuario no autenticado"
                            DebugLog.log("topup uid null")
                            return@Button
                        }
                        val amt = amount.toDoubleOrNull()
                        if (amt == null || amt <= 0) {
                            status = "Monto inválido"
                            DebugLog.log("topup amount invalid='$amount'")
                            return@Button
                        }
                        val uri = screenshotUri
                        if (uri == null) {
                            status = "Sube una captura del pago"
                            DebugLog.log("topup screenshot missing")
                            return@Button
                        }

                        isLoading = true
                        status = "Subiendo comprobante..."
                        DebugLog.log("topup start uid=$u amount=$amt method=$method")
                        
                        val ts = System.currentTimeMillis()
                        val ref = FirebaseStorage.getInstance().reference.child("users/$u/topups/$ts.jpg")
                        
                        ref.putFile(uri)
                            .addOnSuccessListener {
                                DebugLog.log("topup upload success ts=$ts")
                                status = "Procesando solicitud..."
                                ref.downloadUrl.addOnSuccessListener { url ->
                                    DebugLog.log("topup downloadUrl=$url")
                                    val data = mapOf(
                                        "userId" to u,
                                        "amount" to amt,
                                        "method" to method,
                                        "screenshotUrl" to url.toString(),
                                        "isapproved" to false,
                                        "processed" to false,
                                        "createdAt" to FieldValue.serverTimestamp()
                                    )
                                    FirebaseFirestore.getInstance().collection("topups").add(data)
                                        .addOnSuccessListener { docRef ->
                                            DebugLog.log("topup add success id=${docRef.id}")
                                            status = "Solicitud enviada con éxito"
                                            isLoading = false
                                            onFinished()
                                        }
                                        .addOnFailureListener { e ->
                                            DebugLog.log("topup add failed: ${e.message}")
                                            status = "Error al guardar solicitud"
                                            isLoading = false
                                        }
                                }.addOnFailureListener { e ->
                                    DebugLog.log("topup getUrl failed: ${e.message}")
                                    status = "Error al obtener URL"
                                    isLoading = false
                                }
                            }
                            .addOnFailureListener { e ->
                                DebugLog.log("topup upload failed: ${e.message}")
                                status = "Error al subir captura"
                                isLoading = false
                            }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isLoading && amount.isNotEmpty() && screenshotUri != null,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4DB6AC), // Professional Teal
                        contentColor = Color.White,
                        disabledContainerColor = Color.White.copy(alpha = 0.1f),
                        disabledContentColor = Color.White.copy(alpha = 0.3f)
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(stringResource(com.intu.taxi.R.string.processing), fontWeight = FontWeight.Medium)
                    } else {
                        Text(stringResource(com.intu.taxi.R.string.send_request), fontWeight = FontWeight.SemiBold, fontSize = 16.sp, letterSpacing = 0.5.sp)
                    }
                }
                
                // Cancel Button
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White.copy(alpha = 0.7f),
                        containerColor = Color.Transparent
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Text(stringResource(com.intu.taxi.R.string.cancel), fontWeight = FontWeight.Medium, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun GlassCard(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.08f)) // More subtle
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.1f), // Subtler border
                shape = RoundedCornerShape(16.dp)
            )
    ) {
        content()
    }
}

@Composable
fun PaymentMethodCard(
    name: String,
    isSelected: Boolean,
    activeColor: Color,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) activeColor else Color.White.copy(alpha = 0.1f)
    val backgroundColor = if (isSelected) activeColor.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f)

    Box(
        modifier = modifier
            .height(80.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = activeColor,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f)
            )
        }
    }
}
