package com.intu.taxi.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

@Composable
fun LoginScreen(onLoggedIn: () -> Unit, onPhoneLogin: () -> Unit) {
    val context = LocalContext.current
    val auth = remember { FirebaseAuth.getInstance() }

    LaunchedEffect(auth.currentUser) {
        if (auth.currentUser != null) onLoggedIn()
    }

    val googleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.result
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            auth.signInWithCredential(credential).addOnSuccessListener { onLoggedIn() }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Bienvenido", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold))
        Text("Inicia sesión para continuar", style = MaterialTheme.typography.bodyMedium)

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = {
                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(context.getString(com.intu.taxi.R.string.default_web_client_id))
                        .requestEmail()
                        .build()
                    val client = GoogleSignIn.getClient(context, gso)
                    googleLauncher.launch(client.signInIntent)
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Continuar con Google")
                }

                OutlinedButton(onClick = onPhoneLogin, modifier = Modifier.fillMaxWidth()) {
                    Text("Continuar con teléfono")
                }
            }
        }
    }
}
