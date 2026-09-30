---
paths:
  - "app/src/**/*.kt"
---

# Kotlin / Compose のコードの書き方

## コメント
- コメント・KDoc は日本語で、周りのコードと同じくらいの密度で書く。
  「なぜそうしているか」（過去の不具合・端末ごとの違い・ポリシー上の理由）を残す。
- 画面の文言も日本語。ボタン名などの英語は、アプリの世界観（`ACCESS GRID` など）に合わせる。

## 状態と再コンポジション
- `pointerInput(Unit)` など、一度だけ起動する処理に渡すコールバックは、
  `rememberUpdatedState` で最新の値を読む。
- ローカル関数の参照（`::localFun`）は、Compose が使い回して古い状態のまま動くことがある。
  コールバックには**ラムダで包んで**渡す（`{ x -> localFun(x) }`）。
- 状態を読む関数の中では、関数の外で計算した値を使わず、呼ばれた時点の状態を読む。
- 頻繁に変わる値（再生位置など）は、`drawBehind` など描画の段階で読み、再コンポジションを起こさない。
- アプリ全体に効く設定（フォント・アイコンパック）は、グローバルな状態（`CyberFont`・`IconPackManager.active`）にする。

## 名前
- `var foo` と `fun setFoo()` は JVM の名前がぶつかる。状態を変える関数は `updateFoo()` などにする。

## 電池と重さ
- 定期的な更新は、ホーム画面が見えている間だけ行う（`repeatOnLifecycle(STARTED)`・`LifecycleEventEffect`）。
- 時刻・電池などはポーリングせず、ブロードキャストで更新する。
- 重い処理（アイコンパックの読み込み・使用状況・カレンダー）は `Dispatchers.IO` で行う。
- アプリ一覧は `InstalledAppsCache` を使い回す（画面を作り直すたびに読み込み直さない）。

## 画面の部品
- ダイアログ・シートは、`CompositionLocalProvider(LocalCyberColors provides colors)` の中で出す
  （外で出すとライトテーマの既定の配色になってしまう）。
- 文字は `CyberFont` を使う。色は `LocalCyberColors` から取る（色を直書きしない。危険色などの例外を除く）。
- 設定は SharedPreferences `cyber_launcher` に保存する。新しい設定を足したら、
  バックアップ（`LauncherBackup`）の対象になっているかを確かめる。

## ファイルの分け方
- ホーム画面は `ui/screens/CyberLauncherScreen.kt` が本体。画面の状態に依存しない処理は
  `Launcher*.kt`（`LauncherAppDrop` / `LauncherWidgetRules` / `LauncherEffects` /
  `LauncherThemeState` / `LauncherScreenParts`）に分ける。詳しくは `docs/architecture.md`。
