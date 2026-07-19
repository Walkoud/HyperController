package com.walkoud.hypercontroller.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.walkoud.hypercontroller.ui.screens.applist.AppListScreen
import com.walkoud.hypercontroller.ui.screens.applist.AppListViewModel
import com.walkoud.hypercontroller.ui.screens.detail.AppDetailScreen
import com.walkoud.hypercontroller.ui.screens.templates.TemplateEditor
import com.walkoud.hypercontroller.ui.screens.templates.TemplateScreen
import com.walkoud.hypercontroller.ui.screens.templates.TemplateViewModel
import com.walkoud.hypercontroller.ui.screens.settings.SettingsScreen

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    data object AppList : Screen("app_list", "Apps", Icons.Default.List)
    data object Templates : Screen("templates", "Templates", Icons.Default.ViewModule)
    data object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

val bottomNavItems = listOf(Screen.AppList, Screen.Templates, Screen.Settings)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    // Shared ViewModels scoped to the activity so state persists across destinations
    val appListViewModel: AppListViewModel = viewModel()
    val templateViewModel: TemplateViewModel = viewModel()

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(Screen.AppList.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.AppList.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            composable(Screen.AppList.route) {
                AppListScreen(
                    onAppClick = { pkgName ->
                        navController.navigate("app_detail/$pkgName")
                    },
                    viewModel = appListViewModel
                )
            }

            composable(
                route = "app_detail/{pkgName}",
                arguments = listOf(navArgument("pkgName") { type = NavType.StringType })
            ) { backStackEntry ->
                val pkgName = backStackEntry.arguments?.getString("pkgName") ?: return@composable
                AppDetailScreen(
                    pkgName = pkgName,
                    appListViewModel = appListViewModel,
                    onBack = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.Templates.route) {
                TemplateScreen(
                    onEditTemplate = { template ->
                        navController.navigate("template_editor/${template.id}")
                    },
                    onCreateTemplate = {
                        navController.navigate("template_editor/new")
                    },
                    viewModel = templateViewModel
                )
            }

            composable(
                route = "template_editor/{templateId}",
                arguments = listOf(navArgument("templateId") { type = NavType.StringType })
            ) { backStackEntry ->
                val templateId = backStackEntry.arguments?.getString("templateId") ?: ""
                val currentTemplates by templateViewModel.templates.collectAsState()
                val template = if (templateId == "new") null else currentTemplates.find { it.id == templateId }
                TemplateEditor(
                    template = template,
                    onSave = { savedTemplate ->
                        templateViewModel.saveUserTemplate(savedTemplate)
                        navController.popBackStack()
                    },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen()
            }
        }
    }
}
