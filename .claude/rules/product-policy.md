# アプリの方針（PRO・ポリシー・プライバシー）

## PRO（有料機能の買い切り解放）
- PRO の機能は、使う直前に `requirePro("機能名")` で確かめる。未購入なら購入画面を出す。
- PRO でなくなっても、すでに置いてあるもの（スタック・NOW PLAYING ウィジェットなど）は表示し続ける（配置を壊さない）。
- PRO の機能を増やした・変えたときは、次の3か所をそろえて直す：
  - 購入画面の一覧（`ui/components/ProComponents.kt` の `ProFeatureGroups`）
  - ストアの掲載文（`docs/store-listing.md`）
  - 機能一覧の PRO の章（`docs/features.md`）
- 価格は買い切り。公開時は ¥480 で、様子を見て ¥700 前後まで上げる予定。サブスクにはしない。

## 通信とプライバシー
- 外部との通信は Google Play の課金だけ。`INTERNET` を使う機能は足さない。
- 通知は、内容を保持・送信しない。持つのは「どのアプリに何件あるか」だけ。

## Google Play のポリシー
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` は使わない（ホームアプリは対象外）。
  バッテリー最適化は、端末の一覧画面（`ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`）を開いて案内する。
- アクセシビリティサービスは、申告した用途だけに使う（電源メニュー、ジェスチャーでの通知パネル・
  クイック設定・最近使ったアプリ・画面オフ）。用途を増やすときは `docs/requirements.md` の申告の項目も直す。
- 権限を足す・外すときは、`AndroidManifest.xml` と `docs/requirements.md` の権限の表をそろえる。
