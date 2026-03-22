package com.example.wlauncher

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.util.Locale

class CrashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val crashInfo = intent.getStringExtra("crash_info") ?: readCache("crash_info.txt", "Unknown error")
        val crashBrief = intent.getStringExtra("crash_brief") ?: readCache("crash_brief.txt", "Unknown")
        val appInfo = intent.getStringExtra("crash_app_info") ?: readCache("crash_app_info.txt", "Unknown")
        val fullCrashLog = buildString {
            appendLine(appInfo)
            appendLine()
            appendLine(crashBrief)
            appendLine()
            append(crashInfo)
        }.trim()
        setContent {
            val isZh = remember { Locale.getDefault().language.startsWith("zh") }
            CrashScreen(
                isZh = isZh,
                crashInfo = crashInfo,
                appInfo = appInfo,
                onCopyLog = {
                    copyText(
                        fullCrashLog,
                        if (isZh) "已复制详细崩溃日志" else "Copied full crash log"
                    )
                },
                onRestart = { restart() },
                onOpenSettings = { openSettings() }
            )
        }
    }

    private fun readCache(name: String, fallback: String): String {
        return try {
            File(cacheDir, name).takeIf { it.exists() }?.readText() ?: fallback
        } catch (_: Exception) {
            fallback
        }
    }

    private fun copyText(text: String, toastText: String) {
        (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager)
            .setPrimaryClip(ClipData.newPlainText("crash", text))
        Toast.makeText(this, toastText, Toast.LENGTH_SHORT).show()
    }

    private fun restart() {
        startActivity(packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })
        finish()
    }

    private fun openSettings() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
    }
}

@Composable
fun CrashScreen(
    isZh: Boolean,
    crashInfo: String,
    appInfo: String,
    onCopyLog: () -> Unit,
    onRestart: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(60.dp))
        Text(
            text = if (isZh) "哎呀，应用崩溃了" else "Oops, it crashed",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF111111)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (isZh) {
                "请复制完整日志给开发者，并附上触发崩溃的操作步骤。"
            } else {
                "Copy the logs for the developer and include the steps that caused the crash."
            },
            fontSize = 14.sp,
            color = Color(0xFF666666),
            lineHeight = 20.sp
        )
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFFFCEAEA))
                .padding(16.dp)
        ) {
            Text(
                text = "$appInfo\n\n$crashInfo",
                fontSize = 11.sp,
                color = Color(0xFF222222),
                modifier = Modifier.verticalScroll(rememberScrollState())
            )
        }

        Spacer(modifier = Modifier.height(18.dp))
        CrashButton(
            text = if (isZh) "复制详细崩溃日志" else "Copy full crash log",
            bg = Color(0xFFFF4D73),
            fg = Color.White,
            onClick = onCopyLog
        )
        Spacer(modifier = Modifier.height(10.dp))
        CrashButton(
            text = if (isZh) "重启应用" else "Restart app",
            bg = Color(0xFFF3F3F3),
            fg = Color(0xFFFF4D73),
            onClick = onRestart
        )
        Spacer(modifier = Modifier.height(10.dp))
        CrashButton(
            text = if (isZh) "应用详情" else "App settings",
            bg = Color(0xFFF3F3F3),
            fg = Color(0xFFFF4D73),
            onClick = onOpenSettings
        )
        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun CrashButton(text: String, bg: Color, fg: Color, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
    ) {
        Text(text, color = fg, fontSize = 16.sp, fontWeight = FontWeight.W600)
    }
}
