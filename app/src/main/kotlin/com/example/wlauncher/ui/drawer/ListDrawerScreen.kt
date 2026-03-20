package com.example.wlauncher.ui.drawer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.wlauncher.data.model.AppInfo
import com.example.wlauncher.ui.theme.WatchColors

/**
 * 列表样式应用抽屉 - 对应 watchOS 设置列表视图截图。
 * 每个应用一行：圆形图标 + 应用名，玻璃拟态卡片。
 */
@Composable
fun ListDrawerScreen(
    apps: List<AppInfo>,
    onAppClick: (AppInfo) -> Unit,
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentPadding = PaddingValues(
            top = 40.dp,
            bottom = 60.dp,
            start = 12.dp,
            end = 12.dp
        ),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // 设置入口
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(WatchColors.SurfaceGlass)
                    .clickable { onSettingsClick() }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = "设置",
                    tint = WatchColors.TextSecondary,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "桌面设置",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.W500,
                    color = WatchColors.ActiveCyan
                )
            }
        }

        items(apps, key = { it.packageName }) { app ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(WatchColors.SurfaceGlass)
                    .clickable { onAppClick(app) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    bitmap = app.icon.toBitmap(96, 96).asImageBitmap(),
                    contentDescription = app.label,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = app.label,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.W500,
                    color = Color.White
                )
            }
        }
    }
}
