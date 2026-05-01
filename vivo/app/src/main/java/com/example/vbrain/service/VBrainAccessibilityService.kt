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
        // 🌟 1. 点击的瞬间立刻强制震动（给出即时反馈）
        triggerVibration()

        val rootNode = rootInActiveWindow

        if (rootNode == null) {
            Toast.makeText(applicationContext, "无法读取屏幕，可能是应用限制或页面未加载完", Toast.LENGTH_SHORT).show()
            return
        }

        val stringBuilder = StringBuilder()
        traverseNode(rootNode, stringBuilder)
        rootNode.recycle()

        val extractedText = stringBuilder.toString().trim()

        if (extractedText.isNotEmpty()) {
            // 🌟 2. 在协程(网络请求)开始前，弹出“正在处理”的提示
            Toast.makeText(applicationContext, "正在思考并存入大脑...", Toast.LENGTH_SHORT).show()

            serviceScope.launch {
                try {
                    // 请求千问大模型，会耗时 1-3 秒
                    extractAndSaveSnippetUseCase(originalText = extractedText, source = "屏幕提取")

                    withContext(Dispatchers.Main) {
                        // 🌟 3. 成功后再震动一下，并提示保存成功
                        triggerVibration()
                        Toast.makeText(applicationContext, "🎉 保存成功", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(applicationContext, "保存时发生网络异常", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            Toast.makeText(applicationContext, "当前屏幕未发现可提取的纯文本", Toast.LENGTH_SHORT).show()
        }
    }

    // 💥 强化版的震动方法 (已确保放在类的内部，且使用了 applicationContext 安全调用)
    private fun triggerVibration() {
        try {
            val duration = 150L
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
            child?.recycle()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}