package com.vikas.tryon.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vikas.tryon.presentation.avatar.AvatarScreen
import com.vikas.tryon.presentation.camera.CameraScreen
import com.vikas.tryon.presentation.garment.GarmentScreen
import com.vikas.tryon.presentation.home.HomeScreen
import com.vikas.tryon.presentation.measurement.MeasurementScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onStartTryOn = { navController.navigate(Screen.Camera.route) },
                onOpenAvatar = { navController.navigate(Screen.Avatar.route) },
                onOpenGarments = { navController.navigate(Screen.Garment.route) }
            )
        }
        composable(Screen.Camera.route) {
            CameraScreen(
                onNavigateBack = { navController.popBackStack() },
                onOpenGarments = { navController.navigate(Screen.Garment.route) },
                onOpenMeasurements = { navController.navigate(Screen.Measurement.route) }
            )
        }
        composable(Screen.Garment.route) {
            GarmentScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Avatar.route) {
            AvatarScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Measurement.route) {
            MeasurementScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
