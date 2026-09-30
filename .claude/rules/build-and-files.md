# ビルドとファイルの扱い

## ビルド（Windows・Git Bash）
- ビルド：
  `JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew :app:assembleDebug -q`
- 警告の確認（**警告・エラーは0件にする**）：
  `JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew :app:compileDebugKotlin --rerun-tasks -q 2>&1 | grep "^w:\|^e:"`
- 作業を報告する前に、必ずビルドが通ることを確かめる。

## 改行コード
- ソースとドキュメントの多くは **CRLF**。新しく作るファイルも CRLF にそろえる。
- 確認：`file <パス>`（`with CRLF line terminators` と出ればよい）。
- Git Bash の `sed -i` は CRLF を LF に変えてしまうことがある。使ったあとは
  `perl -i -pe 's/\r?\n/\r\n/' <パス>` で CRLF に戻す。
- 複数行の書き換えは、なるべく Edit ツールで行う。

## 使える道具
- Python は入っていない。スクリプトが必要なときは perl か Bash を使う。

## Lint
- 次の指摘は、理由があって残している（直さない）：
  - `IconPackManager` の `getIdentifier`（アイコンパックの仕組み上必要）
  - 画面ロックの `InlinedApi`（実行時にバージョンを確かめている）
  - 依存ライブラリの更新のおすすめ
