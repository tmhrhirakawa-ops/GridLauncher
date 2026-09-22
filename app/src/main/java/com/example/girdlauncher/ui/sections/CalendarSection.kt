package com.example.girdlauncher.ui.sections

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
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

private enum class CalendarViewMode { MONTHLY, WEEKLY, DAILY }

private fun CalendarViewMode.next(): CalendarViewMode = when (this) {
    CalendarViewMode.MONTHLY -> CalendarViewMode.WEEKLY
    CalendarViewMode.WEEKLY -> CalendarViewMode.DAILY
    CalendarViewMode.DAILY -> CalendarViewMode.MONTHLY
}

/**
 * 予定1件分の情報（イベントID・タイトル・開始/終了時刻・終日フラグ）。
 */
private data class CalendarEvent(
    val eventId: Long,
    val title: String,
    val startMillis: Long,
    val endMillis: Long,
    val isAllDay: Boolean
)

/**
 * 予定をタップしたときに、標準カレンダーアプリでその予定自体を開く。
 */
private fun openEventInCalendar(context: android.content.Context, event: CalendarEvent) {
    try {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, event.eventId)
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.startMillis)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.endMillis)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

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
 * 指定した期間[startMillis, endMillis)に含まれる予定を、タイトル・開始/終了時刻付きで取得する。
 * Weekly/Dailyのアジェンダ表示に使う。
 */
private suspend fun fetchEvents(context: android.content.Context, startMillis: Long, endMillis: Long): List<CalendarEvent> = withContext(Dispatchers.IO) {
    val events = mutableListOf<CalendarEvent>()
    val projection = arrayOf(
        CalendarContract.Instances.EVENT_ID,
        CalendarContract.Instances.TITLE,
        CalendarContract.Instances.BEGIN,
        CalendarContract.Instances.END,
        CalendarContract.Instances.ALL_DAY
    )
    val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
    ContentUris.appendId(builder, startMillis)
    ContentUris.appendId(builder, endMillis)

    try {
        context.contentResolver.query(
            builder.build(), projection, null, null, "${CalendarContract.Instances.BEGIN} ASC"
        )?.use { cursor ->
            val eventIdCol = cursor.getColumnIndex(CalendarContract.Instances.EVENT_ID)
            val titleCol = cursor.getColumnIndex(CalendarContract.Instances.TITLE)
            val beginCol = cursor.getColumnIndex(CalendarContract.Instances.BEGIN)
            val endCol = cursor.getColumnIndex(CalendarContract.Instances.END)
            val allDayCol = cursor.getColumnIndex(CalendarContract.Instances.ALL_DAY)
            while (cursor.moveToNext()) {
                events.add(
                    CalendarEvent(
                        eventId = cursor.getLong(eventIdCol),
                        title = cursor.getString(titleCol)?.takeIf { it.isNotBlank() } ?: "(無題の予定)",
                        startMillis = cursor.getLong(beginCol),
                        endMillis = cursor.getLong(endCol),
                        isAllDay = cursor.getInt(allDayCol) != 0
                    )
                )
            }
        }
    } catch (e: Exception) { e.printStackTrace() }
    events
}

private fun formatWeekLabel(weekStart: LocalDate): String {
    val weekEnd = weekStart.plusDays(6)
    fun monthOf(d: LocalDate) = d.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH).uppercase()
    return if (weekStart.month == weekEnd.month) {
        "${monthOf(weekStart)} ${weekStart.dayOfMonth}-${weekEnd.dayOfMonth}, ${weekStart.year}"
    } else {
        "${monthOf(weekStart)} ${weekStart.dayOfMonth} - ${monthOf(weekEnd)} ${weekEnd.dayOfMonth}, ${weekEnd.year}"
    }
}

private fun formatDayLabel(day: LocalDate): String {
    val weekdayShort = day.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH).uppercase()
    val monthShort = day.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH).uppercase()
    return "$weekdayShort $monthShort ${day.dayOfMonth}, ${day.year}"
}

