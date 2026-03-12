package com.example.log.infrastructure.api.common

import com.example.log.domain.model.ActivityObject
import com.example.log.domain.model.PlatformType
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.Instant

class GtsPostConverterTest {

    private val objectMapper: ObjectMapper = jacksonObjectMapper()
    private val converter = GtsPostConverter(objectMapper)

    @Test
    @DisplayName("GoToSocialの実際のレスポンスサンプルをActivityPostに変換できるべき")
    fun shouldConvertRealGtsSampleToActivityPost() {
        // Given: misc/gts-status.json の内容を模したデータ
        val json = """
        {
          "id": "01FBVD42CQ3ZEEVMW180SBX03B",
          "created_at": "2021-07-30T09:20:25+00:00",
          "content": "<p>Hey this is a status!</p>",
          "url": "https://example.org/@some_user/statuses/01FBVD42CQ3ZEEVMW180SBX03B",
          "account": {
            "username": "some_user",
            "acct": "some_user@example.org",
            "display_name": "big jeff (he/him)",
            "url": "https://example.org/@some_user",
            "avatar": "https://example.org/media/some_user/avatar/original/avatar.jpeg"
          },
          "media_attachments": [
            {
              "id": "01FC31DZT1AYWDZ8XTCRWRBYRK",
              "type": "image",
              "url": "https://example.org/fileserver/original/attachment.jpeg",
              "description": "This is a picture of a kitten."
            }
          ]
        }
        """.trimIndent()
        val gtsStatus: Map<String, Any> = objectMapper.readValue(json)

        // When: 変換
        val post = converter.convertToActivityPost(gtsStatus)

        // Then: ドメインモデルの検証
        post.source.platform shouldBe PlatformType.GOTOSOCIAL
        post.source.originalId shouldBe "01FBVD42CQ3ZEEVMW180SBX03B"
        
        post.actor.preferredUsername shouldBe "some_user"
        post.actor.host shouldBe "example.org"
        post.actor.name shouldBe "big jeff (he/him)"
        post.actor.avatarUrl shouldBe "https://example.org/media/some_user/avatar/original/avatar.jpeg"

        val note = post.activityObject
        note.shouldBeInstanceOf<ActivityObject.Note>()
        note.content shouldBe "<p>Hey this is a status!</p>"
        note.publishedAt shouldBe Instant.parse("2021-07-30T09:20:25Z")
        note.url shouldBe "https://example.org/@some_user/statuses/01FBVD42CQ3ZEEVMW180SBX03B"
        
        note.attachments.size shouldBe 1
        note.attachments[0].url shouldBe "https://example.org/fileserver/original/attachment.jpeg"
        note.attachments[0].description shouldBe "This is a picture of a kitten."
    }

    @Test
    @DisplayName("Reblog(Boost)の場合、オリジナルの投稿内容が抽出されるべき")
    fun shouldExtractOriginalContentWhenReblog() {
        // Given: Reblog を含むレスポンス
        val json = """
        {
          "id": "BOOST_ID",
          "reblog": {
            "id": "ORIGINAL_ID",
            "content": "<p>Original Content</p>",
            "created_at": "2021-07-30T09:20:25+00:00",
            "account": {
              "username": "original_user",
              "acct": "original_user@remote.com",
              "display_name": "Original User"
            }
          }
        }
        """.trimIndent()
        val gtsStatus: Map<String, Any> = objectMapper.readValue(json)

        // When: 変換
        val post = converter.convertToActivityPost(gtsStatus)

        // Then: オリジナルの投稿者が Actor となり、オリジナルの内容が Note となるべき
        // (あるいは Boost であることを示すフラグが必要か？一旦オリジナルを優先する仕様と仮定)
        post.source.originalId shouldBe "ORIGINAL_ID"
        post.actor.preferredUsername shouldBe "original_user"
        post.activityObject.content shouldBe "<p>Original Content</p>"
    }

    @Test
    @DisplayName("CW(spoiler_text)がある場合、summaryにマッピングされるべき")
    fun shouldMapSpoilerTextToSummary() {
        // Given: spoiler_text を含むレスポンス
        val json = """
        {
          "id": "CW_ID",
          "content": "<p>Sensitive content</p>",
          "spoiler_text": "warning nsfw",
          "created_at": "2021-07-30T09:20:25+00:00",
          "account": { "username": "user", "acct": "user", "display_name": "User" }
        }
        """.trimIndent()
        val gtsStatus: Map<String, Any> = objectMapper.readValue(json)

        // When: 変換
        val post = converter.convertToActivityPost(gtsStatus)

        // Then: summary にマッピングされているか
        val note = post.activityObject as ActivityObject.Note
        note.summary shouldBe "warning nsfw"
        note.content shouldBe "<p>Sensitive content</p>"
    }
}
