package com.example.log.usecase.sync

import com.example.log.domain.gateway.PostRepository
import com.example.log.domain.gateway.PostProvider
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * すべての外部ソースから投稿を取得し、データベースを更新するユースケース。
 */
@Service
class SyncAllPostsUseCase(
    private val providers: List<PostProvider>,
    private val repository: PostRepository
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 同期処理を実行する。
     * 各プラットフォームごとに並列（Coroutine）でフェッチを行う。
     */
    suspend fun execute() = coroutineScope {
        logger.info("すべてのソースからの同期を開始します...")

        // 各プロバイダーを並列実行
        val syncJobs = providers.map { provider ->
            async(Dispatchers.IO) {
                syncPlatform(provider)
            }
        }

        // すべての終了を待機
        syncJobs.awaitAll()
        logger.info("同期処理が完了しました。")
    }

    private fun syncPlatform(provider: PostProvider) {
        val platform = provider.getSupportedPlatform()
        
        try {
            // DBに保存されている最新の投稿を取得し、どこから取得を再開するかを決定
            val latestStoredPost = repository.findLatestOneByPlatform(platform.name)
            val sinceId = latestStoredPost?.source?.originalId
            
            logger.debug("プラットフォーム $platform の同期を開始 (sinceId: $sinceId)")
            
            val newPosts = provider.fetchLatestPosts(sinceId)
            
            if (newPosts.isNotEmpty()) {
                repository.saveAll(newPosts)
                logger.info("プラットフォーム $platform から ${newPosts.size} 件の新しい投稿を保存しました。")
            } else {
                logger.debug("プラットフォーム $platform に新しい投稿はありませんでした。")
            }
        } catch (e: Exception) {
            logger.error("プラットフォーム $platform の同期中にエラーが発生しました: ${e.message}", e)
        }
    }
}
