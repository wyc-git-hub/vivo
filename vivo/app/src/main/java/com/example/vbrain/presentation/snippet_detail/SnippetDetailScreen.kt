package com.example.vbrain.presentation.snippet_detail

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
    val context = LocalContext.current

    var summaryText by remember { mutableStateOf("") }
    var originalText by remember { mutableStateOf("") }
    var tagsList by remember { mutableStateOf<List<String>>(emptyList()) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showChatSheet by remember { mutableStateOf(false) }
    var chatInput by remember { mutableStateOf("") }

    var newTagText by remember { mutableStateOf("") }

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
        containerColor = GitHubWhite,
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("知识详情", fontWeight = FontWeight.Bold, color = GitHubTextPrimary) },
                    navigationIcon = {
                        // 🌟 点击返回自动保存
                        IconButton(onClick = { saveAndExit() }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = GitHubTextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = GitHubWhite),
                    actions = {
                        // 系统分享按钮
                        IconButton(onClick = {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, "【V-Brain 知识摘要】\n$summaryText\n\n【原文内容】\n$originalText")
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "分享知识"))
                        }) {
                            Icon(Icons.Rounded.Share, contentDescription = "Share", tint = GitHubTextSecondary)
                        }
                        // 删除按钮
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                )
                HorizontalDivider(color = GitHubBorder, thickness = 1.dp)
            }
        },
        floatingActionButton = {
            if (snippet != null) {
                FloatingActionButton(
                    onClick = { showChatSheet = true },
                    containerColor = GitHubAccentBlue,
                    contentColor = GitHubWhite
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Chat Context")
                }
            }
        }
    ) { padding ->
        // 🌟 UX 优化：如果数据还在加载中，显示 Loading，避免白屏闪烁
        if (snippet == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = GitHubAccentBlue)
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

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
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            Text("Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = GitHubTextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = summaryText,
                onValueChange = { summaryText = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = GitHubBg,
                    focusedContainerColor = GitHubWhite,
                    unfocusedBorderColor = GitHubBorder,
                    focusedBorderColor = GitHubAccentBlue,
                    cursorColor = GitHubAccentBlue
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text("Tags", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = GitHubTextPrimary)
            Spacer(modifier = Modifier.height(8.dp))

            // 🌟 UX 优化：使用 FlowRow 替代 LazyRow，让标签自动换行，不用横向苦苦滑动
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                tagsList.forEach { tag ->
                    InputChip(
                        selected = true,
                        onClick = { tagsList = tagsList.filter { it != tag } },
                        label = { Text(tag) },
                        trailingIcon = { Icon(Icons.Rounded.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp)) },
                        colors = InputChipDefaults.inputChipColors(
                            containerColor = GitHubTagBg,
                            selectedContainerColor = GitHubTagBg,
                            labelColor = GitHubTagText,
                            trailingIconColor = GitHubTagText
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

            Spacer(modifier = Modifier.height(12.dp))

            // 添加新标签区域
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newTagText,
                    onValueChange = { newTagText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Add a tag...", color = GitHubTextSecondary) },
                    singleLine = true,
                    shape = RoundedCornerShape(6.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = GitHubBg,
                        focusedContainerColor = GitHubWhite,
                        unfocusedBorderColor = GitHubBorder,
                        focusedBorderColor = GitHubAccentBlue,
                        cursorColor = GitHubAccentBlue
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
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GitHubBg, contentColor = GitHubTextPrimary),
                    border = BorderStroke(1.dp, GitHubBorder)
                ) {
                    Text("Add", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text("Formatted Text", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = GitHubTextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = GitHubBg,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, GitHubBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    MarkdownText(
                        markdown = originalText,
                        color = GitHubTextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            snippet?.sourceUrl?.let { url ->
                Button(
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(6.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GitHubAccentBlue)
                ) {
                    Text("查看原文", fontWeight = FontWeight.Bold, color = GitHubWhite)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 底部的显式保存按钮 (照顾那些不知道“返回会自动保存”的用户)
            Button(
                onClick = { saveAndExit() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(6.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GitHubGreen)
            ) {
                Text("Save Snippet", fontWeight = FontWeight.Bold, color = GitHubWhite)
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showChatSheet) {
        ModalBottomSheet(
            onDismissRequest = { showChatSheet = false },
            containerColor = GitHubBg,
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
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
                    color = GitHubTextPrimary,
                    modifier = Modifier.padding(16.dp)
                )
                HorizontalDivider(color = GitHubBorder)

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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                    items(chatMessages) { msg ->
                        val isUser = msg.role == "user"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                        ) {
                            Surface(
                                color = if (isUser) GitHubAccentBlue else GitHubWhite,
                                shape = RoundedCornerShape(
                                    topStart = 12.dp,
                                    topEnd = 12.dp,
                                    bottomStart = if (isUser) 12.dp else 4.dp,
                                    bottomEnd = if (isUser) 4.dp else 12.dp
                                ),
                                border = if (isUser) null else BorderStroke(1.dp, GitHubBorder),
                                modifier = Modifier.widthIn(max = 280.dp)
                            ) {
                                Box(modifier = Modifier.padding(12.dp)) {
                                    if (isUser) {
                                        Text(msg.content, color = GitHubWhite)
                                    } else {
                                        MarkdownText(
                                            markdown = msg.content,
                                            color = GitHubTextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(8.dp)) }
                }

                HorizontalDivider(color = GitHubBorder)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = chatInput,
                        onValueChange = { chatInput = it },
                        placeholder = { Text("Ask something...", color = GitHubTextSecondary) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = GitHubWhite,
                            focusedContainerColor = GitHubWhite,
                            unfocusedBorderColor = GitHubBorder,
                            focusedBorderColor = GitHubAccentBlue
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
                        containerColor = GitHubAccentBlue,
                        contentColor = GitHubWhite,
                        modifier = Modifier.size(48.dp)
                    ) {
                        if (isChatLoading) {
                            CircularProgressIndicator(
                                color = GitHubWhite,
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