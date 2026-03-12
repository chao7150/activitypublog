package com.example.log.domain.service

import com.example.log.domain.model.ActivityPost

/**
 * 複数の投稿を一つのダイジェスト文にまとめるロジックを定義する。
 */
interface Summarizer {
    /**
     * 指定された投稿リストから、ダイジェスト用のテキストを生成する。
     */
    fun summarize(posts: List<ActivityPost>): String
}

/**
 * 投稿を単純に連結し、最大文字数で切り取るシンプルな実装。
 */
class SimpleSummarizer(private val maxLength: Int = 500) : Summarizer {
    override fun summarize(posts: List<ActivityPost>): String {
        if (posts.isEmpty()) return "本日の投稿はありませんでした。"

        val joinedText = posts.sortedBy { it.publishedAt }
            .joinToString("\n\n") { post ->
                val platform = post.platformName.uppercase()
                val content = post.activityObject.content.take(100) // 各投稿から100文字抽出
                "[$platform] $content"
            }

        return if (joinedText.length > maxLength) {
            joinedText.take(maxLength - 3) + "..."
        } else {
            joinedText
        }
    }
}
