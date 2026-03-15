package com.example.log.usecase.digest

import com.example.log.domain.gateway.PostRepository
import com.example.log.domain.gateway.PostBroadcaster
import com.example.log.domain.model.*
import com.example.log.domain.service.Summarizer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * 指定された期間の投稿を集約し、ダイジェスト投稿を生成・保存するユースケース。
 */
@Service
class GenerateDigestUseCase(
    private val repository: PostRepository,
    private val summarizer: Summarizer,
    private val broadcasters: List<PostBroadcaster>
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * ダイジェストを生成して保存し、各プラットフォームへ配信する。
     * @param hours 過去何時間分の投稿を対象にするか（デフォルト24時間）
     */
    fun execute(hours: Long = 24): ActivityPost? {
        val since = Instant.now().minus(hours, ChronoUnit.HOURS)
        
        // 1. 対象となる投稿をDBから取得
        val targetPosts = repository.findAllOrderByPublishedDesc()
            .filter { it.publishedAt.isAfter(since) }
            .filter { it.source.platform != PlatformType.INTERNAL } // ダイジェスト自身は除外

        if (targetPosts.isEmpty()) {
            logger.info("対象期間内に投稿がないため、ダイジェスト生成をスキップします。")
            return null
        }

        // 2. 要約文の生成
        val digestText = summarizer.summarize(targetPosts)

        // 3. ダイジェスト投稿（ActivityPost）の作成
        val digestPost = createDigestPost(digestText)

        // 4. 保存
        val savedPost = repository.save(digestPost).also {
            logger.info("ダイジェスト投稿を保存しました (ID: ${it.id})")
        }

        // 5. 各プラットフォームへ配信
        broadcasters.forEach { broadcaster ->
            try {
                broadcaster.broadcast(savedPost)
            } catch (e: Exception) {
                logger.error("配信中にエラーが発生しました: ${e.message}")
            }
        }

        return savedPost
    }

    private fun createDigestPost(text: String): ActivityPost {
        val now = Instant.now()
        val actor = Actor(
            name = "System Digest",
            preferredUsername = "system",
            host = "internal",
            profileUrl = null,
            avatarUrl = null
        )
        val activityObject = ActivityObject.Note(
            content = text,
            publishedAt = now,
            url = null // 内部生成のため、最初はURLなし
        )
        val source = PostSource(
            platform = PlatformType.INTERNAL,
            originalId = "digest-${now.toEpochMilli()}"
        )
        return ActivityPost.create(actor, activityObject, source)
    }
}
