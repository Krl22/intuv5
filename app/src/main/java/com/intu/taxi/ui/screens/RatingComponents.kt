package com.intu.taxi.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun StarRating(rating: Int, onRatingChange: (Int) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        (1..5).forEach { i ->
            androidx.compose.material3.IconButton(onClick = { onRatingChange(i) }) {
                val filled = i <= rating
                Icon(
                    imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = null,
                    tint = if (filled) Color(0xFFFFC107) else Color(0xFF9E9E9E)
                )
            }
        }
    }
}

@Composable
fun RatingDialog(
    title: String,
    show: Boolean,
    allowComment: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (Int, String?) -> Unit,
) {
    if (!show) return
    val ratingState = remember { mutableStateOf(0) }
    val commentState = remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { onDismiss() },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StarRating(rating = ratingState.value, onRatingChange = { ratingState.value = it })
                if (allowComment) {
                    OutlinedTextField(
                        value = commentState.value,
                        onValueChange = { commentState.value = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(androidx.compose.ui.res.stringResource(com.intu.taxi.R.string.comment_optional)) }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (ratingState.value > 0) onSubmit(ratingState.value, if (allowComment) commentState.value.trim().ifEmpty { null } else null) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D9488), contentColor = Color.White)
            ) {
                Text(androidx.compose.ui.res.stringResource(com.intu.taxi.R.string.send_button), style = MaterialTheme.typography.bodySmall)
            }
        },
        dismissButton = {
            OutlinedButton(onClick = { onDismiss() }) {
                Text(androidx.compose.ui.res.stringResource(com.intu.taxi.R.string.skip_button))
            }
        }
    )
}
