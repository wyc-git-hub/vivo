package com.example.vbrain.presentation.snippet_list

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
    onNavigateToAdd: () -> Unit // 🌟 新增：手动录入入口
) {
    val snippets by viewModel.snippets.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedTag by viewModel.selectedTag.collectAsState()
    val availableTags by viewModel.availableTags.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isChatSheetVisible by viewModel.isChatSheetVisible.collectAsState()
    val chatInput by viewModel.chatInput.collectAsState()
    val chatReply by viewModel.chatReply.collectAsState()

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
                            Icon(Icons.Rounded.SelectAll, contentDescription = "全选", tint = GitHubTextPrimary)
                        }
                        IconButton(onClick = { viewModel.deleteSelectedSnippets() }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "删除", tint = MaterialTheme.colorScheme.error)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = GitHubWhite)
                )
            } else {
                // 普通模式下的顶部栏
                TopAppBar(
                    title = { Text("V-Brain 知识库", fontWeight = FontWeight.Bold, color = GitHubTextPrimary) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = GitHubWhite),
                    actions = {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp).padding(end = 16.dp), strokeWidth = 2.dp)
                        } else {
                            IconButton(onClick = { filePickerLauncher.launch("text/*") }) {
                                Icon(Icons.Rounded.FileUpload, contentDescription = "Import", tint = GitHubTextSecondary)
                            }
                            IconButton(onClick = { viewModel.processUnsummarizedSnippets() }) {
                                Icon(Icons.Rounded.CloudSync, contentDescription = "Sync", tint = GitHubTextSecondary)
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
            HorizontalDivider(color = GitHubBorder, thickness = 1.dp)
        },
        containerColor = GitHubBg,
        floatingActionButton = {
            if (!isSelectionMode) { // 多选时隐藏悬浮窗，防误触
                Column(horizontalAlignment = Alignment.End) {
                    // 🌟 新增：手动录入按钮 (小号悬浮按钮，不抢戏)
                    SmallFloatingActionButton(
                        onClick = onNavigateToAdd,
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Manual Add")
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    // 原有的 AI 问答按钮
                    FloatingActionButton(
                        onClick = { viewModel.toggleChatSheet(true) },
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = "AI Q&A", tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())

            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("搜索知识、摘要或标签...", color = GitHubTextSecondary) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search", tint = GitHubTextSecondary) },
                singleLine = true,
                shape = RoundedCornerShape(6.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = GitHubWhite,
                    focusedContainerColor = GitHubWhite,
                    unfocusedBorderColor = GitHubBorder,
                    focusedBorderColor = GitHubAccentBlue,
                    cursorColor = GitHubAccentBlue
                )
            )
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
                            containerColor = GitHubWhite,
                            selectedContainerColor = GitHubWhite,
                            labelColor = GitHubTextSecondary,
                            selectedLabelColor = GitHubAccentBlue
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = GitHubBorder,
                            selectedBorderColor = GitHubAccentBlue,
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
                            containerColor = GitHubWhite,
                            selectedContainerColor = GitHubWhite,
                            labelColor = GitHubTextSecondary,
                            selectedLabelColor = GitHubAccentBlue
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            borderColor = GitHubBorder,
                            selectedBorderColor = GitHubAccentBlue,
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
                    Text(text = "暂无知识碎片", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "点击右下角 + 号或使用系统分享获取第一条知识吧！", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
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
                                    if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) MaterialTheme.colorScheme.errorContainer else Color.Transparent
                                )
                                Box(
                                    modifier = Modifier.fillMaxSize().background(color, RoundedCornerShape(20.dp)).padding(horizontal = 24.dp),
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
            text = { Text("确定要删除当前列表下的���有知识碎片吗？此操作不可恢复。") },
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

    // AI 问答抽屉
    if (isChatSheetVisible) {
        ModalBottomSheet(onDismissRequest = { viewModel.toggleChatSheet(false) }, containerColor = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 32.dp)) {
                Text("V-Brain 问答", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                if (chatReply.isNotEmpty()) {
                    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Text(text = chatReply, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = chatInput, onValueChange = viewModel::onChatInputChange, modifier = Modifier.weight(1f),
                        placeholder = { Text("向 V-Brain 提问...") }, shape = RoundedCornerShape(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (chatInput.isNotBlank()) {
                                viewModel.askQuestion()
                                viewModel.onChatInputChange("")
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.background(MaterialTheme.colorScheme.primary, shape = RoundedCornerShape(100))
                    ) {
                        Icon(Icons.Rounded.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
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

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.outlinedCardColors(containerColor = GitHubWhite),
        border = BorderStroke(1.dp, if (isSelected) GitHubAccentBlue else GitHubBorder),
        elevation = CardDefaults.outlinedCardElevation(0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Description, contentDescription = null, modifier = Modifier.size(16.dp), tint = GitHubTextSecondary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = snippet.source, style = MaterialTheme.typography.labelSmall, color = GitHubTextPrimary, fontWeight = FontWeight.Bold)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = dateFormat.format(Date(snippet.timestamp)), style = MaterialTheme.typography.labelSmall, color = GitHubTextSecondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        // 保留剪贴板复制功能
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("vbrain", snippet.summary + "\n\n" + snippet.originalText)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(14.dp), tint = GitHubTextSecondary)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                // 🌟 ��显示摘要，完整原文统一放在详情页展示，保持列表清爽
                Text(
                    text = snippet.summary.ifEmpty { "未提纯的知识碎片 (待处理)" },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = GitHubAccentBlue,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start, verticalAlignment = Alignment.CenterVertically) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        snippet.tags.take(3).forEach { tag ->
                            Surface(shape = RoundedCornerShape(100), color = GitHubTagBg) {
                                Text(text = tag, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = GitHubTagText, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}