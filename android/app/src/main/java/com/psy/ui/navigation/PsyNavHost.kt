package com.psy.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.psy.ui.addedit.AddEditTransactionScreen
import com.psy.ui.home.HomeScreen

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
    }
}
