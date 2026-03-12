# Testing Strategy

本プロジェクトでは、堅牢なビジネスロジックを維持するために、**TDD (Test-Driven Development)** を基本としたテスト戦略を採用する。

## 1. テストの分類

### Unit Tests (ドメイン・ユースケース)
- **対象**: `domain/*`, `usecase/*`
- **目的**: ビジネスルール、計算ロジック、手順の正当性を検証。
- **原則**: 外部依存（DB, API）をすべて MockK で差し替え、メモリ上ですべて完結。1秒以内に実行可能にする。
- **推奨スタック**: JUnit 5 + MockK + Kotest (Assertions)。

### Integration Tests (インフラ)
- **対象**: `infrastructure/persistence/*`, `infrastructure/api/*`
- **目的**: 実際のデータベース接続や外部 API クライアントの動作検証。
- **手段**: `Testcontainers` (MySQL/PostgreSQL) や `MockWebServer` を活用。

## 2. TDD サイクル
和田卓人 (@twada) 氏の提唱する手法に基づき、以下のサイクルを回す：
1. **Red**: 達成したい仕様をテストコードとして記述し、失敗させる。
2. **Green**: テストを通すための「最小限」のコードを書く。
3. **Refactor**: テストが通っている状態を保ったまま、コードを洗練させる（可読性、保守性の向上）。

## 3. 命名規則と構造
- **テスト名**: `should ... when ...` 形式、または日本語で「〜のとき、〜となるべき」と詳細に記述し、**テストコード自体を仕様書（Documentation）として扱う**。
- **構造**: `Given - When - Then` の構成を保つ。

---
*テストは信頼の源であり、リファクタリングの防波堤である。*
