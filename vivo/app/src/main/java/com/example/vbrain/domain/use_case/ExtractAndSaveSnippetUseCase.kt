package com.example.vbrain.domain.use_case

import com.example.vbrain.data.local.entity.KnowledgeSnippet
import com.example.vbrain.domain.repository.KnowledgeRepository
import javax.inject.Inject

class ExtractAndSaveSnippetUseCase @Inject constructor(
    private val repository: KnowledgeRepository
) {
    suspend operator fun invoke(originalText: String, source: String = "系统分享") {
        // 在这里先构建一个未经大模型处理的基础实体，待后续后台或ViewModel统一拿去提纯
        val snippet = KnowledgeSnippet(
            originalText = originalText,
            summary = "", // 留空，作为标记
            tags = emptyList(), // 留空
            source = source
        )
        // 保存进数据库
        repository.addSnippet(snippet)
    }
}

