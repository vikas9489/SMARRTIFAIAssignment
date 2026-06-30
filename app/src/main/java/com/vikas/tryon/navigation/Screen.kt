package com.vikas.tryon.navigation

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Camera : Screen("camera")
    data object Garment : Screen("garment")
    data object Avatar : Screen("avatar")
    data object Measurement : Screen("measurement")
}
