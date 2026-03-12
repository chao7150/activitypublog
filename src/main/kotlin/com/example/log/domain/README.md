# Domain Layer

このパッケージは、アプリケーションの**ビジネスロジックの中心**であり、最も重要な「核」です。
外部のフレームワーク、データベース、API クライアントなどに一切依存してはいけません（依存性の逆転原則）。

## ディレクトリ構成と役割

### 1. model/ (Entities & Value Objects)
- 投稿 (`ActivityPost`) や投稿者 (`Actor`) など、ビジネス上の概念をデータ構造として表現したもの。
- ActivityStreams 2.0 の仕様に準拠し、不変（Immutable）なデータクラスとして定義する。

### 2. gateway/ (Interfaces / Boundaries)
- データベースへの保存 (`PostRepository`) や外部 API からの取得 (`PostProvider`) など、**外界との通信手段を「抽象化」したインターフェース**を置く。
- 具体的な実装（MySQL, HttpClient 等）は Infrastructure 層に置く。

### 3. service/ (Domain Services)
- 単一の Entity（モデル）の責務に収まらない、複数の Entity を跨ぐ計算や判定のロジックを置く。
- 例: `Summarizer`（複数の投稿からダイジェスト文を生成するロジックの定義）。
- **注意**: 手順（DBから取ってきて〜など）を書く場所ではなく、純粋な「計算ルール」を書く場所とする。
