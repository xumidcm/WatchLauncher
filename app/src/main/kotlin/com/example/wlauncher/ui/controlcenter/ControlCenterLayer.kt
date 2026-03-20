package com.example.wlauncher.ui.controlcenter

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wlauncher.ui.theme.WatchColors

/**
 * 控制中心 - 对应截图第一张 Apple Watch 控制中心。
 * 2x3 网格布局，毛玻璃卡片，支持开关切换。
 */
@Composable
fun ControlCenterLayer(
    modifier: Modifier = Modifier
) {
    var wifiOn by remember { mutableStateOf(true) }
    var cellularOn by remember { mutableStateOf(true) }
    var silentOn by remember { mutableStateOf(false) }
    var dndOn by remember { mutableStateOf(false) }
    var theaterOn by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(top = 36.dp, start = 14.dp, end = 14.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Row 1: Cellular + WiFi
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CCButton(
                icon = Icons.Filled.CellTower,
                label = "蜂窝",
                isActive = cellularOn,
                activeColor = WatchColors.ActiveGreen,
                onClick = { cellularOn = !cellularOn },
                modifier = Modifier.weight(1f)
            )
            CCButton(
                icon = Icons.Filled.Wifi,
                label = "Wi-Fi",
                isActive = wifiOn,
                activeColor = WatchColors.ActiveBlue,
                onClick = { wifiOn = !wifiOn },
                modifier = Modifier.weight(1f)
            )
        }

        // Row 2: Battery + Silent
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Battery (read-only display)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(WatchColors.SurfaceGlass)
                    .padding(12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "69%",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = WatchColors.ActiveGreen
                    )
                }
            }
            CCButton(
                icon = Icons.Filled.NotificationsOff,
                label = "静音",
                isActive = silentOn,
                activeColor = WatchColors.ActiveRed,
                onClick = { silentOn = !silentOn },
                modifier = Modifier.weight(1f)
            )
        }

        // Row 3: Theater + DND
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            CCButton(
                icon = Icons.Filled.TheaterComedy,
                label = "剧院",
                isActive = theaterOn,
                activeColor = WatchColors.ActiveGreen,
                onClick = { theaterOn = !theaterOn },
                modifier = Modifier.weight(1f)
            )
            CCButton(
                icon = Icons.Filled.DarkMode,
                label = "勿扰",
                isActive = dndOn,
                activeColor = WatchColors.ActiveBlue,
                onClick = { dndOn = !dndOn },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun CCButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isActive) activeColor.copy(alpha = 0.85f) else WatchColors.SurfaceGlass

    Box(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                color = Color.White,
                fontWeight = FontWeight.W500
            )
        }
    }
}
