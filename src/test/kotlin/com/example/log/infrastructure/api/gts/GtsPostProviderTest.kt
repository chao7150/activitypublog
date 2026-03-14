package com.example.log.infrastructure.api.gts

import com.example.log.domain.model.PlatformType
import com.example.log.infrastructure.api.common.GtsPostConverter
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class GtsPostProviderTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var gtsPostProvider: GtsPostProvider
    private val objectMapper = jacksonObjectMapper()

    @BeforeEach
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()
        
        val converter = GtsPostConverter(objectMapper)
        gtsPostProvider = GtsPostProvider(
            gtsPostConverter = converter,
            instanceUrl = mockWebServer.url("/").toString(),
            accessToken = "test-token",
            accountId = "test-account-id"
        )
    }

    @AfterEach
    fun teardown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `fetchLatestPosts should fetch and convert posts from GoToSocial`() {
        // Given
        val statusJson = """
        [
          {
            "id": "01FBVD42CQ3ZEEVMW180SBX03B",
            "created_at": "2021-07-30T09:20:25Z",
            "content": "<p>Hello World</p>",
            "url": "https://example.org/@user/statuses/01FBVD42CQ3ZEEVMW180SBX03B",
            "account": {
              "username": "user",
              "acct": "user@example.org",
              "display_name": "User",
              "url": "https://example.org/@user",
              "avatar": "https://example.org/avatar.png"
            }
          }
        ]
        """.trimIndent()

        mockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(statusJson)
        )

        // When
        val result = gtsPostProvider.fetchLatestPosts(null)

        // Then
        result shouldHaveSize 1
        val post = result[0]
        post.source.platform shouldBe PlatformType.GOTOSOCIAL
        post.source.originalId shouldBe "01FBVD42CQ3ZEEVMW180SBX03B"
        post.activityObject.content shouldBe "<p>Hello World</p>"
        post.actor.preferredUsername shouldBe "user"
        
        val recordedRequest = mockWebServer.takeRequest()
        recordedRequest.path shouldBe "/api/v1/accounts/test-account-id/statuses"
        recordedRequest.getHeader("Authorization") shouldBe "Bearer test-token"
    }

    @Test
    fun `fetchLatestPosts should use sinceId in the query if provided`() {
        // Given
        mockWebServer.enqueue(MockResponse().setResponseCode(200).setBody("[]"))

        // When
        gtsPostProvider.fetchLatestPosts("some-id")

        // Then
        val recordedRequest = mockWebServer.takeRequest()
        recordedRequest.path shouldBe "/api/v1/accounts/test-account-id/statuses?since_id=some-id"
    }
}
