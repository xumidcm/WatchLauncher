package com.example.wlauncher.ui.onboarding

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wlauncher.service.WLauncherNotificationListener
import com.example.wlauncher.ui.theme.WatchColors

@Composable
fun PermissionRequestSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var notificationGranted by remember { mutableStateOf(WLauncherNotificationListener.isConnected()) }
    var activityGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                context.checkSelfPermission(android.Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    val activityPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        activityGranted = granted
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "欢迎",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "为了获得最佳体验，请授予以下权限",
            fontSize = 14.sp,
            color = WatchColors.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // 通知权限
        PermissionItem(
            icon = Icons.Filled.Notifications,
            title = "通知访问",
            description = "在通知中心显示您的通知",
            isGranted = notificationGranted,
            onClick = {
                if (!notificationGranted) {
                    context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }
            }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 步数权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            PermissionItem(
                icon = Icons.Filled.DirectionsWalk,
                title = "活动识别",
                description = "在表盘显示您的步数统计",
                isGranted = activityGranted,
                onClick = {
                    if (!activityGranted) {
                        activityPermissionLauncher.launch(android.Manifest.permission.ACTIVITY_RECOGNITION)
                    }
                }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // 继续按钮
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(WatchColors.ActiveCyan)
                .clickable { onDismiss() }
                .padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (notificationGranted && activityGranted) "开始使用" else "稍后设置",
                fontSize = 16.sp,
                fontWeight = FontWeight.W600,
                color = Color.Black
            )
        }
    }
}

@Composable
private fun PermissionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(WatchColors.SurfaceGlass)
            .clickable(enabled = !isGranted, onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isGranted) WatchColors.ActiveGreen.copy(alpha = 0.2f) else WatchColors.ActiveCyan.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isGranted) WatchColors.ActiveGreen else WatchColors.ActiveCyan,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.W600,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = WatchColors.TextTertiary
            )
        }
        if (isGranted) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = WatchColors.ActiveGreen,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = WatchColors.TextTertiary,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
