package com.psy.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.psy.ui.addedit.AddEditTransactionScreen
import com.psy.ui.home.HomeScreen
import com.psy.ui.manage.account.ManageAccountsScreen
import com.psy.ui.manage.category.ManageCategoriesScreen
import com.psy.ui.settings.SettingsScreen

@Composable
fun PsyNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onAddClick = { navController.navigate(Routes.addEdit()) },
                onTxClick = { id -> navController.navigate(Routes.addEdit(id)) },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) },
            )
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
