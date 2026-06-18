package com.psy.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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

        // Placeholder: Task 6 will replace the body with AddEditTransactionScreen
        composable(
            route = Routes.ADD_EDIT_PATTERN,
            arguments = listOf(
                navArgument(Routes.ARG_TX_ID) {
                    type = NavType.LongType
                    defaultValue = -1L
                },
            ),
        ) {
            // TODO Task 6: AddEditTransactionScreen(onDone = { navController.popBackStack() })
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "Add/Edit — coming in Task 6")
            }
        }
    }
}
