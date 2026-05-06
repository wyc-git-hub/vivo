package com.example.vbrain

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.vbrain.presentation.main.MainScreen
import com.example.vbrain.presentation.snippet_detail.SnippetDetailScreen
import com.example.vbrain.presentation.snippet_list.HomeScreen
import com.example.vbrain.presentation.theme.VBrainTheme
import com.example.vbrain.presentation.todo_center.TodoCenterScreen
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VBrainTheme {
                val navController = rememberNavController()

                Surface(color = MaterialTheme.colorScheme.background) {
                    NavHost(navController = navController, startDestination = "main") {
                        composable("main") {
                            MainScreen(
                                onNavigateToDetail = { snippetId ->
                                    navController.navigate("detail/$snippetId")
                                },
                                onNavigateToAddSnippet = {
                                    navController.navigate("detail/-1")
                                },
                                onNavigateToAddTodo = {
                                    // TODO: Implement add todo navigation
                                }
                            )
                        }

                        composable(
                            route = "detail/{snippetId}",
                            arguments = listOf(navArgument("snippetId") { type = NavType.LongType })
                        ) {
                            SnippetDetailScreen(
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }
}