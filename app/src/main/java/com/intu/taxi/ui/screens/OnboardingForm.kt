package com.intu.taxi.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingForm(onCompleted: () -> Unit) {
    val user = FirebaseAuth.getInstance().currentUser
    val firstName = remember { mutableStateOf("") }
    val lastName = remember { mutableStateOf("") }
    val birthdate = remember { mutableStateOf("") }
    val email = remember { mutableStateOf(user?.email ?: "") }
    val countryCodes = listOf("+1", "+34", "+44", "+51", "+52", "+54", "+57", "+58")
    val countryExpanded = remember { mutableStateOf(false) }
    val rawPhone = user?.phoneNumber ?: ""
    fun splitE164(phone: String): Pair<String, String> {
        val normalized = phone.filter { it == '+' || it.isDigit() }
        val code = countryCodes.filter { normalized.startsWith(it) }.maxByOrNull { it.length } ?: "+51"
        val rest = if (normalized.startsWith(code)) normalized.removePrefix(code) else ""
        return code to rest
    }
    val initial = splitE164(rawPhone)
    val countryCode = remember { mutableStateOf(initial.first) }
    val number = remember { mutableStateOf(initial.second) }

    val datePickerOpen = remember { mutableStateOf(false) }
    val dateState = rememberDatePickerState()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(androidx.compose.ui.res.stringResource(com.intu.taxi.R.string.complete_profile_title))
        OutlinedTextField(value = firstName.value, onValueChange = { firstName.value = it }, label = { Text(androidx.compose.ui.res.stringResource(com.intu.taxi.R.string.first_name_label)) }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = lastName.value, onValueChange = { lastName.value = it }, label = { Text(androidx.compose.ui.res.stringResource(com.intu.taxi.R.string.last_name_label)) }, modifier = Modifier.fillMaxWidth())

        OutlinedTextField(
            value = birthdate.value,
            onValueChange = {},
            readOnly = true,
            label = { Text(androidx.compose.ui.res.stringResource(com.intu.taxi.R.string.birth_date_label)) },
            modifier = Modifier.fillMaxWidth().clickable { datePickerOpen.value = true },
            trailingIcon = {
                IconButton(onClick = { datePickerOpen.value = true }) {
                    Icon(Icons.Filled.DateRange, contentDescription = null)
                }
            }
        )

        if (datePickerOpen.value) {
            DatePickerDialog(
                onDismissRequest = { datePickerOpen.value = false },
                confirmButton = {
                    Button(onClick = {
                        val millis = dateState.selectedDateMillis
                        if (millis != null) {
                            val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            birthdate.value = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                        }
                        datePickerOpen.value = false
                    }) { Text(androidx.compose.ui.res.stringResource(com.intu.taxi.R.string.ok)) }
                },
                dismissButton = {
                    Button(onClick = { datePickerOpen.value = false }) { Text(androidx.compose.ui.res.stringResource(com.intu.taxi.R.string.cancel)) }
                }
            ) {
                DatePicker(state = dateState)
            }
        }
        
        OutlinedTextField(value = email.value, onValueChange = { email.value = it }, label = { Text(androidx.compose.ui.res.stringResource(com.intu.taxi.R.string.email_label)) }, modifier = Modifier.fillMaxWidth())

        Row(modifier = Modifier.fillMaxWidth()) {
            ExposedDropdownMenuBox(
                expanded = countryExpanded.value,
                onExpandedChange = { countryExpanded.value = !countryExpanded.value },
                modifier = Modifier.weight(0.4f)
            ) {
                TextField(
                    value = countryCode.value,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(androidx.compose.ui.res.stringResource(com.intu.taxi.R.string.country_code_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = countryExpanded.value) },
                    colors = ExposedDropdownMenuDefaults.textFieldColors(),
                    modifier = Modifier.menuAnchor()
                )
                DropdownMenu(expanded = countryExpanded.value, onDismissRequest = { countryExpanded.value = false }) {
                    countryCodes.forEach { code ->
                        DropdownMenuItem(text = { Text(code) }, onClick = {
                            countryCode.value = code
                            countryExpanded.value = false
                        })
                    }
                }
            }
            OutlinedTextField(
                value = number.value,
                onValueChange = { number.value = it.filter { ch -> ch.isDigit() } },
                label = { Text(androidx.compose.ui.res.stringResource(com.intu.taxi.R.string.phone_number_label)) },
                modifier = Modifier.weight(0.6f).padding(start = 8.dp)
            )
        }

        Button(onClick = {
            val uid = user?.uid ?: return@Button
            val ref = FirebaseFirestore.getInstance().collection("users").document(uid)
            val data = mapOf(
                "firstName" to firstName.value.trim(),
                "lastName" to lastName.value.trim(),
                "birthdate" to birthdate.value.trim(),
                "email" to email.value.trim(),
                "countryCode" to countryCode.value,
                "number" to number.value.trim(),
                "fullNumber" to (countryCode.value + number.value.trim())
            )
            ref.set(data).addOnSuccessListener { onCompleted() }
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Guardar")
        }
    }
}
