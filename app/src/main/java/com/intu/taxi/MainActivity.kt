package com.intu.taxi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.intu.taxi.ui.theme.IntuTheme
import com.intu.taxi.ui.navigation.TaxiApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IntuTheme {
                TaxiApp()
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
