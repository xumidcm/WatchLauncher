package com.example.wlauncher.ui.notification

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wlauncher.ui.theme.WatchColors
import kotlinx.coroutines.launch

data class NotificationItem(
    val id: Int,
    val app: String,
    val iconTint: Color,
    val title: String,
    val body: String
)

/**
 * 通知中心 - 下滑打开，支持左滑删除。
 */
@Composable
fun NotificationLayer(
    modifier: Modifier = Modifier
) {
    var notifications by remember {
        mutableStateOf(
            listOf(
                NotificationItem(1, "微信", Color(0xFF34C759), "微信", "今天下班去跑步吧，运动卡点等你打卡！"),
                NotificationItem(2, "健康", Color(0xFFFF416C), "健康", "恭喜，您已达到今日卡路里目标。")
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(top = 30.dp, start = 14.dp, end = 14.dp, bottom = 60.dp)
    ) {
        Text(
            text = "通知中心",
            fontSize = 14.sp,
            color = WatchColors.TextSecondary,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 16.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(notifications, key = { _, n -> n.id }) { _, noti ->
                SwipeToDeleteCard(
                    onDelete = { notifications = notifications.filter { it.id != noti.id } }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(WatchColors.SurfaceGlass)
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.Chat,
                                contentDescription = null,
                                tint = noti.iconTint,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = noti.app,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = noti.body,
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * 左滑删除包装 - 对应 HTML 原型的 .swipe-wrap。
 */
@Composable
private fun SwipeToDeleteCard(
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        // 红色删除背景
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFFFF3B30))
                .clickable { onDelete() },
            contentAlignment = Alignment.CenterEnd
        ) {
            Icon(
                Icons.Filled.Close,
                contentDescription = "删除",
                tint = Color.White,
                modifier = Modifier.padding(end = 20.dp)
            )
        }

        // 前景卡片
        Box(
            modifier = Modifier
                .graphicsLayer { translationX = offsetX.value }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dx ->
                            scope.launch {
                                val newVal = (offsetX.value + dx).coerceIn(-200f, 0f)
                                offsetX.snapTo(newVal)
                            }
                        },
                        onDragEnd = {
                            scope.launch {
                                if (offsetX.value < -100f) {
                                    offsetX.animateTo(-200f, tween(200))
                                    onDelete()
                                } else {
                                    offsetX.animateTo(0f, tween(200))
                                }
                            }
                        }
                    )
                }
        ) {
            content()
        }
    }
}
