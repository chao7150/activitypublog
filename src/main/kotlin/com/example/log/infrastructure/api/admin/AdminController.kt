package com.example.log.infrastructure.api.admin

import com.example.log.domain.gateway.PostRepository
import com.example.log.usecase.digest.GenerateDigestUseCase
import com.example.log.usecase.sync.SyncAllPostsUseCase
import kotlinx.coroutines.runBlocking
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin")
class AdminController(
    private val syncAllPostsUseCase: SyncAllPostsUseCase,
    private val generateDigestUseCase: GenerateDigestUseCase,
    private val postRepository: PostRepository
) {

    /**
     * すべての外部ソースから投稿を同期する
     */
    @PostMapping("/sync")
    fun sync(): Map<String, Any> {
        return runBlocking {
            syncAllPostsUseCase.execute()
            mapOf("status" to "success", "message" to "Synchronization triggered")
        }
    }

    /**
     * ダイジェストを生成し、各プラットフォーム（Discord等）へ配信する
     */
    @PostMapping("/digest")
    fun generateDigest(@RequestParam(defaultValue = "24") hours: Long): Map<String, Any> {
        val digest = generateDigestUseCase.execute(hours)
        return if (digest != null) {
            mapOf(
                "status" to "success",
                "digestId" to digest.id,
                "content" to digest.activityObject.content
            )
        } else {
            mapOf("status" to "skipped", "message" to "No posts found in the last $hours hours")
        }
    }

    /**
     * 現在保存されている最新の投稿を10件取得する（動作確認用）
     */
    @GetMapping("/posts")
    fun getLatestPosts(): List<Map<String, Any>> {
        return postRepository.findAllOrderByPublishedDesc().take(10).map {
            mapOf(
                "id" to it.id,
                "platform" to it.source.platform,
                "actor" to it.actor.identifier(),
                "publishedAt" to it.publishedAt,
                "content" to it.activityObject.content.take(100)
            )
        }
    }
}
