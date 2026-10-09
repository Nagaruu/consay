package com.consay.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.consay.app.ui.screens.appselect.AppSelectScreen
import com.consay.app.ui.screens.home.HomeScreen
import com.consay.app.ui.screens.permission.PermissionGuideScreen
import com.consay.app.ui.screens.setup.SetupSessionScreen
import com.consay.app.ui.theme.ConSayTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ConSayTheme {
                ConSayAppNavigation()
            }
        }
    }
}

@Composable
fun ConSayAppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onNavigateToSetup = { navController.navigate("setup") },
                onNavigateToPermissions = { navController.navigate("permissions") }
            )
        }
        composable("setup") {
            SetupSessionScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAppSelect = { navController.navigate("app_select") },
                onSessionActivated = {
                    navController.popBackStack("home", inclusive = false)
                }
            )
        }
        composable("app_select") {
            AppSelectScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("permissions") {
            PermissionGuideScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
