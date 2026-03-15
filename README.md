# Activity Log (ActivityPub Log)

複数プラットフォーム（GoToSocial, WordPress, Discord等）の投稿を一箇所に集約し、全文検索や日次ダイジェスト配信を自動化するツール。

## Features
- **マルチソース同期**: ActivityStreams 2.0 に基づき、GoToSocial, WordPress などから投稿を取得。
- **日本語全文検索**: PostgreSQL + PGroonga による高度な日本語検索。
- **ダイナミックタイムライン**: Thymeleaf + HTMX + Tailwind CSS による高速でモダンなUI。
- **自動ダイジェスト**: 毎朝8時（JST）に過去24時間の投稿を要約し、Discordへ自動配信。
- **コンテナ化デプロイ**: Docker Compose による容易な展開。

## Development
```bash
# データベースの起動
cd container && nerdctl compose up -d

# アプリケーションの起動 (Local)
env $(grep -v '^#' .env | xargs) ./gradlew bootRun
```

## Production Deployment (sakura)
1. **設定**: 
    - `~/activitypublog_2` ディレクトリにソース一式を配置。
    - `.env` ファイルに GTS や Discord の認証情報を設定。
2. **起動**:
    ```bash
    docker compose up -d --build
    ```
    - ホストの **8082** ポートで待ち受けます。
3. **管理用 API (手動トリガー)**:
    - 同期実行: `curl -X POST http://localhost:8082/api/admin/sync`
    - ダイジェスト配信: `curl -X POST http://localhost:8082/api/admin/digest`

## Architecture
- **Language**: Kotlin 1.9 (Java 21)
- **Framework**: Spring Boot 3.2.3
- **Pattern**: Clean Architecture (Domain / UseCase / Infrastructure / Presentation)
- **UI**: Thymeleaf + HTMX + Tailwind CSS
