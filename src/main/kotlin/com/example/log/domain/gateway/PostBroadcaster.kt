package com.example.log.domain.gateway

import com.example.log.domain.model.ActivityPost

/**
 * 生成された投稿（ダイジェスト等）を外部プラットフォームへ配信するためのインターフェース。
 */
interface PostBroadcaster {
    /**
     * 指定された投稿を配信する。
     */
    fun broadcast(post: ActivityPost)
}