/**
 * 月間・週間・日間を切り替えられるカレンダーを表示するセクション。
 * 週間・日間表示では、その期間に含まれる予定の中身（時刻・タイトル）まで一覧表示する。
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

    var viewMode by remember { mutableStateOf(CalendarViewMode.MONTHLY) }

    val currentDate = LocalDate.now()
    val baseMonth = remember { YearMonth.now() }
    // 日曜始まりの週の開始日（MonthGridの曜日計算と揃える: 月曜=1...日曜=7 なので7で割った余り分だけ戻す）
    val baseWeekStart = remember { currentDate.minusDays((currentDate.dayOfWeek.value % 7).toLong()) }
    // 前後に大きくページを取っておき、実質無限にスワイプできるようにする
    val initialPage = Int.MAX_VALUE / 2
    val monthPagerState = rememberPagerState(initialPage = initialPage) { Int.MAX_VALUE }
    val weekPagerState = rememberPagerState(initialPage = initialPage) { Int.MAX_VALUE }
    val dayPagerState = rememberPagerState(initialPage = initialPage) { Int.MAX_VALUE }
    val coroutineScope = rememberCoroutineScope()

    val displayedMonth = remember(monthPagerState.currentPage) {
        baseMonth.plusMonths((monthPagerState.currentPage - initialPage).toLong())
    }
    val displayedWeekStart = remember(weekPagerState.currentPage) {
        baseWeekStart.plusWeeks((weekPagerState.currentPage - initialPage).toLong())
    }
    val displayedDay = remember(dayPagerState.currentPage) {
        currentDate.plusDays((dayPagerState.currentPage - initialPage).toLong())
    }

    val headerLabel = when (viewMode) {
        CalendarViewMode.MONTHLY -> displayedMonth.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH).uppercase() + " " + displayedMonth.year
        CalendarViewMode.WEEKLY -> formatWeekLabel(displayedWeekStart)
        CalendarViewMode.DAILY -> formatDayLabel(displayedDay)
    }
    // 現在の表示モードに応じたPagerState（ヘッダーの矢印・TODAYボタンの操作対象を切り替える）
    val activePagerState = when (viewMode) {
        CalendarViewMode.MONTHLY -> monthPagerState
        CalendarViewMode.WEEKLY -> weekPagerState
        CalendarViewMode.DAILY -> dayPagerState
    }
    val isOnToday = activePagerState.currentPage == initialPage

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
                    .size(7.dp)
                    .background(LocalCyberColors.current.accent))
                Spacer(modifier = Modifier.width(6.dp))
                Text("CALENDAR", fontFamily = CyberFont, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = LocalCyberColors.current.text)
                Spacer(modifier = Modifier.weight(1f))
                // 表示モード切り替え（1つのボタンで現在のモードを表示し、タップで循環させる）
                // Material3のSurface(onClick=...)はアクセシビリティ用に最小48dpのタッチ領域を
                // 強制するため、ここではあえてBox+clickableで組んで高さを詰めている
                Text(
                    text = viewMode.name,
                    fontFamily = CyberFont,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = LocalCyberColors.current.accent,
                    modifier = Modifier
                        .border(1.dp, LocalCyberColors.current.accent.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                        .clickable { viewMode = viewMode.next() }
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "TODAY",
                    fontFamily = CyberFont,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isOnToday) LocalCyberColors.current.text.copy(alpha = 0.3f) else LocalCyberColors.current.accent,
                    modifier = Modifier
                        .clickable(enabled = !isOnToday) {
                            coroutineScope.launch {
                                activePagerState.animateScrollToPage(initialPage)
                            }
                        }
                        .padding(horizontal = 3.dp, vertical = 1.dp)
                )
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = "‹",
                    fontFamily = CyberFont,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = LocalCyberColors.current.accent,
                    modifier = Modifier
                        .clickable {
                            coroutineScope.launch {
                                activePagerState.animateScrollToPage(activePagerState.currentPage - 1)
                            }
                        }
                        .padding(horizontal = 3.dp)
                )
                Text(headerLabel, fontFamily = CyberFont, fontSize = 9.sp, color = LocalCyberColors.current.accent, fontWeight = FontWeight.Bold)
                Text(
                    text = "›",
                    fontFamily = CyberFont,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = LocalCyberColors.current.accent,
                    modifier = Modifier
                        .clickable {
                            coroutineScope.launch {
                                activePagerState.animateScrollToPage(activePagerState.currentPage + 1)
                            }
                        }
                        .padding(horizontal = 3.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))

            when (viewMode) {
                CalendarViewMode.MONTHLY -> {
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
                        state = monthPagerState,
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
                CalendarViewMode.WEEKLY -> {
                    HorizontalPager(
                        state = weekPagerState,
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    ) { page ->
                        val weekStart = remember(page) { baseWeekStart.plusWeeks((page - initialPage).toLong()) }
                        WeekAgenda(
                            weekStart = weekStart,
                            currentDate = currentDate,
                            hasPermission = hasPermission,
                            context = context
                        )
                    }
                }
                CalendarViewMode.DAILY -> {
                    HorizontalPager(
                        state = dayPagerState,
                        modifier = Modifier.fillMaxWidth().weight(1f)
                    ) { page ->
                        val day = remember(page) { currentDate.plusDays((page - initialPage).toLong()) }
                        DayAgenda(
                            day = day,
                            hasPermission = hasPermission,
                            context = context
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    currentDate: LocalDate,
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
                                    color = if (isToday) LocalCyberColors.current.onAccent else LocalCyberColors.current.text
                                )
                                if (eventDays.contains(dayNumber)) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 2.dp, end = 2.dp)
                                            .size(4.dp)
                                            .background(if (isToday) LocalCyberColors.current.onAccent else LocalCyberColors.current.accent, CircleShape)
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

/**
 * 週間表示: 7列（日毎）のタイムライン形式。各列は上から下へ、その日の予定を時刻順に並べる。
 */
