package com.example.chess_app.ui.navigation

import androidx.compose.runtime.*
import androidx.navigation.*
import androidx.navigation.compose.*
import com.example.chess_app.ui.screens.GameScreen
import com.example.chess_app.ui.screens.HomeScreen
import com.example.chess_app.ui.screens.WaitingScreen

sealed class Screen(val route: String) {
    object Home    : Screen("home")
    object Waiting : Screen("waiting/{roomId}/{color}") {
        fun createRoute(roomId: String, color: String) = "waiting/$roomId/$color"
    }
    object Game    : Screen("game/{color}") {
        fun createRoute(color: String) = "game/$color"
    }
}

@Composable
fun AppNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onGoToWaiting = { roomId, color ->
                    navController.navigate(Screen.Waiting.createRoute(roomId, color))
                },
                onGoToGame = { color ->
                    navController.navigate(Screen.Game.createRoute(color)) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                }
            )
        }

        composable(
            route = Screen.Waiting.route,
            arguments = listOf(
                navArgument("roomId") { type = NavType.StringType },
                navArgument("color")  { type = NavType.StringType }
            )
        ) { backStack ->
            val roomId = backStack.arguments?.getString("roomId") ?: ""
            val color  = backStack.arguments?.getString("color")  ?: "white"
            WaitingScreen(
                roomId  = roomId,
                myColor = color,
                onGameStarted = {
                    navController.navigate(Screen.Game.createRoute(color)) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                }
            )
        }

        composable(
            route = Screen.Game.route,
            arguments = listOf(navArgument("color") { type = NavType.StringType })
        ) { backStack ->
            val color = backStack.arguments?.getString("color") ?: "white"
            GameScreen(
                myColor = color,
                onNavigateHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
