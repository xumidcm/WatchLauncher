package com.example.wlauncher.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.wlauncher.ui.navigation.LayoutMode
import com.example.wlauncher.ui.theme.WatchColors

/**
 * 桌面设置页面 - 从应用列表顶部进入。
 * 包含：布局切换、模糊动画开关、关于信息等。
 */
@Composable
fun LauncherSettingsSheet(
    currentLayout: LayoutMode,
    blurEnabled: Boolean,
    onLayoutChange: (LayoutMode) -> Unit,
    onBlurToggle: (Boolean) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(top = 30.dp, start = 14.dp, end = 14.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "桌面设置",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // 布局模式
        item { SectionHeader("应用列表布局") }
        item {
            SettingOption(
                label = "蜂窝布局",
                description = "watchOS 经典圆形网格",
                isSelected = currentLayout == LayoutMode.Honeycomb,
                onClick = { onLayoutChange(LayoutMode.Honeycomb) }
            )
        }
        item {
            SettingOption(
                label = "列表布局",
                description = "按字母排序的线性列表",
                isSelected = currentLayout == LayoutMode.List,
                onClick = { onLayoutChange(LayoutMode.List) }
            )
        }

        // 动画设置
        item { SectionHeader("动画效果") }
        item {
            SettingToggle(
                label = "模糊动画",
                description = "需要 Android 12+ (API 31)",
                isOn = blurEnabled,
                onToggle = onBlurToggle
            )
        }

        // 关于
        item { SectionHeader("关于") }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(WatchColors.SurfaceGlass)
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("WatchLauncher", fontSize = 14.sp, fontWeight = FontWeight.W600, color = Color.White)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text("v1.0 · watchOS 26 Style", fontSize = 12.sp, color = WatchColors.TextTertiary)
                }
            }
        }

        // 返回按钮
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(WatchColors.SurfaceGlass)
                    .clickable { onDismiss() }
                    .padding(14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("返回", fontSize = 14.sp, color = WatchColors.ActiveCyan)
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = WatchColors.TextTertiary,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp, start = 4.dp)
    )
}

@Composable
private fun SettingOption(
    label: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) WatchColors.ActiveCyan.copy(alpha = 0.2f)
                else WatchColors.SurfaceGlass
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.W600, color = Color.White)
            Spacer(modifier = Modifier.height(2.dp))
            Text(description, fontSize = 12.sp, color = WatchColors.TextTertiary)
        }
        if (isSelected) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = WatchColors.ActiveCyan,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SettingToggle(
    label: String,
    description: String,
    isOn: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(WatchColors.SurfaceGlass)
            .clickable { onToggle(!isOn) }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.W600, color = Color.White)
            Spacer(modifier = Modifier.height(2.dp))
            Text(description, fontSize = 12.sp, color = WatchColors.TextTertiary)
        }
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isOn) WatchColors.ActiveGreen else Color(0xFF555555)
                ),
            contentAlignment = if (isOn) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .padding(2.dp)
                    .size(20.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White)
            )
        }
    }
}
