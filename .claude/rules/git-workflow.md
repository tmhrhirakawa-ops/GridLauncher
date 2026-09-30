# Git の進め方

## ブランチ
- 新しい作業を始めるときは、`develop` から作業ブランチを切る（`feature/<topic>`）。
  `develop` に直接コミットしない。
- `master` はリリース用。触らない。

## コミット
- コミットは、ユーザーが「コミットして」と言ったときだけ行う。
- `.idea/` の変更は**絶対にコミットしない**（`git add app/src docs` のように、対象を指定して追加する）。
- 署名鍵（`/key/`）やビルド成果物（`/app/release/`）もコミットしない。
- メッセージは日本語で、何をしたかが分かる1行にする。最後に次の行を付ける：
  `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`

## マージ
- 「マージして」と言われたら、`develop` へ fast-forward でマージし、作業ブランチを消す：
  `git checkout develop && git merge --ff-only feature/<topic> && git branch -d feature/<topic>`
- 「コミットしてマージして」と言われたら、両方を続けて行う。
- **push はしない**（頼まれたときだけ）。
- 終わったら、コミットのハッシュ・今いるブランチ・push していないことを報告する。
