package com.example.log.domain.gateway

import com.example.log.domain.model.ActivityPost
import com.example.log.domain.model.PlatformType

/**
 * 各外部プラットフォームから投稿を取得するためのインターフェース。
 */
interface PostProvider {
    /**
     * 最新の投稿を取得する。
     * @param sinceId 前回取得した最後のID（これより新しいものを取得）
     */
    fun fetchLatestPosts(sinceId: String?): List<ActivityPost>
    
    /**
     * このプロバイダーが担当するプラットフォームの種類
     */
    fun getSupportedPlatform(): PlatformType
}
