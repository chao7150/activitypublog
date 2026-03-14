-- PGroonga 拡張を有効にする (存在しない場合)
CREATE EXTENSION IF NOT EXISTS pgroonga;

-- content カラムに日本語全文検索用のインデックスを張る
-- (JPA の自動生成後に実行されることを想定)
CREATE INDEX IF NOT EXISTS pgroonga_content_index ON posts USING pgroonga (content);
