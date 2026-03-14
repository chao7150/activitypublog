package com.example.log.infrastructure.api.discord

import com.example.log.domain.gateway.PostBroadcaster
import com.example.log.domain.model.ActivityPost
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class DiscordPostBroadcaster(
    @Value("\${discord.webhook-url}") private val webhookUrl: String
) : PostBroadcaster {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val restClient = RestClient.builder().baseUrl(webhookUrl).build()

    override fun broadcast(post: ActivityPost) {
        logger.info("Discord へ投稿を配信中 (ID: ${post.id})")

        val content = post.activityObject.content
        val payload = mapOf("content" to content)

        try {
            restClient.post()
                .body(payload)
                .retrieve()
                .toBodilessEntity()
            logger.info("Discord への配信が完了しました。")
        } catch (e: Exception) {
            logger.error("Discord への配信に失敗しました: \${e.message}", e)
        }
    }
}
