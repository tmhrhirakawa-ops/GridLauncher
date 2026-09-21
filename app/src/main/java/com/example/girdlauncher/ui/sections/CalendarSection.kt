package com.example.girdlauncher.ui.sections

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import android.content.Intent
import android.provider.CalendarContract
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.ContentUris
import java.time.ZoneId
import java.time.Instant
import java.time.YearMonth
import androidx.compose.foundation.shape.CircleShape
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect

suspend fun fetchEventDays(context: android.content.Context, yearMonth: java.time.YearMonth): Set<Int> = withContext(Dispatchers.IO) {
    val daysWithEvents = mutableSetOf<Int>()
    val startMillis = yearMonth.atDay(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val endMillis = yearMonth.atEndOfMonth().atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    val projection = arrayOf(CalendarContract.Instances.BEGIN)
    val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
    ContentUris.appendId(builder, startMillis)
    ContentUris.appendId(builder, endMillis)

    try {
        context.contentResolver.query(
            builder.build(), projection, null, null, null
        )?.use { cursor ->
            val beginCol = cursor.getColumnIndex(CalendarContract.Instances.BEGIN)
            while (cursor.moveToNext()) {
                val begin = cursor.getLong(beginCol)
                val date = Instant.ofEpochMilli(begin).atZone(ZoneId.systemDefault()).toLocalDate()
                if ((date.year == yearMonth.year) && (date.month == yearMonth.month)) {
                    daysWithEvents.add(date.dayOfMonth)
                }
            }
        }
    } catch(e: Exception) { e.printStackTrace() }
    daysWithEvents
}

/**
 * 月間カレンダーを表示するセクション。
 *
 * @param modifier レイアウトに適用するModifier。
 */
@Composable
fun CalendarSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED)
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        hasPermission = isGranted
    }
    
    var permissionRequested by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!hasPermission && !permissionRequested) {
            permissionRequested = true
            permissionLauncher.launch(Manifest.permission.READ_CALENDAR)
        }
    }

    val currentDate = java.time.LocalDate.now()
    val baseMonth = remember { YearMonth.now() }
    // 前後に大きくページを取っておき、実質無限にスワイプできるようにする
    val initialPage = Int.MAX_VALUE / 2
    val pagerState = rememberPagerState(initialPage = initialPage) { Int.MAX_VALUE }
    val coroutineScope = rememberCoroutineScope()

    val displayedMonth = remember(pagerState.currentPage) {
        baseMonth.plusMonths((pagerState.currentPage - initialPage).toLong())
    }
    val monthString = displayedMonth.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH).uppercase() + " " + displayedMonth.year

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
                Text(
                    text = "‹",
                    fontFamily = CyberFont,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = LocalCyberColors.current.accent,
                    modifier = Modifier
                        .clickable {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        }
                        .padding(horizontal = 6.dp)
                )
                Text(monthString, fontFamily = CyberFont, fontSize = 10.sp, color = LocalCyberColors.current.accent, fontWeight = FontWeight.Bold)
                Text(
                    text = "›",
                    fontFamily = CyberFont,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = LocalCyberColors.current.accent,
                    modifier = Modifier
                        .clickable {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                        .padding(horizontal = 6.dp)
                )
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

            // 月ごとに横スクロール（スワイプ）できるカレンダーグリッド
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxWidth().weight(1f)
            ) { page ->
                val month = remember(page) { baseMonth.plusMonths((page - initialPage).toLong()) }
                MonthGrid(
                    month = month,
                    currentDate = currentDate,
                    hasPermission = hasPermission,
                    context = context
                )
            }
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    currentDate: java.time.LocalDate,
    hasPermission: Boolean,
    context: android.content.Context
) {
    var eventDays by remember(month) { mutableStateOf(emptySet<Int>()) }
    LaunchedEffect(hasPermission, month) {
        if (hasPermission) {
            eventDays = fetchEventDays(context, month)
        }
    }

    // カレンダーの計算
    val firstDayOfMonth = month.atDay(1)
    val daysInMonth = month.lengthOfMonth()
    // getDayOfWeek() は月曜=1, 日曜=7. 日曜始まりにするための計算
    val startDayOfWeek = firstDayOfMonth.dayOfWeek.value % 7

    val totalCells = startDayOfWeek + daysInMonth
    val rows = (totalCells + 6) / 7

    Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(), verticalArrangement = Arrangement.SpaceEvenly) {
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
                            val isToday = month == YearMonth.from(currentDate) && dayNumber == currentDate.dayOfMonth
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        color = if (isToday) LocalCyberColors.current.accent else Color.Transparent,
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .clickable {
                                        // タップした日付からミリ秒のUnixタイムスタンプを作成
                                        val targetDate = month.atDay(dayNumber)
                                        val timeInMillis = targetDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

                                        val intent = Intent(Intent.ACTION_VIEW).apply {
                                            data = "content://com.android.calendar/time/$timeInMillis".toUri()
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        try {
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayNumber.toString(),
                                    fontFamily = CyberFont,
                                    fontSize = 12.sp,
                                    fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isToday) Color.White else LocalCyberColors.current.text
                                )
                                if (eventDays.contains(dayNumber)) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 2.dp, end = 2.dp)
                                            .size(4.dp)
                                            .background(if (isToday) Color.White else LocalCyberColors.current.accent, CircleShape)
                                            .align(Alignment.TopEnd)
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
