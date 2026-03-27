package com.example.wlauncher.ui.drawer

import android.content.Context
import android.content.Intent
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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.wlauncher.data.model.AppInfo
import com.example.wlauncher.ui.anim.platformBlur
import kotlinx.coroutines.delay

@Composable
fun AppShortcutOverlay(
    app: AppInfo,
    blurEnabled: Boolean = true,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showing by remember { mutableStateOf(false) }
    var dismissing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { showing = true }

    fun animateDismiss() {
        dismissing = true
        showing = false
    }

    LaunchedEffect(dismissing) {
        if (dismissing) {
            delay(220)
            onDismiss()
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (showing && !dismissing) 1f else 0f,
        animationSpec = tween(180),
        label = "shortcut_alpha"
    )
    val scale by animateFloatAsState(
        targetValue = if (showing && !dismissing) 1f else 0.92f,
        animationSpec = spring(dampingRatio = 0.76f, stiffness = 560f),
        label = "shortcut_scale"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha }
            .background(Color.Black.copy(alpha = 0.72f))
            .platformBlur(blurRadiusDp = 4f, enabled = blurEnabled)
            .clickable(indication = null, interactionSource = null) { animateDismiss() },
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current
        val maxMenuHeight = with(density) { maxHeight * 0.44f }
        val scrollState = rememberScrollState()

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clickable(indication = null, interactionSource = null) { }
                .padding(horizontal = 18.dp)
        ) {
            Column(
                modifier = Modifier
                    .width(220.dp)
                    .heightIn(max = maxMenuHeight)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF232326))
                    .verticalScroll(scrollState)
            ) {
                ShortcutMenuItem("应用信息") {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.parse("package:${app.packageName}")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                    animateDismiss()
                }
                DividerLine()
                ShortcutMenuItem("卸载", Color(0xFFFF5A54)) {
                    launchUninstall(context, app.packageName)
                    animateDismiss()
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Image(
                bitmap = app.cachedIcon,
                contentDescription = null,
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = app.label, color = Color.White, fontSize = 13.sp)
        }
    }
}

@Composable
private fun DividerLine() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(Color(0xFF48484A))
    )
}

@Composable
private fun ShortcutMenuItem(
    text: String,
    color: Color = Color.White,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(text = text, color = color, fontSize = 15.sp)
    }
}

private fun launchUninstall(context: Context, packageName: String) {
    val packageUri = Uri.parse("package:$packageName")
    val candidates = listOf(
        Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageUri),
        Intent(Intent.ACTION_DELETE, packageUri)
    ).map {
        it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        it.putExtra(Intent.EXTRA_RETURN_RESULT, true)
        it
    }

    val launched = candidates.firstOrNull { it.resolveActivity(context.packageManager) != null }?.let {
        context.startActivity(it)
        true
    } ?: false

    if (!launched) {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        )
    }
}

fun vibrateHaptic(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            manager.defaultVibrator.vibrate(
                VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    } catch (_: Exception) {
    }
}
