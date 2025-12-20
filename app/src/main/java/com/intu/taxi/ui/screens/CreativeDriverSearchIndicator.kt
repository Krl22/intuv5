package com.intu.taxi.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

@Composable
fun CreativeDriverSearchIndicator(
    isVisible: Boolean,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return

    val infiniteTransition = rememberInfiniteTransition(label = "creative_search")

    val radarRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(3000, easing = LinearEasing)),
        label = "radar_rotation"
    )
    val centerPulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "center_pulse"
    )
    val wave1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(animation = tween(2000)),
        label = "wave1"
    )
    val wave2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(animation = tween(2000, delayMillis = 800)),
        label = "wave2"
    )
    val wave3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(animation = tween(2000, delayMillis = 1600)),
        label = "wave3"
    )
    val floatY by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float_animation"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_pulse"
    )
    val particleOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(8000, easing = LinearEasing)),
        label = "particle_rotation"
    )

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF00FFE0).copy(alpha = 0.15f * glowAlpha),
                            Color(0xFF1E1F47).copy(alpha = 0.08f * glowAlpha),
                            Color.Transparent
                        ),
                        radius = 0.8f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.05f * glowAlpha),
                            Color.Transparent,
                            Color.White.copy(alpha = 0.03f * glowAlpha)
                        )
                    )
                )
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom,
            modifier = Modifier.fillMaxSize().padding(bottom = 24.dp)
        ) {
            Card(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .wrapContentSize()
                    .alpha(0.98f),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1F47).copy(alpha = 0.85f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp, pressedElevation = 12.dp),
                border = BorderStroke(1.5.dp, Brush.linearGradient(listOf(Color(0xFF08817E).copy(alpha = 0.8f), Color.White.copy(alpha = 0.4f), Color(0xFF08817E).copy(alpha = 0.8f))))
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Box(modifier = Modifier.size(120.dp), contentAlignment = Alignment.Center) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            val center = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
                            val maxRadius = min(size.width, size.height) / 2f
                            drawCircle(color = Color(0xFF00FFE0).copy(alpha = 0.3f), center = center, radius = maxRadius * 0.9f, style = Stroke(width = 2.dp.toPx()))
                            drawCircle(color = Color(0xFF00FFE0).copy(alpha = 0.4f), center = center, radius = maxRadius * 0.6f, style = Stroke(width = 2.dp.toPx()))
                            val waveRadius1 = maxRadius * 0.7f * (1f - wave1Alpha)
                            val waveRadius2 = maxRadius * 0.7f * (1f - wave2Alpha)
                            val waveRadius3 = maxRadius * 0.7f * (1f - wave3Alpha)
                            drawCircle(color = Color(0xFF00FFE0).copy(alpha = wave1Alpha * 0.7f), center = center, radius = waveRadius1, style = Stroke(width = 3.dp.toPx()))
                            drawCircle(color = Color(0xFF00FFE0).copy(alpha = wave2Alpha * 0.7f), center = center, radius = waveRadius2, style = Stroke(width = 3.dp.toPx()))
                            drawCircle(color = Color(0xFF00FFE0).copy(alpha = wave3Alpha * 0.7f), center = center, radius = waveRadius3, style = Stroke(width = 3.dp.toPx()))
                            val radarAngle = Math.toRadians(radarRotation.toDouble())
                            val radarLength = maxRadius * 0.8f
                            drawLine(color = Color(0xFF00FFE0).copy(alpha = 1f), start = center, end = androidx.compose.ui.geometry.Offset(center.x + kotlin.math.cos(radarAngle).toFloat() * radarLength, center.y + kotlin.math.sin(radarAngle).toFloat() * radarLength), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
                            val pulseRadius = maxRadius * 0.15f * centerPulse
                            drawCircle(color = Color(0xFF00FFE0), center = center, radius = pulseRadius, style = Fill)
                            drawCircle(color = Color.White.copy(alpha = 0.9f), center = center, radius = pulseRadius * 0.6f, style = Fill)
                            val particleCount = 8
                            val particleAngle = Math.toRadians(particleOffset.toDouble())
                            val particleRadius = maxRadius * 0.85f
                            for (i in 0 until particleCount) {
                                val angle = particleAngle + (i * Math.PI * 2 / particleCount)
                                val particleX = center.x + kotlin.math.cos(angle).toFloat() * particleRadius
                                val particleY = center.y + kotlin.math.sin(angle).toFloat() * particleRadius
                                drawCircle(color = Color(0xFF00FFE0).copy(alpha = 0.8f), center = androidx.compose.ui.geometry.Offset(particleX, particleY), radius = 3.dp.toPx(), style = Fill)
                            }
                        }
                        Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = Color(0xFF08817E), modifier = Modifier.size(24.dp))
                    }
                    Spacer(modifier = Modifier.size(24.dp))
                    Text(text = "Buscando conductor", style = MaterialTheme.typography.headlineSmall, color = Color(0xFFE8F8F7), fontWeight = FontWeight.Bold, fontSize = 24.sp)
                    Spacer(modifier = Modifier.size(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "Escaneando área", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF88D8D5), fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.size(4.dp))
                        Row {
                            for (i in 0..2) {
                                val dotAlpha by infiniteTransition.animateFloat(
                                    initialValue = 0.3f,
                                    targetValue = 1f,
                                    animationSpec = infiniteRepeatable(animation = tween(durationMillis = 1000, delayMillis = i * 200, easing = FastOutSlowInEasing), repeatMode = RepeatMode.Reverse),
                                    label = "text_dot_$i"
                                )
                                Box(modifier = Modifier.padding(horizontal = 1.dp).size(3.dp).clip(CircleShape).background(Color.White.copy(alpha = dotAlpha)))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.size(24.dp))
                    Surface(
                        onClick = onCancel,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = Color(0xFF08817E).copy(alpha = 0.4f),
                        border = BorderStroke(2.dp, Brush.linearGradient(listOf(Color(0xFF00FFE0).copy(alpha = 0.8f), Color.White.copy(alpha = 0.6f), Color(0xFF00FFE0).copy(alpha = 0.8f)))),
                        shadowElevation = 4.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(52.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(text = androidx.compose.ui.res.stringResource(com.intu.taxi.R.string.cancel_search), style = MaterialTheme.typography.bodyMedium, color = Color(0xFFE8F8F7), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }
}
