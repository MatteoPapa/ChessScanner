package com.tutorials.chessscanner.nav

import android.net.Uri
import android.util.Log
import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.tutorials.chessscanner.screens.analysis_list.AnalysisListScreen
import com.tutorials.chessscanner.screens.chessboard.ChessBoardScreen
import com.tutorials.chessscanner.screens.fen_editor.FenEditorScreen
import com.tutorials.chessscanner.screens.login.LoginScreen
import com.tutorials.chessscanner.screens.menu.MenuScreen
import com.tutorials.chessscanner.screens.profile.ProfileScreen
import com.tutorials.chessscanner.util.AnalysisData

@Composable
fun AppNavigator() {
    val navController = rememberNavController()
    val auth = remember { FirebaseAuth.getInstance() }
    val isLoggedIn = remember { mutableStateOf(auth.currentUser != null) }

    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn.value) "menu" else "login"
    ) {
        composable("login") {
            LoginScreen(onLoginSuccess = {
                isLoggedIn.value = true
                navController.navigate("menu") {
                    popUpTo("login") { inclusive = true }
                }
            })
        }
        composable("menu") { MenuScreen(navController) }
        composable("profile") {
            ProfileScreen(onSignOut = {
                isLoggedIn.value = false
                navController.navigate("login") {
                    popUpTo("profile") { inclusive = true }
                }
            })
        }
        composable("analysis") {
            AnalysisListScreen(navController)
        }
        composable(
            route = "fen_editor/{fen}",
            arguments = listOf(navArgument("fen") { type = NavType.StringType })
        ) { backStackEntry ->
            val fen = Uri.decode(backStackEntry.arguments?.getString("fen") ?: "")
            FenEditorScreen(initialFen = fen, navController = navController)
        }
        composable("chessboard") {
            val analysis = navController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<AnalysisData>("analysis")

            if (analysis != null) {
                ChessBoardScreen(analysis = analysis)
            } else {
                Log.e("AppNavigator", "No analysis found for ChessBoardScreen.")
            }
        }
    }
}
