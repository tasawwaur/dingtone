package com.example.dingtoneclone

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.dingtoneclone.ui.calls.CallsScreen
import com.example.dingtoneclone.ui.dashboard.DashboardScreen
import com.example.dingtoneclone.ui.login.LoginScreen
import com.example.dingtoneclone.ui.numbers.NumbersScreen
import com.google.firebase.auth.FirebaseAuth

data class BottomNavItem(val label: String, val icon: ImageVector, val destination: NavKey)

@Composable
fun MainNavigation() {
    val auth = FirebaseAuth.getInstance()
    val isLoggedIn = remember { mutableStateOf(auth.currentUser != null) }

    if (!isLoggedIn.value) {
        LoginScreen(onLoginSuccess = { isLoggedIn.value = true })
    } else {
        MainAppScaffold(onLogout = {
            auth.signOut()
            isLoggedIn.value = false
        })
    }
}

@Composable
private fun MainAppScaffold(onLogout: () -> Unit) {
    val backStack = rememberNavBackStack(Dashboard)

    val navItems = listOf(
        BottomNavItem("Inbox",   Icons.Default.Email,  Dashboard),
        BottomNavItem("Numbers", Icons.Default.Phone,  Numbers),
        BottomNavItem("Calls",   Icons.Default.Call,   Calls)
    )

    Scaffold(
        containerColor = Color(0xFF0F0C29),
        bottomBar = {
            NavigationBar(containerColor = Color(0xFF1E1B3A)) {
                val current = backStack.lastOrNull()
                navItems.forEach { item ->
                    NavigationBarItem(
                        selected = current == item.destination,
                        onClick = {
                            if (current != item.destination) {
                                backStack.removeAll { it == item.destination }
                                backStack.add(item.destination)
                            }
                        },
                        icon  = { Icon(item.icon, item.label) },
                        label = { Text(item.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor   = Color(0xFF7C5CBF),
                            selectedTextColor   = Color(0xFF7C5CBF),
                            unselectedIconColor = Color(0xFF606070),
                            unselectedTextColor = Color(0xFF606070),
                            indicatorColor      = Color(0xFF2A2450)
                        )
                    )
                }
            }
        }
    ) { padding ->
        NavDisplay(
            backStack  = backStack,
            onBack     = { backStack.removeLastOrNull() },
            modifier   = Modifier.padding(padding),
            entryProvider = entryProvider {
                entry<Dashboard> { DashboardScreen() }
                entry<Numbers>   { NumbersScreen() }
                entry<Calls>     { CallsScreen() }
            }
        )
    }
}
