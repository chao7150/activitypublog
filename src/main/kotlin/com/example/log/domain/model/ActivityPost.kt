package com.example.log.domain.model

import java.util.UUID
import java.time.Instant

/**
 * 各プラットフォームから収集した投稿を、システム内部で統合的に管理するための Entity。
 * ActivityStreams 2.0 の概念を包含しつつ、永続化や検索に最適化されている。
 */
data class ActivityPost(
    val id: UUID,
    val actor: Actor,
    val activityObject: ActivityObject,
    val source: PostSource,
    val metadata: Map<String, Any> = emptyMap()
) {
    companion object {
        fun create(
            actor: Actor,
            activityObject: ActivityObject,
            source: PostSource,
            metadata: Map<String, Any> = emptyMap()
        ): ActivityPost {
            // platform と originalId から決定論的な UUID を生成
            val seed = "${source.platform.name}:${source.originalId}"
            val id = UUID.nameUUIDFromBytes(seed.toByteArray())
            return ActivityPost(id, actor, activityObject, source, metadata)
        }
    }

    // 便利なショートカット
    val publishedAt: Instant get() = activityObject.publishedAt
    val platformName: String get() = source.platform.name.lowercase()
}

/**
 * オリジナルの投稿元を追跡するための情報
 */
data class PostSource(
    val platform: PlatformType,
    val originalId: String,   // 元のシステム（Discord等）でのID
    val fetchedAt: Instant = Instant.now()
)

enum class PlatformType {
    GOTOSOCIAL,
    DISCORD,
    WORDPRESS,
    INTERNAL // システム自身が生成したサマリなど
}
