package com.example.vbrain.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.util.Log

class VBrainAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 监听并提取屏幕碎知识片段
        event?.let {
            if (it.eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
                // TODO: 结合端侧大模型提取有用内容
            }
        }
    }

    override fun onInterrupt() {
        Log.e("VBrainService", "Service Interrupted")
    }
}

