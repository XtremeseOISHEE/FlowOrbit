package com.example.floworbit.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.example.floworbit.data.auth.AuthRepository
import com.example.floworbit.presentation.analytics.AnalyticsScreen
import com.example.floworbit.presentation.auth.LoginScreen
import com.example.floworbit.presentation.auth.RegistrationScreen
import com.example.floworbit.presentation.chat.ChatScreen
import com.example.floworbit.presentation.focus.FocusTimerScreen
import com.example.floworbit.presentation.home.HomeScreen
import com.example.floworbit.presentation.profile.ProfileScreen
import com.example.floworbit.presentation.task.TaskDetailScreen
import com.example.floworbit.ui.dnd.DNDPermissionScreen
import com.example.floworbit.presentation.dnd.DNDViewModel
import com.example.floworbit.presentation.blockedapps.BlockedAppsScreen
import com.example.floworbit.presentation.task.TaskScreen

object Routes {
    const val SPLASH = "splash"
    const val AUTH = "auth"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val HOME = "home"
    const val PROFILE = "profile"
    const val TASK_DETAIL = "task_detail"
    const val TASKS = "tasks"
    const val FOCUS = "focus"
    const val DND_PERMISSION = "dnd_permission"
    const val BLOCKED_APPS = "blocked_apps"
    const val ANALYTICS = "analytics"
    const val CHAT = "chat"
    const val SETTINGS = "settings"
}

@Composable
fun NavGraph(navController: NavHostController) {

    val authRepository = AuthRepository()

    NavHost(
        navController = navController,
        startDestination = Routes.SPLASH
    ) {

        composable(Routes.SPLASH) {
            LaunchedEffect(Unit) {
                val currentUser = authRepository.getCurrentUser()
                if (currentUser != null) {
                    navController.navigate(Routes.DND_PERMISSION) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                } else {
                    navController.navigate(Routes.AUTH) {
                        popUpTo(Routes.SPLASH) { inclusive = true }
                    }
                }
            }
        }

        navigation(startDestination = Routes.LOGIN, route = Routes.AUTH) {
            composable(Routes.LOGIN) {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Routes.DND_PERMISSION) {
                            popUpTo(Routes.AUTH) { inclusive = true }
                        }
                    },
                    onNavigateToRegister = {
                        navController.navigate(Routes.REGISTER)
                    }
                )
            }
            composable(Routes.REGISTER) {
                RegistrationScreen(
                    onRegistrationSuccess = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.LOGIN) { inclusive = true }
                        }
                    }
                )
            }
        }

        composable(Routes.PROFILE) {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onLogout = {
                    authRepository.logout()
                    navController.navigate(Routes.AUTH) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.CHAT) {
            ChatScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.DND_PERMISSION) {
            val dndViewModel: DNDViewModel = viewModel()
            LaunchedEffect(Unit) {
                dndViewModel.checkPermission()
            }
            val hasPermission = dndViewModel.hasPermission.collectAsState(initial = false).value
            LaunchedEffect(hasPermission) {
                if (hasPermission) {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.DND_PERMISSION) { inclusive = true }
                    }
                }
            }
            DNDPermissionScreen(
                navController = navController,
                dndViewModel = dndViewModel
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                onProfileClick = { navController.navigate(Routes.PROFILE) },
                onChatClick = { navController.navigate(Routes.CHAT) },
                onAnalyticsClick = { navController.navigate(Routes.ANALYTICS) },
                onSettingsClick = { navController.navigate(Routes.BLOCKED_APPS) },
                onFocusClick = { navController.navigate(Routes.FOCUS) },
                onTaskClick = { navController.navigate(Routes.TASKS) }
            )
        }
        
        composable(Routes.TASKS) {
            TaskScreen(
                onAddTask = {
                    navController.navigate("${Routes.TASK_DETAIL}/")
                },
                onTaskClick = {
                    navController.navigate("${Routes.TASK_DETAIL}/$it")
                }
            )
        }

        composable(Routes.ANALYTICS) {
            AnalyticsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.BLOCKED_APPS) {
             BlockedAppsScreen()
        }

        composable("${Routes.TASK_DETAIL}/{taskId}") { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId") ?: ""
            TaskDetailScreen(
                taskId = taskId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.FOCUS) {
            FocusTimerScreen(
                onBack = { navController.popBackStack() },
                onEditBlockedApps = { navController.navigate(Routes.BLOCKED_APPS) }
            )
        }
    }
}