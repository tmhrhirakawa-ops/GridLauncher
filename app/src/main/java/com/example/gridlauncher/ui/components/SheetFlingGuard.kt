package com.example.gridlauncher.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Velocity

/**
 * ModalBottomSheet内のスクロールする中身に付ける。中身を下端まで勢いよくスクロールしたとき、
 * 使い切れなかった上向きのフリングの勢いがシート本体に渡らないように、ここで吸収する。
 *
 * Material3のModalBottomSheetは、中身の余ったフリングをそのままシートのアニメーション
 * （弾むスプリング）に渡すため、シートが既に展開済みでも上へ突き上げられる。上端付近では
 * シートの位置に応じてステータスバー分の余白が増減し、シートの高さ＝アンカーが動くため、
 * アニメーションが収束せずにバウンドし続けてしまう。下向き（シートを閉じる方向）の勢いは
 * そのまま渡すので、中身を上端までスクロールした勢いでシートを閉じる操作は従来どおり使える。
 */
fun Modifier.consumeUpwardSheetFling(): Modifier = nestedScroll(UpwardFlingConsumer)

/** 状態を持たないので、全シートで1つのインスタンスを共有する。 */
private object UpwardFlingConsumer : NestedScrollConnection {
    override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity =
        if (available.y < 0f) Velocity(0f, available.y) else Velocity.Zero
}
