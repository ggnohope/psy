package com.psy.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.psy.ui.addedit.AddEditTransactionScreen
import com.psy.ui.budget.BudgetScreen
import com.psy.ui.calendar.CalendarScreen
import com.psy.ui.home.HomeScreen
import com.psy.ui.manage.account.ManageAccountsScreen
import com.psy.ui.manage.category.ManageCategoriesScreen
import com.psy.ui.settings.SettingsScreen
import com.psy.ui.stats.StatsScreen

private val bottomBarRoutes = setOf(Routes.HOME, Routes.STATS, Routes.CALENDAR, Routes.BUDGET)

@Composable
fun PsyNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute in bottomBarRoutes) {
                PsyBottomBar(currentRoute = currentRoute) { route ->
                    navController.navigate(route) {
                        launchSingleTop = true
                        restoreState = true
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    onAddClick = { navController.navigate(Routes.addEdit()) },
                    onTxClick = { id -> navController.navigate(Routes.addEdit(id)) },
                    onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                )
            }

            composable(Routes.STATS) {
                StatsScreen()
            }

            composable(Routes.CALENDAR) {
                CalendarScreen()
            }

            composable(Routes.BUDGET) {
                BudgetScreen()
            }

            composable(
                route = Routes.ADD_EDIT_PATTERN,
                arguments = listOf(
                    navArgument(Routes.ARG_TX_ID) {
                        type = NavType.LongType
                        defaultValue = -1L
                    },
                ),
            ) {
                // ViewModel reads txId from SavedStateHandle automatically via Hilt.
                AddEditTransactionScreen(onDone = { navController.popBackStack() })
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onManageCategories = { navController.navigate(Routes.MANAGE_CATEGORIES) },
                    onManageAccounts = { navController.navigate(Routes.MANAGE_ACCOUNTS) },
                )
            }

            composable(Routes.MANAGE_CATEGORIES) {
                ManageCategoriesScreen(onBack = { navController.popBackStack() })
            }

            composable(Routes.MANAGE_ACCOUNTS) {
                ManageAccountsScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
