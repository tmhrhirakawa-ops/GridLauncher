package com.example.gridlauncher.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.gridlauncher.ui.theme.CyberFont
import com.example.gridlauncher.ui.theme.LocalCyberColors

/**
 * ALL APPS・SELECT APPのシート上部の見出し。右上の虫眼鏡をタップすると、その下に検索ボックスを
 * 出してキーボードを開く（普段は検索ボックスを出さず、アプリ一覧を広く見せる）。
 * 検索ボックスの×で、入力を消して閉じる。
 *
 * @param title 見出しの文字。
 * @param query 検索語。
 * @param onQueryChange 検索語が変わったときのコールバック。
 * @param actions 虫眼鏡の左に並べるボタン（並べ替えボタンなど）。
 */
@Composable
fun SearchableSheetHeader(
    title: String,
    query: String,
    onQueryChange: (String) -> Unit,
    actions: @Composable () -> Unit = {}
) {
    val colors = LocalCyberColors.current
    // 検索語が入っている間は（シートを開き直した場合なども）検索ボックスを出したままにする
    var isSearchOpen by remember { mutableStateOf(query.isNotEmpty()) }
    val focusRequester = remember { FocusRequester() }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                title,
                fontFamily = CyberFont,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = colors.accent,
                modifier = Modifier.weight(1f)
            )
            actions()
            if (!isSearchOpen) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { isSearchOpen = true }
                ) {
                    Icon(Icons.Outlined.Search, contentDescription = "Search", tint = colors.accent, modifier = Modifier.size(24.dp))
                }
            }
        }
        AnimatedVisibility(
            visible = isSearchOpen,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("SEARCH APPS...", fontFamily = CyberFont, fontSize = 14.sp, color = colors.text.copy(alpha = 0.5f)) },
                singleLine = true,
                textStyle = TextStyle(fontFamily = CyberFont, color = colors.text),
                trailingIcon = {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "Close search",
                        tint = colors.text.copy(alpha = 0.7f),
                        modifier = Modifier
                            .clip(CircleShape)
                            .clickable {
                                onQueryChange("")
                                isSearchOpen = false
                            }
                            .padding(4.dp)
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.accent,
                    unfocusedBorderColor = colors.border,
                    cursorColor = colors.accent
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .focusRequester(focusRequester)
            )
        }
    }
    // 虫眼鏡で開いたら、すぐ入力できるようにキーボードを出す
    LaunchedEffect(isSearchOpen) {
        if (isSearchOpen) focusRequester.requestFocus()
    }
}
