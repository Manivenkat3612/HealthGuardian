package com.example.urban_safety.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.urban_safety.ui.screens.auth.LoginScreen
import com.example.urban_safety.ui.screens.auth.SignupScreen
import com.example.urban_safety.ui.screens.community.CommunityHelpersScreen
import com.example.urban_safety.ui.screens.emergency.EmergencyContactsScreen
import com.example.urban_safety.ui.screens.emergency.ManualSOSScreen
import com.example.urban_safety.ui.screens.health.AIHealthMonitoringScreen
import com.example.urban_safety.ui.screens.home.HomeScreen
import com.example.urban_safety.ui.screens.maps.SafeRoutesScreen
import com.example.urban_safety.ui.screens.profile.ProfileScreen
import com.example.urban_safety.ui.screens.SafetyScoreScreen
import com.example.urban_safety.ui.screens.testing.TestCenterScreen
import com.example.urban_safety.ui.screens.travel.TravelCheckInScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val startDestination = remember { "login" }
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Auth routes
        composable(route = "login") {
            LoginScreen(
                onNavigateToSignup = { navController.navigate("signup") },
                onLoginSuccess = { navController.navigate("home") {
                    popUpTo("login") { inclusive = true }
                }}
            )
        }
        
        composable(route = "signup") {
            SignupScreen(
                onNavigateToLogin = { navController.navigate("login") },
                onSignupSuccess = { navController.navigate("home") {
                    popUpTo("login") { inclusive = true }
                }}
            )
        }
        
        // Main app routes
        composable(route = "home") {
            HomeScreen(
                onNavigateToSafeRoutes = { navController.navigate("safe_routes") },
                onNavigateToManualSOS = { navController.navigate("manual_sos") },
                onNavigateToEmergencyContacts = { navController.navigate("emergency_contacts") },
                onNavigateToWearableMonitoring = { navController.navigate("ai_health_monitoring") },
                onNavigateToSafetyScore = { navController.navigate("safety_score") },
                onNavigateToTravelCheckIn = { navController.navigate("travel_checkin") },
                onNavigateToProfile = { navController.navigate("profile") },
                onNavigateToCommunityHelpers = { navController.navigate("community_helpers") },
                onNavigateToTestCenter = { navController.navigate("test_center") }
            )
        }
        
        composable(route = "safe_routes") {
            SafeRoutesScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(route = "manual_sos") {
            ManualSOSScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(route = "emergency_contacts") {
            EmergencyContactsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(route = "safety_score") {
            SafetyScoreScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(route = "profile") {
            ProfileScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Health monitoring screen
        composable(route = "ai_health_monitoring") {
            AIHealthMonitoringScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(route = "travel_checkin") {
            TravelCheckInScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(route = "community_helpers") {
            CommunityHelpersScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Test Center screen
        composable(route = "test_center") {
            TestCenterScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
} 