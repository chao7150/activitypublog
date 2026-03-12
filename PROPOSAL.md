# 次世代個人投稿集約プラットフォーム (Personal Post Aggregator) 設計案

## 1. プロジェクト概要
既存の Mastodon ログ保存ツールを大幅にアップグレードし、GoToSocial (ActivityPub), Discord, WordPress といった複数のプラットフォームから自分の投稿を集約し、認証されたユーザーのみが閲覧できるプライベートなタイムラインを提供する。

## 2. 技術スタック (JVM Ecosystem)
- **言語**: Kotlin (Coroutines による非同期並列処理を活用)
- **フレームワーク**: Spring Boot 3.x
- **データベース**: MySQL または PostgreSQL (Spring Data JPA / Exposed)
- **アーキテクチャ**: クリーンアーキテクチャ (Clean Architecture)
- **フロントエンド**: HTMX + Tailwind CSS (サーバーサイドレンダリング + 動的UI)
- **認証**: Spring Security (パスワードベースのセッション認証)

## 3. コアアーキテクチャ (クリーンアーキテクチャ)

### Domain Layer (中心部)
- **ActivityPost (Entity)**: ActivityStreams 2.0 に準拠した統一データモデル。
    - `Note` (SNS投稿) や `Article` (ブログ記事) をサポート。
    - `source` フィールドにより、オリジナルのプラットフォーム情報を保持。
- **PostSourceProvider (Interface)**: 各プラットフォームからデータを取得するための抽象インターフェース。

### Use Case Layer
- **SyncPostsUseCase**: 定期的に各 Provider を巡回し、新規投稿を DB に同期。
- **GetTimelineUseCase**: 集約された投稿を時系列で取得。

### Infrastructure Layer (詳細)
- **API Clients**: GoToSocial, Discord, WordPress の各 REST API 実装。
- **Persistence**: データベースへの永続化ロジック。
- **Security**: サイト全体のアクセス制限。

## 4. データモデル (ActivityStreams 2.0 準拠)
すべての外部投稿を以下の論理構造にマッピングする：

- **Type**: `Note` (SNS), `Article` (Blog), `Image` (Media)
- **Actor**: 投稿者の識別子 (例: `chao@gotosocial.example.com`)
- **Content**: 正規化された HTML 内容
- **Published**: 投稿日時 (ISO 8601 / Instant)
- **URL**: オリジナル投稿へのパーマリンク
- **Metadata**: オリジナルプラットフォーム固有のデータ (JSON)

## 5. 追加機能：自動ダイジェスト配信 (Daily Digest)
- **24時間ダイジェストの生成**: 全ソースの投稿を統合し、設定されたスケジュールに基づき定期配信を行う。
- **配信タイミングの管理**: 管理画面から配信時間（1日数回）を動的に設定可能。
- **AI 要約エンジン (プラグイン形式)**: 
    - 初期実装: 単純なテキスト切り出し (Truncate)。
    - 将来実装: 軽量・低性能LLMを活用した「味のある/面白い」要約生成。
- **リッチなプレビュー (OGP)**: ダイジェストのパーマリンクにおいて、内容を反映した動的な OGP タグを生成し、リンク先を開かずとも内容が伝わるようにする。
- **マルチプラットフォーム出力**: 収集した情報を元に、GTS, Discord, WP のそれぞれへ最適化された形式でダイジェストを逆投稿する。

## 6. 非同期同期メカニズム (Sync Strategy)
- **定期バックグラウンド同期**: スケジュールに基づき全ソースを巡回。
- **アクセス時バックグラウンドフェッチ**: ユーザーがアクセスした際、DBから即座にデータを返しつつ、裏側で最新情報を取得・更新する。
- **リアルタイム連携 (Optional)**: Discord Bot や WebSub 等を用いたプッシュ型取得の検討。

## 7. 実装ロードマップ
1. **Domain & Data Modeling**: ActivityStreams 2.0 に基づく Entity 定義。
2. **Core Logic Implementation**: 同期ロジック、リポジトリ、サマリ生成の抽象化。
3. **Infrastructure Setup**: Spring Boot 3 プロジェクトの構築と DB 接続。
4. **Provider & Client Implementation**: 各プラットフォーム用 Adapter (Read/Write 両対応) の実装。
5. **Authentication & Admin UI**: サイト全体の保護と配信設定画面。
6. **Frontend Development**: Tailwind と HTMX によるタイムラインとサマリ表示。


---
*このドキュメントは、プロジェクトの進行に合わせて随時更新される。*
