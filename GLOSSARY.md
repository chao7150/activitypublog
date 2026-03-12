# プロジェクト用語集 (Ubiquitous Language)

本プロジェクトにおける用語の定義を以下に定める。

## コア概念
- **Post (投稿)**: [名詞] 各プラットフォーム（GTS, Discord, WP）に投稿された個々の内容。内部的には ActivityStreams 2.0 準拠の `ActivityPost` として扱われる。
- **Actor (投稿者)**: [名詞] 投稿を行った主体。
- **Platform (プラットフォーム)**: [名詞] 投稿の発生元となる外部サービス（GoToSocial, Discord, WordPress）。
- **Digest (ダイジェスト)**: [名詞] 一定期間の複数の Post を集約・要約した内容。※Note のプロパティである summary と区別する。

## アーキテクチャ構成要素
- **Provider (プロバイダー)**: [インターフェース] 外部プラットフォームから Post を取得するための窓口。
- **Repository (リポジトリ)**: [インターフェース] 取得した Post を内部 DB に保存・検索するための窓口。
- **UseCase (ユースケース)**: [サービス] 「同期する」「ダイジェストを作る」といった、アプリケーション固有のビジネスロジック。

## アクション
- **Sync (同期)**: Provider から最新の Post を取得し、Repository に格納する処理。
- **Broadcast (配信)**: 生成したダイジェストを各プラットフォームへ逆投稿する処理。
- **Summarize (要約)**: 複数の Post からダイジェスト文を生成する処理（AI または Truncate）。
