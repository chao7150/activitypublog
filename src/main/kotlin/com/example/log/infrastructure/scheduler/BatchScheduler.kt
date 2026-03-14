package com.example.log.infrastructure.scheduler

import com.example.log.usecase.digest.GenerateDigestUseCase
import com.example.log.usecase.sync.SyncAllPostsUseCase
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class BatchScheduler(
    private val syncAllPostsUseCase: SyncAllPostsUseCase,
    private val generateDigestUseCase: GenerateDigestUseCase
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * 1時間おきにすべてのソースから投稿を同期する。
     * initialDelay を設定して、アプリ起動直後にも一度実行する。
     */
    @Scheduled(fixedRate = 3600000, initialDelay = 60000)
    fun syncPosts() {
        logger.info("定期投稿同期を開始します...")
        runBlocking {
            try {
                syncAllPostsUseCase.execute()
                logger.info("定期投稿同期が完了しました。")
            } catch (e: Exception) {
                logger.error("定期投稿同期中にエラーが発生しました: ${e.message}", e)
            }
        }
    }

    /**
     * 毎朝8時（JST）にダイジェストを生成し、Discord 等へ配信する。
     * cron 形式: 秒 分 時 日 月 曜日
     */
    @Scheduled(cron = "0 0 8 * * *", zone = "Asia/Tokyo")
    fun dailyDigest() {
        logger.info("日次ダイジェスト生成を開始します...")
        try {
            val digest = generateDigestUseCase.execute(24)
            if (digest != null) {
                logger.info("日次ダイジェストの生成と配信が完了しました。 (Digest ID: ${digest.id})")
            } else {
                logger.info("過去24時間に投稿がなかったため、ダイジェスト生成をスキップしました。")
            }
        } catch (e: Exception) {
            logger.error("日次ダイジェスト生成中にエラーが発生しました: ${e.message}", e)
        }
    }
}
