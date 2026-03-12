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

## ADR 6: フロントエンド技術の選定 (HTMX + Tailwind CSS)
- **Status**: Accepted
- **Context**: 複雑な SPA フレームワークは避けたいが、モダンな操作感は欲しい。
- **Decision**: HTMX によるサーバーサイド駆動の動的 UI と、Tailwind CSS によるデザイン。
- **Consequences**: JavaScript の記述量を最小限に抑えつつ、サクサク動く UI を実現する。
