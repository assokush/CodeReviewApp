package com.codereview.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.codereview.ui.theme.CodeReviewAITheme

@Composable
fun App() {
    CodeReviewAITheme {
        val navController = rememberNavController()
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        Scaffold(
            bottomBar = {
                if (currentRoute == "review" || currentRoute == "history") {
                    NavigationBar {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Search, contentDescription = "Review") },
                            label = { Text("Review") },
                            selected = currentRoute == "review",
                            onClick = {
                                navController.navigate("review") {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.DateRange, contentDescription = "History") },
                            label = { Text("History") },
                            selected = currentRoute == "history",
                            onClick = {
                                navController.navigate("history") {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        ) { _ ->
            NavHost(navController, startDestination = "review") {
                composable("review") { ReviewScreen() }
                composable("history") {
                    HistoryScreen(onItemClick = { id ->
                        navController.navigate("detail/$id")
                    })
                }
                composable("detail/{id}") { backStackEntry ->
                    val id = backStackEntry.savedStateHandle.get<String>("id")?.toIntOrNull()
                        ?: return@composable
                    DetailScreen(id = id, onBack = { navController.popBackStack() })
                }
            }
        }
    }
}
