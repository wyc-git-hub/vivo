package com.example.vbrain.service

import android.accessibilityservice.AccessibilityButtonController
import android.accessibilityservice.AccessibilityService
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import android.widget.Toast
import com.example.vbrain.domain.use_case.ExtractAndSaveSnippetUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class VBrainAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var extractAndSaveSnippetUseCase: ExtractAndSaveSnippetUseCase

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var buttonCallback: AccessibilityButtonController.AccessibilityButtonCallback? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 轻量级状态变更刷新
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED ||
            event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            try { rootInActiveWindow?.recycle() } catch (e: Exception) {}
        }
    }

    override fun onInterrupt() {}

    override fun onServiceConnected() {
        super.onServiceConnected()

        // 【核心修复 1：启动前台保活机制，防止切换应用时进程被系统冻结】
        startKeepAliveNotification()

        val controller = accessibilityButtonController
        buttonCallback = object : AccessibilityButtonController.AccessibilityButtonCallback() {
            override fun onClicked(controller: AccessibilityButtonController) {
                // 不管主线程有没有卡住，先强行调用震动反馈
                triggerVibration(50L)
                mainHandler.post {
                    Toast.makeText(applicationContext, "正在深度扫描屏幕内容...", Toast.LENGTH_SHORT).show()
                }
                extractScreenContent()
            }
        }

        buttonCallback?.let {
            controller.registerAccessibilityButtonCallback(it)
        }
    }

    private fun startKeepAliveNotification() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channelId = "vbrain_keep_alive"
                val channel = NotificationChannel(
                    channelId,
                    "VBrain 核心服务保活",
                    NotificationManager.IMPORTANCE_MIN // 极低优先级，不会有声音和弹窗打扰
                )
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.createNotificationChannel(channel)

                val notification = Notification.Builder(this, channelId)
                    .setContentTitle("VBrain 大脑在线")
                    .setContentText("提取服务运行中，防止被系统强制休眠")
                    .setSmallIcon(android.R.drawable.ic_menu_view) // 使用 Android 系统内置安全图标
                    .build()

                startForeground(9999, notification) // 开启前台防杀
            }
        } catch (e: Exception) {
            Log.e("VBrainService", "开启保活失败", e)
        }
    }

    private fun extractScreenContent() {
        serviceScope.launch {
            var extractedText = ""

            // 【核心修复 2：针对浏览器等慢加载应用，增加至 3 次渐进式重试】
            for (i in 1..3) {
                extractedText = extractAllScreenText()
                if (extractedText.isNotBlank()) {
                    break // 抓到了就立刻跳出循环
                }
                Log.d("VBrainService", "第 $i 次抓取为空，等待 ${i * 500}ms 后重试...")
                // 递增等待时间：500ms -> 1000ms -> 1500ms
                delay(i * 500L)
            }

            if (extractedText.isNotBlank()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(applicationContext, "抓取成功 (${extractedText.length} 字)，开始处理...", Toast.LENGTH_SHORT).show()
                }

                try {
                    val safeText = extractedText.take(15000)
                    extractAndSaveSnippetUseCase(
                        originalText = safeText,
                        source = "无障碍屏幕提取",
                        imagePaths = emptyList()
                    )

                    withContext(Dispatchers.Main) {
                        triggerVibration(100L)
                        Toast.makeText(applicationContext, "🎉 知识已保存到大脑", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(applicationContext, "大脑思考中断: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(applicationContext, "未提取到文字 (请尝试滑动一下网页再点击)", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun extractAllScreenText(): String {
        val sb = StringBuilder()
        try {
            var hasFoundNodes = false
            val windowsList = windows
            if (!windowsList.isNullOrEmpty()) {
                for (window in windowsList) {
                    if (window.type == AccessibilityWindowInfo.TYPE_APPLICATION || window.isActive) {
                        val root = try { window.root } catch (e: Exception) { null }
                        if (root != null) {
                            extractTextRecursively(root, sb)
                            try { root.recycle() } catch (e: Exception) {}
                            hasFoundNodes = true
                        }
                    }
                }
            }

            if (!hasFoundNodes) {
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

        // 【核心修复 3：强行刷新浏览器内核 WebView 节点】
        // 如果遇到浏览器内核或者各种复杂列表，强迫系统刷新里面的子节点
        if (node.className?.contains("WebView") == true || node.className?.contains("RecyclerView") == true) {
            try { node.refresh() } catch (e: Exception) {}
        }

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
            } catch (e: Exception) {}
        }
    }

    private fun triggerVibration(duration: Long) {
        try {
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
        } catch (e: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        buttonCallback = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        serviceScope.cancel()
    }
}