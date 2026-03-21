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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class CrashActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val crashInfo = intent.getStringExtra("crash_info") ?: "Unknown error"
        val crashBrief = intent.getStringExtra("crash_brief") ?: "Unknown"
        setContent {
            CrashScreen(crashInfo, crashBrief,
                onCopyDetail = { copyText(crashInfo) },
                onCopyBrief = { copyText(crashBrief) },
                onRestart = { restart() },
                onOpenSettings = { openSettings() })
        }
    }

    private fun copyText(t: String) {
        (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager)
            .setPrimaryClip(ClipData.newPlainText("crash", t))
        Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show()
    }

    private fun restart() {
        startActivity(packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })
        finish()
    }

    private fun openSettings() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:$packageName")))
    }
}

@Composable
fun CrashScreen(
    crashInfo: String,
    crashBrief: String,
    onCopyDetail: () -> Unit,
    onCopyBrief: () -> Unit,
    onRestart: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(60.dp))
        Text("哎呀，崩溃了", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE53935))
        Spacer(modifier = Modifier.height(6.dp))
        Text("Σ(っ °Д °;)っ 请将日志复制给开发者并提供导致出错的操作步骤",
            fontSize = 13.sp, color = Color.Gray, lineHeight = 18.sp)
        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier.weight(1f).fillMaxWidth()
                .clip(RoundedCornerShape(12.dp)).background(Color(0xFF1A1A1A)).padding(12.dp)
        ) {
            Text(crashInfo, fontSize = 11.sp, color = Color(0xFFAAAAAA),
                modifier = Modifier.verticalScroll(rememberScrollState()))
        }

        Spacer(modifier = Modifier.height(16.dp))
        CrashBtn("复制详细信息", Color(0xFFE53935), Color.White, onCopyDetail)
        Spacer(modifier = Modifier.height(8.dp))
        CrashBtn("复制简略信息", Color(0xFF2A2A2A), Color.White, onCopyBrief)
        Spacer(modifier = Modifier.height(8.dp))
        CrashBtn("重启软件", Color(0xFF2A2A2A), Color.White, onRestart)
        Spacer(modifier = Modifier.height(8.dp))
        CrashBtn("应用详情", Color(0xFF2A2A2A), Color.White, onOpenSettings)
        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
private fun CrashBtn(text: String, bg: Color, fg: Color, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(48.dp)
            .clip(RoundedCornerShape(12.dp)).background(bg)
    ) { Text(text, color = fg, fontSize = 15.sp, fontWeight = FontWeight.W500) }
}
