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
import androidx.navigation.navArgument
import androidx.navigation.NavType
import android.net.Uri
import com.intu.taxi.ui.screens.AccountScreen
import com.google.firebase.auth.FirebaseAuth
import com.intu.taxi.ui.screens.HomeScreen
import com.intu.taxi.ui.screens.TripsScreen
import com.intu.taxi.ui.screens.VerifyPhoneScreen
import androidx.compose.ui.res.stringResource
import com.intu.taxi.R
import com.intu.taxi.ui.screens.DriverOnboardingScreen
import com.intu.taxi.ui.screens.DriverHomeScreen
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.DisposableEffect
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun TaxiApp() {
    val navController = rememberNavController()
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val driverActive = remember { mutableStateOf(false) }

    DisposableEffect(uid) {
        val reg = if (uid != null) FirebaseFirestore.getInstance().collection("users").document(uid).addSnapshotListener { doc, _ ->
            val approved = doc?.getBoolean("driverApproved") == true
            val mode = doc?.getBoolean("driverMode") == true
            val active = approved && mode
            driverActive.value = active
            val currentRoute = navController.currentBackStackEntry?.destination?.route
            if (active && currentRoute != "driverHome") {
                navController.navigate("driverHome") {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            } else if (!active && currentRoute == "driverHome") {
                navController.navigate(BottomNavItem.Home.route) {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        } else null
        onDispose { reg?.remove() }
    }

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
                                val targetRoute = if (item.route == BottomNavItem.Home.route && driverActive.value) "driverHome" else item.route
                                navController.navigate(targetRoute) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { androidx.compose.material3.Icon(item.icon, contentDescription = stringResource(item.labelRes)) },
                        label = { Text(stringResource(item.labelRes)) }
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
            composable("driverHome") { DriverHomeScreen() }
            composable(BottomNavItem.Trips.route) { TripsScreen() }
            composable(BottomNavItem.Account.route) {
                AccountScreen(
                    onDebugClick = { navController.navigate("debug") },
                    onLogout = { FirebaseAuth.getInstance().signOut() },
                    onVerifyPhone = { phone ->
                        val encoded = Uri.encode(phone)
                        navController.navigate("verifyPhone?phone=$encoded")
                    },
                    onStartDriver = { navController.navigate("driverOnboarding") }
                )
            }
            composable("debug") { com.intu.taxi.ui.screens.DebugScreen() }
            composable(
                route = "verifyPhone?phone={phone}",
                arguments = listOf(navArgument("phone") { type = NavType.StringType; nullable = true; defaultValue = "" })
            ) { backStackEntry ->
                val phone = backStackEntry.arguments?.getString("phone").orEmpty()
                VerifyPhoneScreen(phone = phone, onFinished = { navController.popBackStack() })
            }
            composable("driverOnboarding") {
                DriverOnboardingScreen(onFinished = { navController.popBackStack() })
            }
        }
    }
}

private fun isDestinationInHierarchy(currentDestination: NavDestination?, route: String): Boolean {
    return currentDestination?.hierarchy?.any { it.route == route } == true
}
