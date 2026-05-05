package com.example.vbrain.presentation.share

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.vbrain.domain.use_case.ExtractAndSaveSnippetUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.regex.Pattern
import javax.inject.Inject

@AndroidEntryPoint
class ShareReceiverActivity : ComponentActivity() {

    @Inject
    lateinit var extractAndSaveSnippetUseCase: ExtractAndSaveSnippetUseCase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch(Dispatchers.IO) {
            handleIntent(intent)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@ShareReceiverActivity, "已存入 V-Brain 碎片库", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private suspend fun handleIntent(intent: Intent) {
        val action = intent.action
        val type = intent.type

        if ((action == Intent.ACTION_SEND || action == Intent.ACTION_SEND_MULTIPLE) && type != null) {
            var sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: ""
            var sourceUrl: String? = null
            val imagePaths = mutableListOf<String>()

            // Try to extract URL from text
            val urlPattern = Pattern.compile("(https?://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|])")
            if (sharedText.isNotBlank()) {
                val matcher = urlPattern.matcher(sharedText)
                if (matcher.find()) {
                    sourceUrl = matcher.group()
                }
            }

            // Handle images
            if (type.startsWith("image/")) {
                if (action == Intent.ACTION_SEND) {
                    val imageUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                    imageUri?.let {
                        val path = saveImageToInternalStorage(it)
                        if (path != null) imagePaths.add(path)
                    }
                } else if (action == Intent.ACTION_SEND_MULTIPLE) {
                    val imageUris = intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                    imageUris?.forEach { uri ->
                        val path = saveImageToInternalStorage(uri)
                        if (path != null) imagePaths.add(path)
                    }
                }
            }

            if (sharedText.isBlank() && imagePaths.isEmpty()) {
                return
            }

            if (sharedText.isBlank()) {
                sharedText = "系统分享图片"
            }

            extractAndSaveSnippetUseCase(
                originalText = sharedText,
                source = "系统分享",
                imagePaths = imagePaths,
                sourceUrl = sourceUrl
            )
        }
    }

    private fun saveImageToInternalStorage(uri: Uri): String? {
        return try {
            val contentResolver = applicationContext.contentResolver
            val imagesDir = File(applicationContext.filesDir, "images")
            if (!imagesDir.exists()) {
                imagesDir.mkdirs()
            }

            val fileName = "${UUID.randomUUID()}.jpg"
            val file = File(imagesDir, fileName)

            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(file).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            Log.e("ShareReceiverActivity", "Error saving image", e)
            null
        }
    }
}
