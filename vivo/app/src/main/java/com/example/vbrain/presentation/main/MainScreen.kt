package com.example.vbrain.presentation.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.vbrain.presentation.snippet_list.HomeScreen
import com.example.vbrain.presentation.todo_center.TodoCenterScreen
import com.example.vbrain.presentation.chat.AIChatScreen

enum class MainTab {
    HOME, TODO, REVIEW, AI_CHAT
}

@Composable
fun MainScreen(
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToAddSnippet: () -> Unit,
    onNavigateToAddTodo: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(MainTab.HOME) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                NavigationBarItem(
                    icon = { Icon(Icons.Rounded.Home, contentDescription = "知识") },
                    label = { Text("知识") },
                    selected = selectedTab == MainTab.HOME,
                    onClick = { selectedTab = MainTab.HOME }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Rounded.Checklist, contentDescription = "待办") },
                    label = { Text("待办") },
                    selected = selectedTab == MainTab.TODO,
                    onClick = { selectedTab = MainTab.TODO }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Rounded.Psychology, contentDescription = "回顾") },
                    label = { Text("回顾") },
                    selected = selectedTab == MainTab.REVIEW,
                    onClick = { selectedTab = MainTab.REVIEW }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = "大脑") },
                    label = { Text("大脑") },
                    selected = selectedTab == MainTab.AI_CHAT,
                    onClick = { selectedTab = MainTab.AI_CHAT }
                )
            }
        },
        floatingActionButton = {
            when (selectedTab) {
                MainTab.HOME -> {
                    FloatingActionButton(
                        onClick = onNavigateToAddSnippet,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "添加知识")
                    }
                }
                MainTab.TODO -> {
                    FloatingActionButton(
                        onClick = onNavigateToAddTodo,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "添加待办")
                    }
                }
                else -> {
                    // No FAB for Review and AI Chat
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                MainTab.HOME -> {
                    HomeScreen(
                        onNavigateToDetail = onNavigateToDetail,
                        onNavigateToAdd = onNavigateToAddSnippet,
                        onNavigateToTodoCenter = { selectedTab = MainTab.TODO }
                    )
                }
                MainTab.TODO -> {
                    TodoCenterScreen(
                        onNavigateBack = { selectedTab = MainTab.HOME },
                        onNavigateToDetail = onNavigateToDetail
                    )
                }
                MainTab.REVIEW -> {
                    // Placeholder for Review Screen
                    Text("Daily Review Screen (WIP)")
                }
                MainTab.AI_CHAT -> {
                    AIChatScreen()
                }
            }
        }
    }
}
