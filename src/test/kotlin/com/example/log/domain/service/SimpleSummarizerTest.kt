package com.example.log.domain.service

import com.example.log.domain.model.*
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldEndWith
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Instant

class SimpleSummarizerTest {

    private val summarizer = SimpleSummarizer(maxLength = 100)

    @Test
    @DisplayName("投稿が空のとき、デフォルトのメッセージを返すべき")
    fun shouldReturnDefaultMessageWhenPostsIsEmpty() {
        val result = summarizer.summarize(emptyList())
        result shouldBe "本日の投稿はありませんでした。"
    }

    @Test
    @DisplayName("複数の投稿があるとき、プラットフォーム名と内容を含めて連結されるべき")
    fun shouldJoinPostsWithPlatformNameAndContent() {
        val posts = listOf(
            createDummyPost("GTS", "Hello from GTS"),
            createDummyPost("Discord", "Hello from Discord")
        )

        val result = summarizer.summarize(posts)

        result shouldContain "[GOTOSOCIAL] Hello from GTS"
        result shouldContain "[DISCORD] Hello from Discord"
    }

    @Test
    @DisplayName("生成されたテキストが最大長を超えるとき、切り詰められて三点リーダーが付与されるべき")
    fun shouldTruncateWhenResultExceedsMaxLength() {
        val longContent = "A".repeat(200)
        val posts = listOf(createDummyPost("GTS", longContent))

        val result = summarizer.summarize(posts)

        result.length shouldBe 100
        result shouldEndWith "..."
    }

    @Test
    @DisplayName("HTMLタグが含まれるとき、タグが除去されて要約されるべき")
    fun shouldStripHtmlTags() {
        val posts = listOf(createDummyPost("GTS", "<p>Hello <b>World</b></p>"))

        val result = summarizer.summarize(posts)

        result shouldContain "[GOTOSOCIAL] Hello World"
    }

    private fun createDummyPost(platformName: String, content: String): ActivityPost {
        return ActivityPost(
            actor = Actor("name", "user", "host", null, null),
            activityObject = ActivityObject.Note(
                content = content,
                publishedAt = Instant.now(),
                url = null
            ),
            source = PostSource(
                platform = when(platformName) {
                    "GTS" -> PlatformType.GOTOSOCIAL
                    "Discord" -> PlatformType.DISCORD
                    else -> PlatformType.INTERNAL
                },
                originalId = "id"
            )
        )
    }
}
