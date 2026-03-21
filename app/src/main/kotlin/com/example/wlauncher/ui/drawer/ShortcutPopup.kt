package com.example.wlauncher.ui.drawer

import android.content.Context
import android.content.Intent
import android.graphics.RenderEffect
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wlauncher.data.model.AppInfo
import kotlinx.coroutines.delay

/**
 * 长按菜单覆盖层 — 背景压暗（API31+ 模糊），图标+菜单居中弹出
 * 有入场和退场动画
 */
@Composable
fun AppShortcutOverlay(
    app: AppInfo,
    blurEnabled: Boolean = true,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val useBlur = blurEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    // 动画状态：true = 显示中，false = 退出中
    var showing by remember { mutableStateOf(false) }
    var dismissing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showing = true
    }

    // 退出时先播放动画再调用 onDismiss
    fun animateDismiss() {
        dismissing = true
        showing = false
    }

    LaunchedEffect(dismissing) {
        if (dismissing) {
            delay(250) // 等退出动画完成
            onDismiss()
        }
    }

    val animAlpha by animateFloatAsState(
        targetValue = if (showing && !dismissing) 1f else 0f,
        animationSpec = tween(200),
        label = "overlay_alpha"
    )
    val animScale by animateFloatAsState(
        targetValue = if (showing && !dismissing) 1f else 0.8f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 600f),
        label = "overlay_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = animAlpha }
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(indication = null, interactionSource = null) { animateDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = animScale
                    scaleY = animScale
                }
                .clickable(indication = null, interactionSource = null) { /* 阻止穿透 */ }
        ) {
            // 菜单
            Column(
                modifier = Modifier
                    .width(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF2C2C2E))
            ) {
                ShortcutMenuItem("应用信息") {
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${app.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                    onDismiss()
                }
                Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Color(0xFF48484A)))
                ShortcutMenuItem("卸载", Color(0xFFFF453A)) {
                    context.startActivity(Intent(Intent.ACTION_DELETE).apply {
                        data = Uri.parse("package:${app.packageName}")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                    onDismiss()
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 应用图标
            Image(
                bitmap = app.cachedIcon,
                contentDescription = null,
                modifier = Modifier.size(88.dp).clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(app.label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.W600)
        }
    }
}

@Composable
private fun ShortcutMenuItem(text: String, color: Color = Color.White, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(text, color = color, fontSize = 15.sp, fontWeight = FontWeight.W500)
    }
}

fun vibrateHaptic(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            v.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    } catch (_: Exception) {}
}
