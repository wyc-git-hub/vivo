package com.example.vbrain.presentation.share

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.vbrain.domain.use_case.ExtractAndSaveSnippetUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ShareReceiverActivity : ComponentActivity() {

    @Inject
    lateinit var extractAndSaveSnippetUseCase: ExtractAndSaveSnippetUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 解析标准的系统分享 Intent
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                // 利用协程处理异步存储逻辑
                lifecycleScope.launch {
                    extractAndSaveSnippetUseCase(sharedText)
                    Toast.makeText(this@ShareReceiverActivity, "已存入 V-Brain 碎片库", Toast.LENGTH_SHORT).show()
                    finish() // 处理完毕后立即无感退出回原应用
                }
                return
            }
        }
        
        // 若没有有效信息也直接退出
        finish()
    }
}

