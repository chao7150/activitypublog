package com.example.log.domain.gateway

import com.example.log.domain.model.ActivityPost
import java.util.UUID

/**
 * 収集した投稿を永続化・検索するためのリポジトリ。
 */
interface PostRepository {
    fun save(post: ActivityPost): ActivityPost
    fun saveAll(posts: List<ActivityPost>): List<ActivityPost>
    fun findById(id: UUID): ActivityPost?
    fun findAllOrderByPublishedDesc(): List<ActivityPost>
    fun findByPlatform(platform: String): List<ActivityPost>
    fun findLatestOneByPlatform(platform: String): ActivityPost?
}
