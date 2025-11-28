package com.intu.taxi.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavDestination.Companion.hierarchy
import com.intu.taxi.ui.screens.AccountScreen
import com.intu.taxi.ui.screens.HomeScreen
import com.intu.taxi.ui.screens.TripsScreen

@Composable
fun TaxiApp() {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = backStackEntry?.destination
            NavigationBar(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                BottomNavItem.items.forEach { item ->
                    val selected = isDestinationInHierarchy(currentDestination, item.route)
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (!selected) {
                                if (item.route == BottomNavItem.Account.route) {
                                    navController.popBackStack("debug", inclusive = true)
                                }
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { androidx.compose.material3.Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Home.route) { HomeScreen() }
            composable(BottomNavItem.Trips.route) { TripsScreen() }
            composable(BottomNavItem.Account.route) { AccountScreen(onDebugClick = { navController.navigate("debug") }) }
            composable("debug") { com.intu.taxi.ui.screens.DebugScreen() }
        }
    }
}

private fun isDestinationInHierarchy(currentDestination: NavDestination?, route: String): Boolean {
    return currentDestination?.hierarchy?.any { it.route == route } == true
}
