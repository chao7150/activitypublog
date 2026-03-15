# Architectural Decision Records (ADR)

このプロジェクトにおける重要なアーキテクチャ上の意思決定を記録する。

## ADR 1: Go から Kotlin/Spring Boot (JVM) への移行
- **Status**: Accepted
- **Context**: 従来の Go 実装では DB 操作が原始的であり、並列でのデータフェッチや複雑なドメインロジック（ActivityPub 連携）の記述が煩雑であった。
- **Decision**: 
    - 言語: Kotlin 1.9+ (Coroutines による非同期処理の容易さ)
    - フレームワーク: Spring Boot 3.x (エコシステムの豊富さと Security 強化)
    - 実行環境: Java 21 (Virtual Threads による高いスループット)
- **Consequences**: 型安全な開発、高度な抽象化、将来的な AI (LLM) 連携の容易さが得られる。

## ADR 2: クリーンアーキテクチャの採用
- **Status**: Accepted
- **Context**: Discord, GoToSocial, WordPress など、外界の API 仕様が多岐にわたり、かつ変更される可能性がある。
- **Decision**: `Domain`, `UseCase`, `Infrastructure`, `Presentation` の 4 層からなるクリーンアーキテクチャを採用する。
- **Consequences**: ビジネスロジックが外部ライブラリや API の変更に依存せず、テストが容易になる。

## ADR 3: ActivityStreams 2.0 によるデータ正規化
- **Status**: Accepted
- **Context**: 異なるプラットフォームの投稿を統合する際、独自のフォーマットを作ると拡張性が乏しくなる。
- **Decision**: 内部ドメインモデルおよびデータベーススキーマのベースとして、ActivityStreams 2.0 (AS2) 仕様を採用する。
- **Consequences**: 将来的に ActivityPub サーバーとしての機能を付与する際、変換コストを最小化できる。

## ADR 4: ハイブリッド同期戦略 (Hybrid Sync)
- **Status**: Accepted
- **Context**: ユーザーアクセス時に API を叩くと遅延が発生し、バッチのみだと鮮度が落ちる。
- **Decision**: 
    1. 定期的なバックグラウンド同期
    2. ユーザーアクセス時に非同期（Coroutine）でフェッチを開始し、レスポンスは即座に DB から返す。
- **Consequences**: 高速なレスポンスとデータの鮮度を両立する。

## ADR 5: Digest (ダイジェスト) 生成と再配信
- **Status**: Accepted
- **Context**: 収集した投稿を振り返りやすくし、かつ各プラットフォームへ活動を再発信したい。
- **Decision**: 24時間ごとの `Digest` を自動生成し、これを `INTERNAL` プラットフォーム発の `ActivityPost` として管理、さらに各外部サービスへ `Broadcast`（再投稿）する。
- **Consequences**: 単なるログツールではなく、コンテンツの再循環ハブとして機能させる。

## ADR 7: PostgreSQL + PGroonga による日本語全文検索
- **Status**: Accepted
- **Context**: 過去の膨大な投稿から日本語の内容を高速かつ正確に検索したい。
- **Decision**: DB に PostgreSQL を採用し、日本語全文検索エンジン `PGroonga` を導入する。
- **Consequences**: `&@` 演算子による形態素解析ベースの高度な日本語検索が可能になる。

## ADR 8: インフラ層における Entity の厳格な分離
- **Status**: Accepted
- **Context**: ドメインモデルを特定の技術（JPA/Hibernate）に依存させたくない。
- **Decision**: 内部ドメイン Entity (`ActivityPost`) と JPA Entity (`PostJpaEntity`) を完全に分離し、Repository 実装クラスで相互にマッピングする。
- **Consequences**: 技術スタックの変更に強くなり、ドメイン層の純粋性が保たれるが、マッピングコードの維持コストが発生する。

## ADR 9: UI Framework - HTMX and Tailwind CSS (2026-03-14)
- **Status**: Accepted
- **Context**: フロントエンド開発（React/Vueなど）の学習コストや管理を最小化しつつ、動的なUIを実現したい。
- **Decision**: **HTMX** と **Thymeleaf** (SSR) を組み合わせる。スタイリングには **Tailwind CSS** を採用する。
- **Consequences**: 「Sync Now」ボタンや「無限スクロール」を最小限のJavaScriptで実装可能になり、開発速度が向上した。

## ADR 10: スケジューリング戦略 (2026-03-14)
- **Status**: Accepted
- **Context**: 投稿の鮮度維持と、決まった時間（朝8時）のダイジェスト配信を自動化したい。
- **Decision**: Spring Boot の `@Scheduled` を使用し、同期は1時間毎、ダイジェスト生成・配信は `0 0 8 * * *` (JST) で実行する。
- **Consequences**: サーバーを立ち上げておくだけで、外部プラットフォームの活動が自動的に集約・再発信されるサイクルが完成した。

## ADR 11: 本番環境デプロイ設定 (2026-03-14)
- **Status**: Accepted
- **Context**: 本番サーバー `sakura` には既に GoToSocial (8080) 等が稼働しており、ポート競合やディレクトリ管理に配慮が必要であった。
- **Decision**: 
  - 本プロジェクトの待ち受けポートを **8082** に変更。
  - ディレクトリ `~/activitypublog_2` を新規作成し、既存のコードを温存しつつデプロイ。
- **Consequences**: 既存サービスに影響を与えることなく、最新版のコンテナデプロイと動作確認に成功した。
