package com.example.wlauncher.data.repository

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.example.wlauncher.data.model.AppInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppRepository(private val context: Context) {

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            refresh()
        }
    }

    init {
        refresh()
        // 监听应用安装/卸载
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        context.registerReceiver(packageReceiver, filter)
    }

    fun refresh() {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos: List<ResolveInfo> = pm.queryIntentActivities(mainIntent, 0)
        val myPackage = context.packageName

        _apps.value = resolveInfos
            .filter { it.activityInfo.packageName != myPackage }
            .map { ri ->
                val drawable = ri.loadIcon(pm)
                AppInfo(
                    label = ri.loadLabel(pm).toString(),
                    packageName = ri.activityInfo.packageName,
                    activityName = ri.activityInfo.name,
                    icon = drawable,
                    cachedIcon = drawable.toBitmap(128, 128).asImageBitmap()
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    fun launchApp(appInfo: AppInfo) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            component = android.content.ComponentName(appInfo.packageName, appInfo.activityName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        val options = android.app.ActivityOptions.makeCustomAnimation(
            context,
            android.R.anim.fade_in,
            android.R.anim.fade_out
        )
        context.startActivity(intent, options.toBundle())
    }

    fun destroy() {
        try { context.unregisterReceiver(packageReceiver) } catch (_: Exception) {}
    }
}
