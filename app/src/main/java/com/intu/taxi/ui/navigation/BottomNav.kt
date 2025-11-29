package com.intu.taxi.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.annotation.StringRes
import com.intu.taxi.R

sealed class BottomNavItem(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector
) {
    data object Home : BottomNavItem("home", R.string.nav_home, Icons.Filled.Home)
    data object Trips : BottomNavItem("trips", R.string.nav_trips, Icons.Filled.List)
    data object Account : BottomNavItem("account", R.string.nav_account, Icons.Filled.Person)

    companion object {
        val items = listOf(Home, Trips, Account)
    }
}
