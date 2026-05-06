package com.example.vbrain.presentation.snippet_list

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.vbrain.data.local.entity.KnowledgeSnippet
import com.example.vbrain.presentation.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: SnippetListViewModel = hiltViewModel(),
    onNavigateToDetail: (Long) -> Unit,
    onNavigateToAdd: () -> Unit, // 🌟 新增：手动录入入口
    onNavigateToTodoCenter: () -> Unit // 🌟 新增：待办中心入口
) {
    val snippets by viewModel.snippets.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTag by viewModel.selectedTag.collectAsState()
    val availableTags by viewModel.availableTags.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // 多选与确认弹窗状态
    val isSelectionMode by viewModel.isSelectionMode.collectAsState()
    val selectedSnippetIds by viewModel.selectedSnippetIds.collectAsState()
    var showClearConfirmDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.importFiles(uris, context)
        }
    }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                // 多选模式下的顶部栏
                TopAppBar(
                    title = { Text("已选择 ${selectedSnippetIds.size} 项", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.exitSelectionMode() }) {
                            Icon(Icons.Rounded.Close, contentDescription = "取消")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.selectAllSnippets() }) {
                            Icon(Icons.Rounded.SelectAll, contentDescription = "全选", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { viewModel.deleteSelectedSnippets() }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
                )
            } else {
                // 普通模式下的顶部栏
                TopAppBar(
                    title = { Text("我的知识大脑", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
                    actions = {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(end = 16.dp), strokeWidth = 2.dp)
                        } else {
                            IconButton(onClick = { filePickerLauncher.launch("text/*") }) {
                                Icon(Icons.Rounded.FileUpload, contentDescription = "Import", tint = MaterialTheme.colorScheme.onSurface)
                            }
                            IconButton(onClick = { viewModel.processUnsummarizedSnippets() }) {
                                Icon(Icons.Rounded.CloudSync, contentDescription = "Sync", tint = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                        if (snippets.isNotEmpty()) {
                            IconButton(onClick = { showClearConfirmDialog = true }) {
                                Icon(Icons.Rounded.DeleteSweep, contentDescription = "快速清空", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
        // 移除 FAB，由 MainScreen 统一管理
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("搜索我的知识...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = { /* TODO: Sort actions */ }, modifier = Modifier.background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(100))) {
                    Icon(Icons.Rounded.Sort, contentDescription = "Sort", tint = MaterialTheme.colorScheme.onSurface)
                }
            }
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedTag == "全部" || selectedTag == null,
                        onClick = { viewModel.onTagSelect(null) },
                        label = { Text("全部") },
                        shape = RoundedCornerShape(100),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = Color.Transparent,
                            selectedBorderColor = Color.Transparent,
                            enabled = true,
                            selected = selectedTag == "全部" || selectedTag == null
                        )
                    )
                }
                items(availableTags) { tag ->
                    FilterChip(
                        selected = selectedTag == tag,
                        onClick = { viewModel.onTagSelect(tag) },
                        label = { Text(tag) },
                        shape = RoundedCornerShape(100),
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = Color.Transparent,
                            selectedBorderColor = Color.Transparent,
                            enabled = true,
                            selected = selectedTag == tag
                        )
                    )
                }
            }
            if (snippets.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Rounded.Inbox, contentDescription = "Empty", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.surfaceVariant)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = "暂无知识碎片", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "点击右下角 + 号或使用系统分享获取第一条知识吧！", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onBackground.copy(alpha=0.6f), textAlign = TextAlign.Center)
                }
            } else {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Adaptive(160.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalItemSpacing = 16.dp,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(snippets, key = { it.id }) { snippet ->
                        val isSelected = selectedSnippetIds.contains(snippet.id)

                        // 侧滑删除控制
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { dismissValue ->
                                if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                    viewModel.deleteSnippet(snippet)
                                    true
                                } else false
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            backgroundContent = {
                                val color by animateColorAsState(
                                    if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) MaterialTheme.colorScheme.errorContainer else Color.Transparent, label = ""
                                )
                                Box(
                                    modifier = Modifier.fillMaxSize().background(color, MaterialTheme.shapes.large).padding(horizontal = 24.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(Icons.Rounded.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.onErrorContainer)
                                }
                            }
                        ) {
                            KnowledgeCard(
                                snippet = snippet,
                                isSelectionMode = isSelectionMode,
                                isSelected = isSelected,
                                onClick = {
                                    // 🌟 根据模式切换点击行为：多选打勾 OR 跳转独立详情页
                                    if (isSelectionMode) viewModel.toggleSnippetSelection(snippet.id)
                                    else onNavigateToDetail(snippet.id)
                                },
                                onLongClick = {
                                    if (!isSelectionMode) viewModel.enterSelectionMode(snippet.id)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // 快速清空确认弹窗
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("快速清空") },
            text = { Text("确定要删除当前列表下的所有知识碎片吗？此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearCurrentList()
                    showClearConfirmDialog = false
                }) { Text("清空", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) { Text("取消") }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun KnowledgeCard(
    snippet: KnowledgeSnippet,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val context = LocalContext.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = if (isSelected) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Description, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = snippet.source, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = dateFormat.format(Date(snippet.timestamp)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = snippet.summary.ifEmpty { "未提纯的知识碎片 (待处理)" },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = MaterialTheme.typography.titleMedium.lineHeight,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(12.dp))
            
            // 🌟 解决横排挤压：使用 FlowRow 自动换行
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                snippet.tags.take(6).forEach { tag ->
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Text(text = tag, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}