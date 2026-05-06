package com.example.vbrain.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class VBrainAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var extractAndSaveSnippetUseCase: ExtractAndSaveSnippetUseCase

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {
        Log.e("VBrainService", "Service Interrupted")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        val controller = accessibilityButtonController
        controller.registerAccessibilityButtonCallback(object : android.accessibilityservice.AccessibilityButtonController.AccessibilityButtonCallback() {
            override fun onClicked(controller: android.accessibilityservice.AccessibilityButtonController) {
                extractScreenContent()
            }
        })
    }

    private fun extractScreenContent() {
        triggerVibration()

        serviceScope.launch {
            withContext(Dispatchers.Main) {
                Toast.makeText(applicationContext, "正在全屏扫描文字...", Toast.LENGTH_SHORT).show()
            }

            val extractedText = extractAllScreenText()
            Log.d("VBrainService", "抓取出文本长度: ${extractedText.length}")

            if (extractedText.isNotBlank()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(applicationContext, "抓取到 ${extractedText.length} 字，交由大脑思考...", Toast.LENGTH_SHORT).show()
                }

                try {
                    // 防止文本过长引发 OOM 或大模型 Token 超限，进行适当截断
                    val safeText = extractedText.take(15000)
                    
                    extractAndSaveSnippetUseCase(
                        originalText = safeText, 
                        source = "无障碍屏幕提取",
                        imagePaths = emptyList()
                    )

                    withContext(Dispatchers.Main) {
                        triggerVibration()
                        Toast.makeText(applicationContext, "🎉 保存与排版成功", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(applicationContext, "大脑思考中断: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(applicationContext, "未能提取到屏幕文字 (页面可能受限)", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun extractAllScreenText(): String {
        val sb = StringBuilder()
        
        try {
            // 1. 优先尝试遍历所有活动视窗 (覆盖悬浮层/底部抽屉等)
            val windowsList = windows
            if (!windowsList.isNullOrEmpty()) {
                for (window in windowsList) {
                    val root = try { window.root } catch (e: Exception) { null }
                    if (root != null) {
                        extractTextRecursively(root, sb)
                        try { root.recycle() } catch (e: Exception) {}
                    }
                }
            } else {
                // 2. 兜底方案：只抓取当前激活视窗
                val root = try { rootInActiveWindow } catch (e: Exception) { null }
                if (root != null) {
                    extractTextRecursively(root, sb)
                    try { root.recycle() } catch (e: Exception) {}
                }
            }
        } catch (e: Exception) {
            Log.e("VBrainService", "提取节点树时发生异常", e)
        }
        
        return sb.toString().trim()
    }

    private fun extractTextRecursively(node: AccessibilityNodeInfo?, sb: StringBuilder) {
        if (node == null) return

        val text = node.text?.toString()
        val desc = node.contentDescription?.toString()

        if (!text.isNullOrBlank()) {
            sb.append(text.trim()).append("\n")
        } else if (!desc.isNullOrBlank()) {
            sb.append(desc.trim()).append("\n")
        }

        for (i in 0 until node.childCount) {
            try {
                val child = node.getChild(i)
                if (child != null) {
                    extractTextRecursively(child, sb)
                    try { child.recycle() } catch (e: Exception) {}
                }
            } catch (e: Exception) {
                // 忽略单个节点发生的读取崩溃
            }
        }
    }

    private fun triggerVibration() {
        try {
            val duration = 100L
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = applicationContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = applicationContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(duration)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}