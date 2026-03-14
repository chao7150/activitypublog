package com.example.log.infrastructure.persistence.jpa.repository

import com.example.log.domain.model.*
import com.example.log.domain.gateway.PostRepository
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import java.time.Instant
import java.util.UUID

@SpringBootTest
class PostRepositoryIntegrationTest {

    @Autowired
    private lateinit var repository: PostRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @BeforeEach
    fun setup() {
        // 全データをクリア
        jdbcTemplate.execute("TRUNCATE TABLE attachments CASCADE")
        jdbcTemplate.execute("TRUNCATE TABLE posts CASCADE")
        
        // PGroonga のインデックスを作成（存在しない場合）
        jdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS pgroonga")
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS pgroonga_content_index ON posts USING pgroonga (content)")
    }

    @Test
    @DisplayName("日本語全文検索で目的の投稿を正確に取得できるべき")
    fun shouldSearchJapaneseContent() {
        // Given: 日本語の投稿を保存
        val posts = listOf(
            createPost("すもももももももものうち"),
            createPost("今日は天気がいいですね"),
            createPost("昨日は雨だったので家でゲームをしていました")
        )
        repository.saveAll(posts)

        // When: "もも" で検索
        val result1 = repository.search("もも")
        result1 shouldHaveSize 1
        result1[0].activityObject.content shouldBe "すもももももももものうち"

        // When: "天気" で検索
        val result2 = repository.search("天気")
        result2 shouldHaveSize 1
        result2[0].activityObject.content shouldBe "今日は天気がいいですね"

        // When: "ゲーム" で検索
        val result3 = repository.search("ゲーム")
        result3 shouldHaveSize 1
        result3[0].activityObject.content shouldBe "昨日は雨だったので家でゲームをしていました"
    }

    private fun createPost(content: String): ActivityPost {
        return ActivityPost(
            id = UUID.randomUUID(),
            actor = Actor("name", "user", "host", null, null),
            activityObject = ActivityObject.Note(
                content = content,
                publishedAt = Instant.now(),
                url = null
            ),
            source = PostSource(
                platform = PlatformType.GOTOSOCIAL,
                originalId = UUID.randomUUID().toString()
            )
        )
    }
}
