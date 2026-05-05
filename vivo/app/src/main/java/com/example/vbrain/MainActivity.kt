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
import com.example.vbrain.presentation.snippet_list.HomeScreen
import com.example.vbrain.presentation.snippet_detail.SnippetDetailScreen
import com.example.vbrain.presentation.theme.VBrainTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            VBrainTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "home") {
                        // 首页列表
                        composable("home") {
                            HomeScreen(
                                onNavigateToDetail = { id ->
                                    navController.navigate("detail/$id")
                                },
                                onNavigateToAdd = {
                                    // 传 -1L 代表新建模式
                                    navController.navigate("detail/-1")
                                }
                            )
                        }
                        // 详情与编辑页
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