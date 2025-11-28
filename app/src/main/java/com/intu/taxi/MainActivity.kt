package com.intu.taxi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.Text
import androidx.compose.ui.tooling.preview.Preview
import com.intu.taxi.ui.theme.IntuTheme
import com.intu.taxi.ui.navigation.TaxiApp
import com.google.firebase.auth.FirebaseAuth
import com.intu.taxi.ui.screens.LoginScreen
import com.google.firebase.firestore.FirebaseFirestore
import com.intu.taxi.ui.screens.OnboardingForm
import com.intu.taxi.ui.screens.PhoneLoginScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IntuTheme {
                val auth = FirebaseAuth.getInstance()
                var isLoggedIn by remember { mutableStateOf(auth.currentUser != null) }
                var needsOnboarding by remember { mutableStateOf(false) }
                var profileChecked by remember { mutableStateOf(false) }
                var showPhoneLogin by remember { mutableStateOf(false) }
                DisposableEffect(auth) {
                    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                        isLoggedIn = firebaseAuth.currentUser != null
                        if (isLoggedIn) {
                            val uid = firebaseAuth.currentUser?.uid
                            if (uid != null) {
                                FirebaseFirestore.getInstance().collection("users").document(uid)
                                    .get()
                                    .addOnSuccessListener { doc ->
                                        needsOnboarding = !doc.exists()
                                        profileChecked = true
                                    }
                                    .addOnFailureListener {
                                        needsOnboarding = true
                                        profileChecked = true
                                    }
                            }
                        } else {
                            needsOnboarding = false
                            profileChecked = false
                        }
                    }
                    auth.addAuthStateListener(listener)
                    onDispose { auth.removeAuthStateListener(listener) }
                }
                if (!isLoggedIn) {
                    if (showPhoneLogin) {
                        PhoneLoginScreen(onBack = { showPhoneLogin = false }, onLoggedIn = { isLoggedIn = true })
                    } else {
                        LoginScreen(onLoggedIn = { isLoggedIn = true }, onPhoneLogin = { showPhoneLogin = true })
                    }
                } else if (!profileChecked) {
                    Text("Verificando perfil...")
                } else if (needsOnboarding) {
                    OnboardingForm(onCompleted = { needsOnboarding = false })
                } else {
                    TaxiApp()
                }
            }
        }
    }
}
@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    IntuTheme {
        TaxiApp()
    }
}
