package com.intu.taxi.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    data object Home : BottomNavItem("home", "Home", Icons.Filled.Home)
    data object Trips : BottomNavItem("trips", "Trips", Icons.Filled.List)
    data object Account : BottomNavItem("account", "Account", Icons.Filled.Person)

    companion object {
        val items = listOf(Home, Trips, Account)
    }
}

