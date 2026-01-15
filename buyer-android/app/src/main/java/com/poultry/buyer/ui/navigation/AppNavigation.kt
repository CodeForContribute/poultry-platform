package com.poultry.buyer.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.poultry.buyer.ui.screens.auth.AuthViewModel
import com.poultry.buyer.ui.screens.auth.PhoneInputScreen
import com.poultry.buyer.ui.screens.auth.OtpVerificationScreen
import com.poultry.buyer.ui.screens.home.HomeScreen
import com.poultry.buyer.ui.screens.cart.CartListScreen
import com.poultry.buyer.ui.screens.orders.OrderListScreen
import com.poultry.buyer.ui.screens.profile.ProfileScreen

sealed class Screen(val route: String) {
    object PhoneInput : Screen("phone_input")
    object OtpVerification : Screen("otp_verification/{phone}") {
        fun createRoute(phone: String) = "otp_verification/$phone"
    }
    object Home : Screen("home")
    object Search : Screen("search")
    object Cart : Screen("cart")
    object Orders : Screen("orders")
    object Profile : Screen("profile")
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: @Composable () -> Unit
)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = hiltViewModel()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsState()

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            navController.navigate(Screen.Home.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    val startDestination = if (isLoggedIn) Screen.Home.route else Screen.PhoneInput.route

    val bottomNavItems = listOf(
        BottomNavItem(Screen.Home.route, "Home") { Icon(Icons.Default.Home, contentDescription = null) },
        BottomNavItem(Screen.Search.route, "Search") { Icon(Icons.Default.Search, contentDescription = null) },
        BottomNavItem(Screen.Cart.route, "Cart") { Icon(Icons.Default.ShoppingCart, contentDescription = null) },
        BottomNavItem(Screen.Orders.route, "Orders") { Icon(Icons.Default.List, contentDescription = null) },
        BottomNavItem(Screen.Profile.route, "Profile") { Icon(Icons.Default.Person, contentDescription = null) },
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = item.icon,
                            label = { Text(item.label) },
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(Screen.Home.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Screen.PhoneInput.route) {
                PhoneInputScreen(
                    onOtpRequested = { phone ->
                        navController.navigate(Screen.OtpVerification.createRoute(phone))
                    }
                )
            }

            composable(Screen.OtpVerification.route) { backStackEntry ->
                val phone = backStackEntry.arguments?.getString("phone") ?: ""
                OtpVerificationScreen(
                    phoneNumber = phone,
                    onVerified = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Home.route) {
                HomeScreen()
            }

            composable(Screen.Search.route) {
                // SearchScreen()
                Text("Search Screen")
            }

            composable(Screen.Cart.route) {
                CartListScreen()
            }

            composable(Screen.Orders.route) {
                OrderListScreen()
            }

            composable(Screen.Profile.route) {
                ProfileScreen(
                    onLogout = {
                        navController.navigate(Screen.PhoneInput.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
