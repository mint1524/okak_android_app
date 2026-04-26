package com.example.okakapp.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.okakapp.OkakApp
import com.example.okakapp.ui.auth.LoginScreen
import com.example.okakapp.ui.auth.RegisterScreen
import com.example.okakapp.ui.chat.ChatsListScreen
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun OkakNavHost() {
    val navController = rememberNavController()
    val tokenStorage = OkakApp.get().tokenStorage

    var startDestination by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        val token = tokenStorage.tokenFlow.first()
        startDestination = if (token.isNullOrBlank()) Routes.LOGIN else Routes.CHATS
    }

    val start = startDestination ?: return

    NavHost(navController = navController, startDestination = start) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoggedIn = { navController.navigateAsRoot(Routes.CHATS) },
                onGoToRegister = { navController.navigate(Routes.REGISTER) }
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen(
                onRegistered = { navController.navigateAsRoot(Routes.CHATS) },
                onGoToLogin = { navController.popBackStack() }
            )
        }
        composable(Routes.CHATS) {
            ChatsListScreen(
                onOpenChat = { navController.navigate(Routes.chat(it)) },
                onLogout = {
                    MainScope().launch {
                        OkakApp.get().authRepo.logout()
                        navController.navigateAsRoot(Routes.LOGIN)
                    }
                }
            )
        }
    }
}

private fun NavHostController.navigateAsRoot(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) { inclusive = true }
        launchSingleTop = true
    }
}
