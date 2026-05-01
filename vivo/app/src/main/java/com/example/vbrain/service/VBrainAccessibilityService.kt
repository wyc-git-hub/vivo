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
        // 1. 点击瞬间立刻震动，证明程序收到了指令
        triggerVibration()

        val rootNode = rootInActiveWindow
        if (rootNode == null) {
            Toast.makeText(applicationContext, "获取失败：小红书页面未加载完或限制了读取", Toast.LENGTH_SHORT).show()
            return
        }

        val stringBuilder = StringBuilder()
        traverseNode(rootNode, stringBuilder)
        rootNode.recycle()

        val extractedText = stringBuilder.toString().trim()

        if (extractedText.isNotEmpty()) {
            Toast.makeText(applicationContext, "正在思考并存入大脑...", Toast.LENGTH_SHORT).show()

            serviceScope.launch {
                try {
                    extractAndSaveSnippetUseCase(originalText = extractedText, source = "屏幕提取")

                    withContext(Dispatchers.Main) {
                        triggerVibration() // 成功后再震动一下
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
            // 提示用户文字为什么没抓到
            Toast.makeText(applicationContext, "未发现可提取文本（注意：AI无法直接读取图片里的文字）", Toast.LENGTH_LONG).show()
        }
    }

    // 💥 增强版抓取逻辑：不再判断是否 isVisibleToUser，只要有文字统统抓走
    private fun traverseNode(node: AccessibilityNodeInfo?, sb: StringBuilder) {
        if (node == null) return

        // 【关键修改】：去掉了 if (node.isVisibleToUser) 的限制
        val text = node.text?.toString()
        val desc = node.contentDescription?.toString()

        if (!text.isNullOrBlank()) {
            sb.append(text).append("\n")
        } else if (!desc.isNullOrBlank()) {
            sb.append(desc).append("\n")
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            traverseNode(child, sb)
            child?.recycle()
        }
    }

    // 震动反馈
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