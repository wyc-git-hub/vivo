package com.example.vbrain.presentation.snippet_detail

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.vbrain.presentation.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnippetDetailScreen(
    onBack: () -> Unit,
    viewModel: SnippetDetailViewModel = hiltViewModel()
) {
    val snippet by viewModel.snippet.collectAsState()
    val context = LocalContext.current

    var summaryText by remember { mutableStateOf("") }
    var originalText by remember { mutableStateOf("") }
    var tagsList by remember { mutableStateOf<List<String>>(emptyList()) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    var newTagText by remember { mutableStateOf("") }

    // Init form state
    LaunchedEffect(snippet) {
        snippet?.let {
            if (summaryText.isEmpty() && originalText.isEmpty() && tagsList.isEmpty()) {
                summaryText = it.summary
                originalText = it.originalText
                tagsList = it.tags
            }
        }
    }

    // Save on dispose or navigate back via DisposableEffect or just back button
    // Here we can save explicitly on back button
    fun saveAndExit() {
        val currentSnippet = snippet
        if (currentSnippet != null && (summaryText != currentSnippet.summary || originalText != currentSnippet.originalText || tagsList != currentSnippet.tags)) {
            viewModel.saveSnippet(summaryText, tagsList, originalText)
        }
        onBack()
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("确认删除") },
            text = { Text("彻底删除这条知识？") },
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
                    title = { Text("Edit Snippet", fontWeight = FontWeight.Bold, color = GitHubTextPrimary) },
                    navigationIcon = {
                        IconButton(onClick = { saveAndExit() }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = GitHubTextPrimary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = GitHubWhite),
                    actions = {
                        IconButton(onClick = {
                            val sendIntent: android.content.Intent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                putExtra(android.content.Intent.EXTRA_TEXT, summaryText + "\n\n" + originalText)
                                type = "text/plain"
                            }
                            val shareIntent = android.content.Intent.createChooser(sendIntent, null)
                            context.startActivity(shareIntent)
                        }) {
                            Icon(Icons.Rounded.Share, contentDescription = "Share", tint = GitHubTextSecondary)
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                )
                HorizontalDivider(color = GitHubBorder, thickness = 1.dp)
            }
        }
    ) { padding ->
        snippet?.let { current ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(16.dp))

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
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(tagsList) { tag ->
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
                Spacer(modifier = Modifier.height(8.dp))
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
                Text("Original Text", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = GitHubTextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = originalText,
                    onValueChange = { originalText = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
                    shape = RoundedCornerShape(6.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = GitHubBg,
                        focusedContainerColor = GitHubWhite,
                        unfocusedBorderColor = GitHubBorder,
                        focusedBorderColor = GitHubAccentBlue,
                        cursorColor = GitHubAccentBlue
                    )
                )

                Spacer(modifier = Modifier.height(32.dp))

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
    }
}
