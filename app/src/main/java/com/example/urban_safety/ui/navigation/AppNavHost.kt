package com.example.urban_safety.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.urban_safety.ui.screens.auth.LoginScreen
import com.example.urban_safety.ui.screens.auth.SignupScreen
import com.example.urban_safety.ui.screens.home.HomeScreen
import com.example.urban_safety.ui.screens.maps.SafeRoutesScreen
import com.example.urban_safety.ui.screens.onboarding.OnboardingScreen
import com.example.urban_safety.ui.screens.emergency.EmergencyContactsScreen
import com.example.urban_safety.ui.screens.emergency.ManualSOSScreen
import com.example.urban_safety.ui.screens.splash.SplashScreen
import com.example.urban_safety.ui.screens.testing.TestCenterScreen

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = NavDestination.Splash.route,
        modifier = modifier
    ) {
        composable(NavDestination.Splash.route) {
            SplashScreen(
                onNavigateToOnboarding = {
                    navController.navigate(NavDestination.Onboarding.route) {
                        popUpTo(NavDestination.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(NavDestination.Home.route) {
                        popUpTo(NavDestination.Splash.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(NavDestination.Onboarding.route) {
            OnboardingScreen(
                onFinish = {
                    navController.navigate(NavDestination.Login.route) {
                        popUpTo(NavDestination.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(NavDestination.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(NavDestination.Home.route) {
                        popUpTo(NavDestination.Login.route) { inclusive = true }
                    }
                },
                onNavigateToSignup = {
                    navController.navigate(NavDestination.Signup.route)
                }
            )
        }
        
        composable(NavDestination.Signup.route) {
            SignupScreen(
                onSignupSuccess = {
                    navController.navigate(NavDestination.Home.route) {
                        popUpTo(NavDestination.Signup.route) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(NavDestination.Login.route) {
                        popUpTo(NavDestination.Signup.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(NavDestination.Home.route) {
            HomeScreen(
                onNavigateToSafeRoutes = {
                    navController.navigate(NavDestination.SafeRoutes.route)
                },
                onNavigateToManualSOS = {
                    navController.navigate(NavDestination.ManualSOS.route)
                },
                onNavigateToEmergencyContacts = {
                    navController.navigate(NavDestination.EmergencyContacts.route)
                },
                onNavigateToWearableMonitoring = {
                    // TODO: Implement wearable monitoring screen
                },
                onNavigateToSafetyScore = {
                    // TODO: Implement safety score screen
                },
                onNavigateToTravelCheckIn = {
                    // TODO: Implement travel check-in screen
                },
                onNavigateToProfile = {
                    // TODO: Implement profile screen
                },
                onNavigateToCommunityHelpers = {
                    // TODO: Implement community helpers screen
                },
                onNavigateToTestCenter = {
                    navController.navigate(NavDestination.TestCenter.route)
                }
            )
        }
        
        composable(NavDestination.SafeRoutes.route) {
            SafeRoutesScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(NavDestination.ManualSOS.route) {
            ManualSOSScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(NavDestination.EmergencyContacts.route) {
            EmergencyContactsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        composable(NavDestination.TestCenter.route) {
            TestCenterScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

sealed class NavDestination(val route: String) {
    object Splash : NavDestination("splash")
    object Onboarding : NavDestination("onboarding")
    object Login : NavDestination("login")
    object Signup : NavDestination("signup")
    object Home : NavDestination("home")
    object SafeRoutes : NavDestination("safe_routes")
    object ManualSOS : NavDestination("manual_sos")
    object EmergencyContacts : NavDestination("emergency_contacts")
    object WearableMonitoring : NavDestination("wearable_monitoring")
    object SafetyScore : NavDestination("safety_score")
    object TravelCheckIn : NavDestination("travel_check_in")
    object Profile : NavDestination("profile")
    object TestCenter : NavDestination("test_center")
} 