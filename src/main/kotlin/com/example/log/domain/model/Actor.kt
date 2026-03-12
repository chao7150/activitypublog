package com.example.log.domain.model

import java.time.Instant

/**
 * ActivityStreams 2.0 における Actor (主語) を表現する。
 * 例: "chao@gotosocial.example.com"
 */
data class Actor(
    val name: String,         // 表示名
    val preferredUsername: String, // ユーザー名
    val host: String,         // インスタンスホスト
    val profileUrl: String?,  // プロフィールへのリンク
    val avatarUrl: String?    // アバター画像へのリンク
) {
    /**
     * "username@host" 形式の識別子を返す
     */
    fun identifier(): String = "$preferredUsername@$host"
}
