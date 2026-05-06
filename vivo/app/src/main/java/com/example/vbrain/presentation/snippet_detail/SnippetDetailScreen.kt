package com.example.vbrain.presentation.snippet_detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.vbrain.presentation.theme.*
import dev.jeziellago.compose.markdowntext.MarkdownText
import java.io.File
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.automirrored.rounded.Send

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SnippetDetailScreen(
    onBack: () -> Unit,
    viewModel: SnippetDetailViewModel = hiltViewModel()
) {
    val snippet by viewModel.snippet.collectAsState()
    val chatMessages by viewModel.chatMessages.collectAsState()
    val isChatLoading by viewModel.isChatLoading.collectAsState()
    val todos by viewModel.todos.collectAsState()
    val context = LocalContext.current

    var summaryText by remember { mutableStateOf("") }
    var originalText by remember { mutableStateOf("") }
    var tagsList by remember { mutableStateOf<List<String>>(emptyList()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showChatSheet by remember { mutableStateOf(false) }
    var chatInput by remember { mutableStateOf("") }
    var newTagText by remember { mutableStateOf("") }

    // 🌟 状态管理：是否处于编辑模式
    var isEditing by remember { mutableStateOf(false) }

    // 初始化表单数据
    LaunchedEffect(snippet) {
        snippet?.let {
            if (summaryText.isEmpty() && originalText.isEmpty() && tagsList.isEmpty()) {
                summaryText = it.summary
                // 显示经过 Markdown 排版的内容 (如果有)，否则显示原文
                originalText = if (it.formattedText.isNotBlank()) it.formattedText else it.originalText
                tagsList = it.tags
            }
        }
    }

    // 🌟 自动保存逻辑：兼顾正常修改和全新创建
    fun saveAndExit() {
        val currentSnippet = snippet
        if (currentSnippet != null && (summaryText != currentSnippet.summary || originalText != currentSnippet.originalText || tagsList != currentSnippet.tags)) {
            viewModel.saveSnippet(summaryText, tagsList, originalText)
        } else if (currentSnippet == null && (summaryText.isNotEmpty() || originalText.isNotEmpty())) {
            // 防止全新创建时 snippet 还没完全加载出来的情况
            viewModel.saveSnippet(summaryText, tagsList, originalText)
        }
        onBack()
    }

    // 危险操作二次确认弹窗
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("彻底删除这条知识？此操作不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSnippet()
                    showDeleteDialog = false
                    onBack()
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { /* Empty title for immersive look */ },
                navigationIcon = {
                    // 🌟 点击返回自动保存
                    IconButton(onClick = { saveAndExit() }) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            BottomAppBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                tonalElevation = 8.dp,
                actions = {
                    if (isEditing) {
                        IconButton(onClick = {
                            viewModel.saveSnippet(summaryText, tagsList, originalText)
                            isEditing = false
                        }) {
                            Icon(Icons.Rounded.Check, contentDescription = "Save", tint = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        IconButton(onClick = { isEditing = true }) {
                            Icon(Icons.Rounded.Edit, contentDescription = "Edit")
                        }
                        IconButton(onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "【V-Brain 知识摘要】\n$summaryText\n\n【原文内容】\n$originalText")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "分享知识"))
                        }) {
                            Icon(Icons.Rounded.Share, contentDescription = "Share")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                floatingActionButton = {
                    if (snippet != null) {
                        FloatingActionButton(
                            onClick = { showChatSheet = true },
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            shape = MaterialTheme.shapes.large
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Chat Context")
                        }
                    }
                }
            )
        }
    ) { padding ->
        // 🌟 UX 优化：如果数据还在加载中，显示 Loading，避免白屏闪烁
        if (snippet == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            snippet?.imagePaths?.let { images ->
                if (images.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(images) { imagePath ->
                            AsyncImage(
                                model = File(imagePath),
                                contentDescription = null,
                                modifier = Modifier
                                    .height(200.dp)
                                    .width(200.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            if (isEditing) {
                OutlinedTextField(
                    value = summaryText,
                    onValueChange = { summaryText = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.titleLarge,
                    placeholder = { Text("Title", style = MaterialTheme.typography.titleLarge) },
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = Color.Transparent,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )
            } else {
                Text(
                    text = summaryText.ifEmpty { "Title" }, 
                    style = MaterialTheme.typography.titleLarge, 
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 🌟 UX 优化：使用 FlowRow 替代 LazyRow，让标签自动换行，不用横向苦苦滑动
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                tagsList.forEach { tag ->
                    InputChip(
                        selected = true,
                        onClick = { if(isEditing) tagsList = tagsList.filter { it != tag } },
                        label = { Text(tag) },
                        trailingIcon = if(isEditing) { { Icon(Icons.Rounded.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp)) } } else null,
                        colors = InputChipDefaults.inputChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            trailingIconColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        border = InputChipDefaults.inputChipBorder(
                            borderColor = Color.Transparent,
                            selectedBorderColor = Color.Transparent,
                            enabled = true,
                            selected = true
                        ),
                        shape = RoundedCornerShape(100)
                    )
                }
            }

            if (isEditing) {
                Spacer(modifier = Modifier.height(12.dp))
                // 添加新标签区域
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newTagText,
                        onValueChange = { newTagText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Add a tag...", color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.5f)) },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            cursorColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (newTagText.isNotBlank() && !tagsList.contains(newTagText.trim())) {
                                tagsList = tagsList + newTagText.trim()
                                newTagText = ""
                            }
                        },
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                    ) {
                        Text("Add", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            if (todos.isNotEmpty()) {
                Text("行动清单 (Actionable Items)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = MaterialTheme.shapes.large,
                    shadowElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        todos.forEach { todo ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.toggleTodoCompletion(todo.id, !todo.isCompleted) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = todo.isCompleted,
                                    onCheckedChange = { isChecked -> viewModel.toggleTodoCompletion(todo.id, isChecked) }
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = todo.taskContent,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                                    ),
                                    color = if (todo.isCompleted) MaterialTheme.colorScheme.onSurface.copy(alpha=0.5f) else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            if (isEditing) {
                // 编辑模式：允许用户直接修改 Markdown 文本
                OutlinedTextField(
                    value = originalText,
                    onValueChange = { originalText = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 300.dp),
                    shape = MaterialTheme.shapes.large,
                    textStyle = MaterialTheme.typography.bodyLarge,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        cursorColor = MaterialTheme.colorScheme.primary
                    )
                )
            } else {
                // 预览模式：无边界大字号沉浸阅读
                Box(modifier = Modifier.fillMaxWidth()) {
                    MarkdownText(
                        markdown = originalText,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            snippet?.sourceUrl?.let { url ->
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                ) {
                    Text("查看原文", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(64.dp))
        }
    }

    if (showChatSheet) {
        ModalBottomSheet(
            onDismissRequest = { showChatSheet = false },
            containerColor = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
            ) {
                Text(
                    text = "Contextual Chat",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(16.dp)
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                val listState = rememberLazyListState()
                LaunchedEffect(chatMessages.size) {
                    if (chatMessages.isNotEmpty()) {
                        listState.animateScrollToItem(chatMessages.size - 1)
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                    items(chatMessages) { msg ->
                        val isUser = msg.role == "user"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                        ) {
                            Surface(
                                color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                shape = RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isUser) 16.dp else 4.dp,
                                    bottomEnd = if (isUser) 4.dp else 16.dp
                                ),
                                shadowElevation = 0.dp,
                                modifier = Modifier.widthIn(max = 280.dp)
                            ) {
                                Box(modifier = Modifier.padding(16.dp)) {
                                    if (isUser) {
                                        Text(msg.content, color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.bodyLarge)
                                    } else {
                                        MarkdownText(
                                            markdown = msg.content,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = chatInput,
                        onValueChange = { chatInput = it },
                        placeholder = { Text("Ask something...", color = MaterialTheme.colorScheme.onSurface.copy(alpha=0.5f)) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FloatingActionButton(
                        onClick = {
                            if (chatInput.isNotBlank() && !isChatLoading) {
                                viewModel.sendMessage(chatInput.trim(), originalText)
                                chatInput = ""
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(56.dp),
                        shape = RoundedCornerShape(28.dp)
                    ) {
                        if (isChatLoading) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send")
                        }
                    }
                }
                Spacer(modifier = Modifier.navigationBarsPadding())
            }
        }
    }
}