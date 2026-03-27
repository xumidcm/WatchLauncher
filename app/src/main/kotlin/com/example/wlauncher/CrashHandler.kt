package com.example.wlauncher

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.os.Process
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

class CrashHandler(private val context: Context) : Thread.UncaughtExceptionHandler {

    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    fun install() {
        Thread.setDefaultUncaughtExceptionHandler(this)
    }

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val crashInfo = buildCrashInfo(throwable)
            val crashBrief = "${throwable.javaClass.simpleName}: ${throwable.message}"
            val appInfo = buildAppInfo()
            persistCrashPayload(crashInfo, crashBrief, appInfo)
            showCrashImmediately(crashInfo, crashBrief, appInfo)
        } catch (_: Exception) {
            defaultHandler?.uncaughtException(thread, throwable)
        }

        try {
            Thread.sleep(80)
        } catch (_: Exception) {
        }

        Process.killProcess(Process.myPid())
        exitProcess(1)
    }

    private fun buildCrashInfo(throwable: Throwable): String {
        val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        return buildString {
            appendLine("Application Config:")
            appendLine("- Build Version: ${BuildConfig.VERSION_NAME}-${BuildConfig.VERSION_CODE}")
            appendLine("- Build Code: ${BuildConfig.VERSION_CODE}")
            appendLine("- Current Date: $time")
            appendLine()
            appendLine("Device Config:")
            appendLine("- Model: ${Build.MODEL}")
            appendLine("- Manufacturer: ${Build.MANUFACTURER}")
            appendLine("- SDK: ${Build.VERSION.SDK_INT}")
            appendLine("- ABIs: ${Build.SUPPORTED_ABIS.joinToString(", ")}")
            appendLine()
            appendLine("Stack Trace:")
            append(throwable.stackTraceToString())
        }
    }

    private fun buildAppInfo(): String = buildString {
        appendLine("Package: ${context.packageName}")
        appendLine("Version: ${BuildConfig.VERSION_NAME}")
        appendLine("Build Code: ${BuildConfig.VERSION_CODE}")
    }

    private fun persistCrashPayload(crashInfo: String, crashBrief: String, appInfo: String) {
        File(context.cacheDir, "crash_info.txt").writeText(crashInfo)
        File(context.cacheDir, "crash_brief.txt").writeText(crashBrief)
        File(context.cacheDir, "crash_app_info.txt").writeText(appInfo)
    }

    private fun showCrashImmediately(crashInfo: String, crashBrief: String, appInfo: String) {
        val intent = CrashActivity.createCrashIntent(
            context = context,
            crashInfo = crashInfo,
            crashBrief = crashBrief,
            appInfo = appInfo
        )
        val pendingIntent = PendingIntent.getActivity(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        runCatching { pendingIntent.send() }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = System.currentTimeMillis() + 40L
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC, triggerAt, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC, triggerAt, pendingIntent)
        }
    }
}
