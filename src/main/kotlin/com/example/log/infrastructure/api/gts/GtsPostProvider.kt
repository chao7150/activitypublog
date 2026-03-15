package com.example.log.infrastructure.api.gts

import com.example.log.domain.gateway.PostProvider
import com.example.log.domain.model.ActivityPost
import com.example.log.domain.model.PlatformType
import com.example.log.infrastructure.api.common.GtsPostConverter
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class GtsPostProvider(
    private val gtsPostConverter: GtsPostConverter,
    @Value("\${gts.instance-url}") private val instanceUrl: String,
    @Value("\${gts.access-token}") private val accessToken: String,
    @Value("\${gts.account-id}") private val accountId: String
) : PostProvider {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val restClient = RestClient.builder()
        .baseUrl(instanceUrl)
        .defaultHeader("Authorization", "Bearer $accessToken")
        .build()

    override fun fetchLatestPosts(sinceId: String?): List<ActivityPost> {
        val uri = "/api/v1/accounts/$accountId/statuses" + (sinceId?.let { "?min_id=$it" } ?: "")
        
        logger.debug("GoToSocial から投稿を取得中: $uri")
        
        return try {
            val response = restClient.get()
                .uri(uri)
                .retrieve()
                .body(object : ParameterizedTypeReference<List<Map<String, Any>>>() {}) ?: emptyList()
            
            response.map { gtsPostConverter.convertToActivityPost(it) }
        } catch (e: Exception) {
            logger.error("GoToSocial からの投稿取得に失敗しました: ${e.message}", e)
            emptyList()
        }
    }

    override fun getSupportedPlatform(): PlatformType = PlatformType.GOTOSOCIAL
}
