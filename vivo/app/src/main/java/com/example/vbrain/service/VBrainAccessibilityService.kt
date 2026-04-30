package com.example.vbrain.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.example.vbrain.domain.use_case.ExtractAndSaveSnippetUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class VBrainAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var extractAndSaveSnippetUseCase: ExtractAndSaveSnippetUseCase

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 在这里监听用户的屏幕内容变化等被动事件
        // 由于我们使用了无障碍系统按钮主动触发，此处可留空
    }

    override fun onInterrupt() {
        Log.e("VBrainService", "Service Interrupted")
    }

    override fun onAccessibilityButtonClicked() {
        super.onAccessibilityButtonClicked()
        extractScreenContent()
    }

    private fun extractScreenContent() {
        val rootNode = rootInActiveWindow ?: return
        val stringBuilder = StringBuilder()

        traverseNode(rootNode, stringBuilder)
        rootNode.recycle() // 避免内存泄漏

        val extractedText = stringBuilder.toString().trim()
        if (extractedText.isNotEmpty()) {
            serviceScope.launch {
                try {
                    extractAndSaveSnippetUseCase(originalText = extractedText, source = "屏幕提取")
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            Toast.makeText(this, "屏幕知识已提取并存入 V-Brain", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "未发现可提取的文本", Toast.LENGTH_SHORT).show()
        }
    }

    private fun traverseNode(node: AccessibilityNodeInfo?, sb: StringBuilder) {
        if (node == null) return

        if (node.isVisibleToUser) {
            val text = node.text?.toString()
            val desc = node.contentDescription?.toString()
            if (!text.isNullOrBlank()) {
                sb.append(text).append("\n")
            } else if (!desc.isNullOrBlank()) {
                sb.append(desc).append("\n")
            }
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            traverseNode(child, sb)
            child?.recycle() // 及时回收子节点，避免内存泄漏
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel() // 关闭协程作用域
    }
}
