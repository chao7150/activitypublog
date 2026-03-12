package com.example.log.domain.model

import java.util.UUID
import java.time.Instant

/**
 * 各プラットフォームから収集した投稿を、システム内部で統合的に管理するための Entity。
 * ActivityStreams 2.0 の概念を包含しつつ、永続化や検索に最適化されている。
 */
data class ActivityPost(
    val id: UUID = UUID.randomUUID(),  // 内部管理用の一意なID
    val actor: Actor,                  // 投稿者
    val activityObject: ActivityObject, // 投稿内容本体
    val source: PostSource,            // オリジナルのプラットフォーム情報
    val metadata: Map<String, Any> = emptyMap() // サービス固有の付加情報
) {
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
