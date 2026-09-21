package com.example.girdlauncher.ui.sections

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.girdlauncher.ui.theme.CyberFont
import com.example.girdlauncher.ui.theme.LocalCyberColors
import java.util.Locale

/**
 * 月間カレンダーを表示するセクション。
 *
 * @param modifier レイアウトに適用するModifier。
 */
@Composable
fun CalendarSection(modifier: Modifier = Modifier) {
    val currentDate = java.time.LocalDate.now()
    val currentMonth = java.time.YearMonth.now()
    val monthString = currentMonth.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH).uppercase() + " " + currentMonth.year
    
    // カレンダーの計算
    val firstDayOfMonth = currentMonth.atDay(1)
    val daysInMonth = currentMonth.lengthOfMonth()
    // getDayOfWeek() は月曜=1, 日曜=7. 日曜始まりにするための計算
    val startDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7 
    
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = LocalCyberColors.current.panel.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, LocalCyberColors.current.border),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // ヘッダー部分
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier
                    .size(8.dp)
                    .background(LocalCyberColors.current.accent))
                Spacer(modifier = Modifier.width(8.dp))
                Text("CALENDAR", fontFamily = CyberFont, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = LocalCyberColors.current.text)
                Text(" // MONTHLY", fontFamily = CyberFont, fontSize = 10.sp, color = LocalCyberColors.current.text.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.weight(1f))
                Text(monthString, fontFamily = CyberFont, fontSize = 10.sp, color = LocalCyberColors.current.accent, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(0.dp))
            
            // 曜日ヘッダー
            val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                daysOfWeek.forEach { day ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(day, fontFamily = CyberFont, fontSize = 10.sp, color = LocalCyberColors.current.text.copy(alpha = 0.5f))
                    }
                }
            }
            Spacer(modifier = Modifier.height(0.dp))
            
            // カレンダーグリッド
            val totalCells = startDayOfWeek + daysInMonth
            val rows = (totalCells + 6) / 7
            
            Column(modifier = Modifier.fillMaxWidth().weight(1f), verticalArrangement = Arrangement.SpaceEvenly) {
                for (r in 0 until rows) {
                    Row(modifier = Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
                        for (c in 0 until 7) {
                            val cellIndex = r * 7 + c
                            val dayNumber = cellIndex - startDayOfWeek + 1
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (dayNumber in 1..daysInMonth) {
                                    val isToday = dayNumber == currentDate.dayOfMonth
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                color = if (isToday) LocalCyberColors.current.accent else Color.Transparent,
                                                shape = RoundedCornerShape(4.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = dayNumber.toString(),
                                            fontFamily = CyberFont,
                                            fontSize = 12.sp,
                                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isToday) Color.White else LocalCyberColors.current.text
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
