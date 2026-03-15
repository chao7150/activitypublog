package com.example.log.infrastructure.api.common

import com.example.log.domain.model.*
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@Component
class GtsPostConverter(private val objectMapper: ObjectMapper) {

    fun convertToActivityPost(status: Map<String, Any>): ActivityPost {
        val targetStatus = (status["reblog"] as? Map<String, Any>) ?: status
        val account = targetStatus["account"] as Map<String, Any>
        val username = account["username"] as String
        val acct = account["acct"] as String
        val host = if (acct.contains("@")) acct.substringAfter("@") else "localhost"
        
        val actor = Actor(
            name = account["display_name"] as? String ?: username,
            preferredUsername = username,
            host = host,
            profileUrl = account["url"] as? String,
            avatarUrl = account["avatar"] as? String
        )

        val publishedAt = parseInstant(targetStatus["created_at"] as String)
        
        val mediaAttachments = (targetStatus["media_attachments"] as? List<Map<String, Any>>) ?: emptyList()
        val attachments = mediaAttachments.map {
            Attachment(
                type = when(it["type"] as String) {
                    "image" -> AttachmentType.IMAGE
                    "video" -> AttachmentType.VIDEO
                    "audio" -> AttachmentType.AUDIO
                    else -> AttachmentType.DOCUMENT
                },
                url = it["url"] as String,
                description = it["description"] as? String
            )
        }

        val activityObject = ActivityObject.Note(
            content = targetStatus["content"] as String,
            publishedAt = publishedAt,
            url = targetStatus["url"] as? String,
            summary = targetStatus["spoiler_text"] as? String,
            attachments = attachments
        )

        return ActivityPost.create(
            actor = actor,
            activityObject = activityObject,
            source = PostSource(
                platform = PlatformType.GOTOSOCIAL,
                originalId = targetStatus["id"] as String
            )
        )
    }

    private fun parseInstant(dateStr: String): Instant {
        return try {
            Instant.parse(dateStr)
        } catch (e: Exception) {
            OffsetDateTime.parse(dateStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME).toInstant()
        }
    }
}
