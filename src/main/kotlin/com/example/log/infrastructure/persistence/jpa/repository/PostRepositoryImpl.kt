package com.example.log.infrastructure.persistence.jpa.repository

import com.example.log.domain.gateway.PostRepository
import com.example.log.domain.model.*
import com.example.log.infrastructure.persistence.jpa.entity.AttachmentJpaEntity
import com.example.log.infrastructure.persistence.jpa.entity.PostJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository
import java.util.*

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

@Repository
class PostRepositoryImpl(
    private val jpaPostRepository: JpaPostRepository
) : PostRepository {

    override fun save(post: ActivityPost): ActivityPost {
        // 重複チェック: 同じプラットフォーム・同じOriginalIDの投稿が既に存在するか確認
        val existing = jpaPostRepository.findByPlatformAndOriginalId(post.source.platform, post.source.originalId)
        
        // 既存がある場合はその ID を使い、なければ新規の ID を使う
        val entity = toEntity(post, existing?.id ?: post.id)
        val saved = jpaPostRepository.save(entity)
        return toDomain(saved)
    }

    override fun saveAll(posts: List<ActivityPost>): List<ActivityPost> {
        // 各投稿ごとに重複チェックを行うため save を呼び出す
        return posts.map { save(it) }
    }

    override fun findById(id: UUID): ActivityPost? {
        return jpaPostRepository.findByIdOrNull(id)?.let { toDomain(it) }
    }

    override fun findAllOrderByPublishedDesc(): List<ActivityPost> {
        return jpaPostRepository.findAllByOrderByPublishedAtDesc().map { toDomain(it) }
    }

    override fun findLatestOneByPlatform(platform: String): ActivityPost? {
        val platformType = PlatformType.valueOf(platform.uppercase())
        return jpaPostRepository.findTopByPlatformOrderByPublishedAtDesc(platformType)?.let { toDomain(it) }
    }

    override fun search(keyword: String): List<ActivityPost> {
        return jpaPostRepository.searchByPgroonga(keyword).map { toDomain(it) }
    }

    // TODO: 後ほど実装
    override fun findByPlatform(platform: String): List<ActivityPost> = emptyList()

    private fun toEntity(post: ActivityPost, forcedId: UUID? = null): PostJpaEntity {
        val obj = post.activityObject
        return PostJpaEntity(
            id = forcedId ?: post.id,
            actorIdentifier = post.actor.identifier(),
            actorName = post.actor.name,
            content = obj.content,
            publishedAt = obj.publishedAt,
            url = obj.url,
            type = when (obj) {
                is ActivityObject.Note -> "Note"
                is ActivityObject.Article -> "Article"
            },
            platform = post.source.platform,
            originalId = post.source.originalId,
            fetchedAt = post.source.fetchedAt,
            summary = if (obj is ActivityObject.Note) obj.summary else null,
            title = if (obj is ActivityObject.Article) obj.title else null,
            tags = if (obj is ActivityObject.Article) obj.tags else emptyList(),
            attachments = if (obj is ActivityObject.Note) {
                obj.attachments.map { 
                    AttachmentJpaEntity(type = it.type.name, url = it.url, description = it.description) 
                }
            } else emptyList()
        )
    }

    private fun toDomain(entity: PostJpaEntity): ActivityPost {
        // Actor の復元
        val actor = Actor(
            name = entity.actorName,
            preferredUsername = entity.actorIdentifier.substringBefore("@"),
            host = entity.actorIdentifier.substringAfter("@"),
            profileUrl = null,
            avatarUrl = null
        )

        val activityObject = when (entity.type) {
            "Article" -> ActivityObject.Article(
                title = entity.title ?: "",
                content = entity.content,
                publishedAt = entity.publishedAt,
                url = entity.url,
                tags = entity.tags
            )
            else -> ActivityObject.Note(
                content = entity.content,
                publishedAt = entity.publishedAt,
                url = entity.url,
                summary = entity.summary,
                attachments = entity.attachments.map {
                    Attachment(
                        type = AttachmentType.valueOf(it.type),
                        url = it.url,
                        description = it.description
                    )
                }
            )
        }

        return ActivityPost(
            id = entity.id,
            actor = actor,
            activityObject = activityObject,
            source = PostSource(
                platform = entity.platform,
                originalId = entity.originalId,
                fetchedAt = entity.fetchedAt
            )
        )
    }
}
