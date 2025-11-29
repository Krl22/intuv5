package com.intu.taxi

import android.os.Bundle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.appcompat.app.AppCompatActivity
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
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.animation.AccelerateDecelerateInterpolator

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val userSet = prefs.getBoolean("lang_user_set", false)
        if (!userSet) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
        }
        // Keep splash briefly until minimal init completes
        var appReady = false
        splash.setKeepOnScreenCondition { !appReady }
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = false
        splash.setOnExitAnimationListener { provider ->
            val iconView = provider.iconView
            val scaleX = ObjectAnimator.ofFloat(iconView, android.view.View.SCALE_X, 1f, 1.2f)
            val scaleY = ObjectAnimator.ofFloat(iconView, android.view.View.SCALE_Y, 1f, 1.2f)
            val alpha = ObjectAnimator.ofFloat(iconView, android.view.View.ALPHA, 1f, 0f)
            AnimatorSet().apply {
                playTogether(scaleX, scaleY, alpha)
                duration = 450
                interpolator = AccelerateDecelerateInterpolator()
                addListener(object : android.animation.AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: android.animation.Animator) {
                        provider.remove()
                    }
                })
                start()
            }
        }
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
        appReady = true
    }
}
@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    IntuTheme {
        TaxiApp()
    }
}
