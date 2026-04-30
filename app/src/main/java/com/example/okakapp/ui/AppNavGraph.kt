package com.example.okakapp.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.okakapp.OkakApp
import com.example.okakapp.ui.auth.LoginScreen
import com.example.okakapp.ui.auth.RegisterScreen
import com.example.okakapp.ui.chat.ChatScreen
import com.example.okakapp.ui.chat.ChatsListScreen
import com.example.okakapp.ui.profile.ProfileScreen
import com.example.okakapp.ui.subscription.SubscriptionScreen
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
                onOpenSubscription = { navController.navigate(Routes.SUBSCRIPTION) },
                onOpenProfile = { navController.navigate(Routes.PROFILE) },
                onLogout = {
                    MainScope().launch {
                        OkakApp.get().authRepo.logout()
                        navController.navigateAsRoot(Routes.LOGIN)
                    }
                }
            )
        }
        composable(Routes.SUBSCRIPTION) {
            SubscriptionScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.PROFILE) {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onLoggedOut = { navController.navigateAsRoot(Routes.LOGIN) }
            )
        }
        composable(
            route = "${Routes.CHAT}/{${Routes.CHAT_ARG}}",
            arguments = listOf(navArgument(Routes.CHAT_ARG) { type = NavType.StringType })
        ) { entry ->
            val chatId = entry.arguments?.getString(Routes.CHAT_ARG).orEmpty()
            ChatScreen(
                chatId = chatId,
                onBack = { navController.popBackStack() }
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
