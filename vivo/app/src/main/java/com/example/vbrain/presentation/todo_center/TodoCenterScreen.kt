package com.example.vbrain.presentation.todo_center

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.vbrain.data.local.dao.TodoWithSnippet
import com.example.vbrain.presentation.theme.GitHubTextPrimary
import com.example.vbrain.presentation.theme.GitHubTextSecondary
import com.example.vbrain.presentation.theme.GitHubWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoCenterScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Long) -> Unit,
    viewModel: TodoCenterViewModel = hiltViewModel()
) {
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("未完成", "已完成")
    
    val uncompletedTodos by viewModel.uncompletedTodos.collectAsState()
    val completedTodos by viewModel.completedTodos.collectAsState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("待办中心", fontWeight = FontWeight.Bold, color = GitHubTextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = GitHubWhite)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = GitHubWhite
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            val listToShow = if (selectedTabIndex == 0) uncompletedTodos else completedTodos

            if (listToShow.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "无${tabs[selectedTabIndex]}待办",
                        color = GitHubTextSecondary,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(listToShow, key = { it.todo.id }) { item ->
                        TodoItemCard(
                            todoWithSnippet = item,
                            onToggle = { isChecked ->
                                viewModel.toggleTodoCompletion(item.todo.id, isChecked)
                            },
                            onClick = {
                                onNavigateToDetail(item.todo.snippetId)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TodoItemCard(
    todoWithSnippet: TodoWithSnippet,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = GitHubWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = todoWithSnippet.todo.isCompleted,
                onCheckedChange = { onToggle(it) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = todoWithSnippet.todo.taskContent,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textDecoration = if (todoWithSnippet.todo.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (todoWithSnippet.todo.isCompleted) GitHubTextSecondary else GitHubTextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                // 来源 Snippet 显示
                Text(
                    text = "来源: ${todoWithSnippet.snippet.summary.ifEmpty { "未命名知识" }}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

