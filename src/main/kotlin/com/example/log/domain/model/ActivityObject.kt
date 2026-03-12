package com.example.log.domain.model

import java.time.Instant

/**
 * ActivityStreams 2.0 の Object を表現する。
 * 投稿の種類（Note, Article等）によって異なるプロパティを持つ。
 */
sealed class ActivityObject {
    abstract val content: String
    abstract val publishedAt: Instant
    abstract val url: String?

    /**
     * 短文投稿 (SNS, Discord 等)
     */
    data class Note(
        override val content: String,
        override val publishedAt: Instant,
        override val url: String?,
        val summary: String? = null, // CW (Content Warning) の理由など
        val attachments: List<Attachment> = emptyList()
    ) : ActivityObject()

    /**
     * 長文記事 (WordPress 等)
     */
    data class Article(
        val title: String,
        override val content: String,
        override val publishedAt: Instant,
        override val url: String?,
        val tags: List<String> = emptyList()
    ) : ActivityObject()
}

/**
 * 添付ファイル (画像, 動画等)
 */
data class Attachment(
    val type: AttachmentType,
    val url: String,
    val blurhash: String? = null,
    val description: String? = null // 代替テキスト
)

enum class AttachmentType { IMAGE, VIDEO, AUDIO, DOCUMENT }
