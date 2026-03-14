package com.example.log.integration

import com.example.log.domain.gateway.PostRepository
import com.example.log.domain.model.PlatformType
import com.example.log.usecase.sync.SyncAllPostsUseCase
import com.example.log.usecase.digest.GenerateDigestUseCase
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import kotlinx.coroutines.runBlocking

@SpringBootTest
class SyncIntegrationTest {

    @Autowired
    private lateinit var syncUseCase: SyncAllPostsUseCase

    @Autowired
    private lateinit var digestUseCase: GenerateDigestUseCase

    @Autowired
    private lateinit var repository: PostRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    companion object {
        private val gtsMockServer = MockWebServer()
        private val discordMockServer = MockWebServer()

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("gts.instance-url") { gtsMockServer.url("/").toString() }
            registry.add("discord.webhook-url") { discordMockServer.url("/").toString() }
        }
    }

    @BeforeEach
    fun setup() {
        jdbcTemplate.execute("TRUNCATE TABLE post_tags CASCADE")
        jdbcTemplate.execute("TRUNCATE TABLE attachments CASCADE")
        jdbcTemplate.execute("TRUNCATE TABLE posts CASCADE")
    }

    @Test
    fun testSyncAndDigest() = runBlocking {
        // 1. GTS のモックレスポンス設定
        val statusJson = """
        [
          {
            "id": "status-1",
            "created_at": "${java.time.Instant.now()}",
            "content": "<p>Integration Test Status</p>",
            "account": {
              "username": "tester",
              "acct": "tester@example.com",
              "display_name": "Tester"
            }
          }
        ]
        """.trimIndent()
        gtsMockServer.enqueue(MockResponse().setBody(statusJson).setHeader("Content-Type", "application/json"))

        // 2. 同期実行
        syncUseCase.execute()

        // DB に保存されているか確認
        val posts = repository.findAllOrderByPublishedDesc()
        posts.shouldNotBeEmpty()
        posts.any { it.source.platform == PlatformType.GOTOSOCIAL } shouldBe true

        // 3. ダイジェスト生成 & Discord 配信
        discordMockServer.enqueue(MockResponse().setResponseCode(204))
        val digest = digestUseCase.execute(24)

        digest shouldNotBe null
        
        // Discord にリクエストが飛んだか確認
        val recordedRequest = discordMockServer.takeRequest()
        recordedRequest.method shouldBe "POST"
        recordedRequest.body.readUtf8() shouldBe "{\"content\":\"[GOTOSOCIAL] Integration Test Status\"}"
    }

    @AfterEach
    fun teardown() {
        // MockServer は使い回すので shutdown しない（dynamic properties のため）
    }
}
