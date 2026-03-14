package com.example.log.infrastructure.persistence.jpa.entity

import com.example.log.domain.model.PlatformType
import jakarta.persistence.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "posts")
class PostJpaEntity(
    @Id
    val id: UUID,

    @Column(nullable = false)
    val actorIdentifier: String,

    @Column(nullable = false)
    val actorName: String,

    @Column(columnDefinition = "TEXT")
    val content: String,

    @Column(nullable = false)
    val publishedAt: Instant,

    @Column
    val url: String?,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val platform: PlatformType,

    @Column(nullable = false)
    val originalId: String,

    @Column(nullable = false)
    val fetchedAt: Instant = Instant.now(),

    @Column(columnDefinition = "TEXT")
    val summary: String? = null,

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "post_id")
    val attachments: List<AttachmentJpaEntity> = emptyList()
)

@Entity
@Table(name = "attachments")
class AttachmentJpaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val type: String,

    @Column(nullable = false)
    val url: String,

    @Column(columnDefinition = "TEXT")
    val description: String? = null
)
