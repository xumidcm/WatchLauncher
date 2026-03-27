package com.example.wlauncher

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

private const val EXTRA_MODE = "mode"
private const val EXTRA_TITLE = "title"
private const val EXTRA_SUMMARY = "summary"
private const val EXTRA_DETAIL = "detail"
private const val EXTRA_PACKAGE = "package_name"
private const val EXTRA_CRASH_INFO = "crash_info"
private const val EXTRA_CRASH_BRIEF = "crash_brief"
private const val EXTRA_CRASH_APP_INFO = "crash_app_info"

private const val MODE_CRASH = "crash"
private const val MODE_ERROR = "error"

private const val TITLE_CRASH = "\u542f\u52a8\u5668\u53d1\u751f\u5d29\u6e83"
private const val TITLE_LAUNCH_ERROR = "\u5e94\u7528\u6253\u5f00\u5931\u8d25"
private const val TEXT_UNKNOWN_ERROR = "\u672a\u77e5\u9519\u8bef"
private const val TEXT_UNKNOWN_CRASH = "\u672a\u77e5\u5d29\u6e83"
private const val TEXT_UNKNOWN_APP = "\u672a\u77e5\u5e94\u7528\u4fe1\u606f"
private const val TEXT_ERROR_SUMMARY = "\u672a\u77e5\u95ee\u9898"
private const val TEXT_ERROR_DETAIL = "\u6ca1\u6709\u66f4\u591a\u9519\u8bef\u4fe1\u606f\u3002"
private const val TEXT_COPY_DONE = "\u5df2\u590d\u5236\u5b8c\u6574\u65e5\u5fd7"
private const val TEXT_CRASH_INTRO = "\u5df2\u62e6\u622a\u5d29\u6e83\u5e76\u4fdd\u7559\u65e5\u5fd7\uff0c\u4f60\u53ef\u4ee5\u5148\u590d\u5236\u65e5\u5fd7\uff0c\u518d\u51b3\u5b9a\u91cd\u542f\u6216\u67e5\u770b\u5e94\u7528\u8be6\u60c5\u3002"
private const val TEXT_ERROR_INTRO = "\u8fd9\u6b21\u542f\u52a8\u6ca1\u6709\u6210\u529f\uff0c\u5df2\u963b\u6b62\u542f\u52a8\u5668\u7ee7\u7eed\u5d29\u6e83\u3002\u4f60\u53ef\u4ee5\u67e5\u770b\u6458\u8981\u3001\u5c55\u5f00\u8be6\u60c5\uff0c\u6216\u76f4\u63a5\u6253\u5f00\u5e94\u7528\u8be6\u60c5\u9875\u3002"
private const val TEXT_SUMMARY = "\u6458\u8981"
private const val TEXT_EXPAND = "\u5c55\u5f00\u8be6\u60c5"
private const val TEXT_COLLAPSE = "\u6536\u8d77\u8be6\u60c5"
private const val TEXT_COPY = "\u590d\u5236\u5b8c\u6574\u65e5\u5fd7"
private const val TEXT_RESTART = "\u91cd\u542f\u542f\u52a8\u5668"
private const val TEXT_SETTINGS = "\u6253\u5f00\u5e94\u7528\u8be6\u60c5"
private const val TEXT_CLOSE = "\u5173\u95ed\u9875\u9762"

class CrashActivity : ComponentActivity() {