@Composable
private fun WeekAgenda(
    weekStart: LocalDate,
    currentDate: LocalDate,
    hasPermission: Boolean,
    context: android.content.Context
) {
    var events by remember(weekStart) { mutableStateOf(emptyList<CalendarEvent>()) }
    LaunchedEffect(hasPermission, weekStart) {
        if (hasPermission) {
            val startMillis = weekStart.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endMillis = weekStart.plusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            events = fetchEvents(context, startMillis, endMillis)
        }
    }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("H:mm") }
    val colors = LocalCyberColors.current

    Row(modifier = Modifier.fillMaxSize()) {
        for (i in 0 until 7) {
            val day = weekStart.plusDays(i.toLong())
            val isToday = day == currentDate
            val dayEvents = remember(events, day) {
                events.filter { event ->
                    Instant.ofEpochMilli(event.startMillis).atZone(ZoneId.systemDefault()).toLocalDate() == day
                }.sortedBy { it.startMillis }
            }

            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = day.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH).take(1).uppercase(),
                    fontFamily = CyberFont,
                    fontSize = 8.sp,
                    color = colors.text.copy(alpha = 0.5f)
                )
                Text(
                    text = day.dayOfMonth.toString(),
                    fontFamily = CyberFont,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isToday) colors.accent else colors.text
                )
                Spacer(modifier = Modifier.height(4.dp))

                if (!hasPermission) {
                    // 権限がない場合は列を空のまま表示する（先頭列にのみ案内を出すと偏るため省略）
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(dayEvents) { event ->
                            WeekEventChip(event, timeFormatter, onClick = { openEventInCalendar(context, event) })
                        }
                    }
                }
            }

            if (i < 6) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.dp)
                        .background(colors.border.copy(alpha = 0.5f))
                )
            }
        }
    }
}

@Composable
private fun WeekEventChip(event: CalendarEvent, timeFormatter: DateTimeFormatter, onClick: () -> Unit) {
    val colors = LocalCyberColors.current
    val timeText = if (event.isAllDay) "終日" else Instant.ofEpochMilli(event.startMillis).atZone(ZoneId.systemDefault()).toLocalTime().format(timeFormatter)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 1.dp)
            .background(colors.accent.copy(alpha = 0.15f), RoundedCornerShape(2.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 3.dp, vertical = 2.dp)
    ) {
        Text(timeText, fontFamily = CyberFont, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = colors.accent)
        Text(event.title, fontFamily = CyberFont, fontSize = 7.sp, color = colors.text, maxLines = 2)
    }
}

/**
 * 日間表示: 1日分の予定を時刻順に一覧できる。
 */
@Composable
private fun DayAgenda(
    day: LocalDate,
    hasPermission: Boolean,
    context: android.content.Context
) {
    var events by remember(day) { mutableStateOf(emptyList<CalendarEvent>()) }
    LaunchedEffect(hasPermission, day) {
        if (hasPermission) {
            val startMillis = day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endMillis = day.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            events = fetchEvents(context, startMillis, endMillis)
        }
    }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    when {
        !hasPermission -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "カレンダーの権限がありません",
                fontFamily = CyberFont,
                fontSize = 10.sp,
                color = LocalCyberColors.current.text.copy(alpha = 0.4f)
            )
        }
        events.isEmpty() -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "予定なし",
                fontFamily = CyberFont,
                fontSize = 10.sp,
                color = LocalCyberColors.current.text.copy(alpha = 0.4f)
            )
        }
        else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(events) { event ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { openEventInCalendar(context, event) }
                        .padding(vertical = 4.dp)
                ) {
                    val startText = if (event.isAllDay) "終日" else Instant.ofEpochMilli(event.startMillis).atZone(ZoneId.systemDefault()).toLocalTime().format(timeFormatter)
                    val endText = if (event.isAllDay) "" else Instant.ofEpochMilli(event.endMillis).atZone(ZoneId.systemDefault()).toLocalTime().format(timeFormatter)
                    Column(modifier = Modifier.width(48.dp)) {
                        Text(startText, fontFamily = CyberFont, fontSize = 9.sp, color = LocalCyberColors.current.accent)
                        if (endText.isNotEmpty()) {
                            Text(endText, fontFamily = CyberFont, fontSize = 8.sp, color = LocalCyberColors.current.text.copy(alpha = 0.4f))
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        event.title,
                        fontFamily = CyberFont,
                        fontSize = 11.sp,
                        color = LocalCyberColors.current.text,
                        maxLines = 2
                    )
                }
            }
        }
    }
}
