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
        setContent {
            val isZh = remember { Locale.getDefault().language.startsWith("zh") }
            CrashScreen(
                isZh = isZh,
                crashInfo = crashInfo,
                crashBrief = crashBrief,
                appInfo = appInfo,
                onCopyDetail = { copyText(crashInfo, if (isZh) "已复制详细信息" else "Copied details") },
                onCopyBrief = { copyText(crashBrief, if (isZh) "已复制错误摘要" else "Copied error summary") },
                onCopyAppInfo = { copyText(appInfo, if (isZh) "已复制应用信息" else "Copied app info") },
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
    crashBrief: String,
    appInfo: String,
    onCopyDetail: () -> Unit,
    onCopyBrief: () -> Unit,
    onCopyAppInfo: () -> Unit,
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
            text = if (isZh) "哎呀，崩溃了" else "Oops, it crashed",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF111111)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (isZh) "请将日志复制给开发者，并提供导致错误的操作步骤" else "Copy the logs for the developer and include the steps that caused the crash.",
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
        CrashButton(if (isZh) "复制详细信息" else "Copy details", Color(0xFFFF4D73), Color.White, onCopyDetail)
        Spacer(modifier = Modifier.height(10.dp))
        CrashButton(if (isZh) "复制错误摘要" else "Copy error summary", Color(0xFFF3F3F3), Color(0xFFFF4D73), onCopyBrief)
        Spacer(modifier = Modifier.height(10.dp))
        CrashButton(if (isZh) "复制应用信息" else "Copy app info", Color(0xFFF3F3F3), Color(0xFFFF4D73), onCopyAppInfo)
        Spacer(modifier = Modifier.height(10.dp))
        CrashButton(if (isZh) "重启软件" else "Restart app", Color(0xFFF3F3F3), Color(0xFFFF4D73), onRestart)
        Spacer(modifier = Modifier.height(10.dp))
        CrashButton(if (isZh) "应用详情" else "App settings", Color(0xFFF3F3F3), Color(0xFFFF4D73), onOpenSettings)
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