    companion object {
        fun createCrashIntent(
            context: Context,
            crashInfo: String,
            crashBrief: String,
            appInfo: String
        ): Intent {
            return Intent(context, CrashActivity::class.java).apply {
                putExtra(EXTRA_MODE, MODE_CRASH)
                putExtra(EXTRA_CRASH_INFO, crashInfo)
                putExtra(EXTRA_CRASH_BRIEF, crashBrief)
                putExtra(EXTRA_CRASH_APP_INFO, appInfo)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
        }

        fun createErrorIntent(
            context: Context,
            title: String,
            summary: String,
            detail: String,
            packageName: String? = null
        ): Intent {
            return Intent(context, CrashActivity::class.java).apply {
                putExtra(EXTRA_MODE, MODE_ERROR)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_SUMMARY, summary)
                putExtra(EXTRA_DETAIL, detail)
                putExtra(EXTRA_PACKAGE, packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_CRASH
        val targetPackage = intent.getStringExtra(EXTRA_PACKAGE)

        val screenModel = if (mode == MODE_ERROR) {
            val title = intent.getStringExtra(EXTRA_TITLE) ?: TITLE_LAUNCH_ERROR
            val summary = intent.getStringExtra(EXTRA_SUMMARY) ?: TEXT_ERROR_SUMMARY
            val detail = intent.getStringExtra(EXTRA_DETAIL) ?: TEXT_ERROR_DETAIL
            ScreenModel(
                title = title,
                summary = summary,
                detail = detail,
                fullLog = buildString {
                    appendLine(title)
                    appendLine()
                    appendLine(summary)
                    appendLine()
                    append(detail)
                }.trim(),
                packageName = targetPackage,
                isCrash = false
            )
        } else {
            val crashInfo = intent.getStringExtra(EXTRA_CRASH_INFO) ?: readCache("crash_info.txt", TEXT_UNKNOWN_ERROR)
            val crashBrief = intent.getStringExtra(EXTRA_CRASH_BRIEF) ?: readCache("crash_brief.txt", TEXT_UNKNOWN_CRASH)
            val appInfo = intent.getStringExtra(EXTRA_CRASH_APP_INFO) ?: readCache("crash_app_info.txt", TEXT_UNKNOWN_APP)
            ScreenModel(
                title = TITLE_CRASH,
                summary = crashBrief,
                detail = buildString {
                    appendLine(appInfo)
                    appendLine()
                    append(crashInfo)
                }.trim(),
                fullLog = buildString {
                    appendLine(appInfo)
                    appendLine()
                    appendLine(crashBrief)
                    appendLine()
                    append(crashInfo)
                }.trim(),
                packageName = targetPackage,
                isCrash = true
            )
        }

        setContent {
            CrashScreen(
                model = screenModel,
                onCopyLog = { copyText(screenModel.fullLog) },
                onRestart = { restartLauncher() },
                onOpenSettings = { openSettings(screenModel.packageName) },
                onClose = { finish() }
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

    private fun copyText(text: String) {
        (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager)
            .setPrimaryClip(ClipData.newPlainText("launcher_error", text))
        Toast.makeText(this, TEXT_COPY_DONE, Toast.LENGTH_SHORT).show()
    }

    private fun restartLauncher() {
        startActivity(packageManager.getLaunchIntentForPackage(packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })
        finish()
    }

    private fun openSettings(targetPackage: String?) {
        val pkg = targetPackage ?: packageName
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg")))
    }
}

private data class ScreenModel(
    val title: String,
    val summary: String,
    val detail: String,
    val fullLog: String,
    val packageName: String?,
    val isCrash: Boolean
)

@Composable
private fun CrashScreen(
    model: ScreenModel,
    onCopyLog: () -> Unit,
    onRestart: () -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = model.title,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = if (model.isCrash) TEXT_CRASH_INTRO else TEXT_ERROR_INTRO,
            fontSize = 13.sp,
            lineHeight = 20.sp,
            color = Color(0xFFB5B5B5)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(Color(0xFF161618))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = TEXT_SUMMARY,
                    fontSize = 13.sp,
                    color = Color(0xFF7FC7FF),
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = model.summary,
                    fontSize = 15.sp,
                    color = Color.White,
                    lineHeight = 22.sp,
                    maxLines = if (expanded) Int.MAX_VALUE else 4,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (expanded) TEXT_COLLAPSE else TEXT_EXPAND,
                    fontSize = 13.sp,
                    color = Color(0xFF7FC7FF),
                    modifier = Modifier.clickable { expanded = !expanded }
                )
                if (expanded) {
                    Text(
                        text = model.detail,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = Color(0xFFD7D7D7),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        ActionButton(TEXT_COPY, Color(0xFF1F7BFF), Color.White, onCopyLog)
        if (model.isCrash) {
            ActionButton(TEXT_RESTART, Color(0xFF2A2A2D), Color.White, onRestart)
        }
        if (model.packageName != null) {
            ActionButton(TEXT_SETTINGS, Color(0xFF2A2A2D), Color.White, onOpenSettings)
        }
        ActionButton(TEXT_CLOSE, Color(0xFF2A2A2D), Color.White, onClose)
        Spacer(modifier = Modifier.height(12.dp))
    }
}

@Composable
private fun ActionButton(
    text: String,
    background: Color,
    foreground: Color,
    onClick: () -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(background)
    ) {
        Text(text = text, color = foreground, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
    }
}
