package com.example.wlauncher.ui.smartstack

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wlauncher.ui.theme.WatchColors
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

/**
 * 智能叠放层 - 对应 HTML 原型中的 #smart-stack。
 * 顶部小时钟 + 多个毛玻璃 widget 卡片。
 */
@Composable
fun SmartStackLayer(
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf("") }
    var dayOfWeek by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dayFmt = SimpleDateFormat("EEEE", Locale.CHINESE)
        while (true) {
            val now = Date()
            currentTime = timeFmt.format(now)
            dayOfWeek = dayFmt.format(now)
            delay(1000)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .padding(top = 30.dp, start = 14.dp, end = 14.dp, bottom = 60.dp)
    ) {
        // 顶部时间行
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = currentTime,
                fontSize = 24.sp,
                fontWeight = FontWeight.W500,
                color = Color.White
            )
            Text(
                text = dayOfWeek,
                fontSize = 12.sp,
                color = WatchColors.TextSecondary
            )
        }

        // 正在播放 widget
        WidgetCard(
            iconTint = Color.White,
            icon = Icons.Filled.MusicNote,
            title = "正在播放",
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Text(
                text = "Blue Archive OST",
                fontSize = 15.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Icon(Icons.Filled.SkipPrevious, null, tint = Color.White, modifier = Modifier.size(22.dp))
                Icon(Icons.Filled.PlayArrow, null, tint = Color.White, modifier = Modifier.size(22.dp))
                Icon(Icons.Filled.SkipNext, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }

        // 天气 widget
        WidgetCard(
            iconTint = Color(0xFF12d8fa),
            icon = Icons.Filled.Cloud,
            title = "天气"
        ) {
            Text(
                text = "24°C 晴朗",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "继续上滑进入应用",
            fontSize = 12.sp,
            color = WatchColors.TextTertiary,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun WidgetCard(
    iconTint: Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(WatchColors.SurfaceGlass)
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(icon, null, tint = iconTint, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                color = WatchColors.TextSecondary
            )
        }
        content()
    }
}
