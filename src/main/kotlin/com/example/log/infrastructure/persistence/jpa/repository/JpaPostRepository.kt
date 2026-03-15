package com.example.log.infrastructure.persistence.jpa.repository

import com.example.log.domain.model.PlatformType
import com.example.log.infrastructure.persistence.jpa.entity.PostJpaEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.*

@Repository
interface JpaPostRepository : JpaRepository<PostJpaEntity, UUID> {
    fun findTopByPlatformOrderByPublishedAtDesc(platform: PlatformType): PostJpaEntity?
    fun findByPlatformAndOriginalId(platform: PlatformType, originalId: String): PostJpaEntity?
    fun findAllByOrderByPublishedAtDesc(): List<PostJpaEntity>

    // PGroonga を使った日本語全文検索
    @Query(value = "SELECT * FROM posts WHERE content &@ :keyword ORDER BY published_at DESC", nativeQuery = true)
    fun searchByPgroonga(@Param("keyword") keyword: String): List<PostJpaEntity>
}
