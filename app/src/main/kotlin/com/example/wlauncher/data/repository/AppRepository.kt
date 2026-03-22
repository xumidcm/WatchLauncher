package com.example.wlauncher.data.repository

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.example.wlauncher.data.model.AppInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AppRepository(private val context: Context) {

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    private var customOrder: List<String> = emptyList()

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            refresh()
        }
    }

    init {
        refresh()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED)
            addDataScheme("package")
        }
        context.registerReceiver(packageReceiver, filter)
    }

    fun setCustomOrder(order: List<String>) {
        customOrder = order
        reorder()
    }

    private fun reorder() {
        if (customOrder.isEmpty()) return
        val current = _apps.value
        _apps.value = current.sortedWith(
            compareBy<AppInfo> { orderRank(it) }
                .thenBy { it.label.lowercase() }
        )
    }

    fun refresh(iconSize: Int = 128) {
        val pm = context.packageManager
        val mainIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos: List<ResolveInfo> = pm.queryIntentActivities(mainIntent, 0)
        val myPackage = context.packageName

        _apps.value = resolveInfos
            .filter { ri ->
                !(ri.activityInfo.packageName == myPackage &&
                    ri.activityInfo.name == "com.example.wlauncher.LauncherActivity")
            }
            .distinctBy { "${it.activityInfo.packageName}/${it.activityInfo.name}" }
            .map { ri ->
                val iconDrawable = ri.loadIcon(pm)
                val iconBitmap = createCircularBitmap(
                    iconDrawable.toBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888)
                )
                val blurredBitmap = createSoftenedBitmap(iconBitmap)
                AppInfo(
                    label = ri.loadLabel(pm).toString(),
                    packageName = ri.activityInfo.packageName,
                    activityName = ri.activityInfo.name,
                    icon = iconDrawable,
                    cachedIcon = iconBitmap.asImageBitmap(),
                    cachedBlurredIcon = blurredBitmap.asImageBitmap()
                )
            }
            .let { list ->
                if (customOrder.isNotEmpty()) {
                    list.sortedWith(
                        compareBy<AppInfo> { orderRank(it) }
                            .thenBy { it.label.lowercase() }
                    )
                } else {
                    list.sortedBy { it.label.lowercase() }
                }
            }
    }

    fun launchApp(appInfo: AppInfo) {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            component = ComponentName(appInfo.packageName, appInfo.activityName)
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
        try {
            context.unregisterReceiver(packageReceiver)
        } catch (_: Exception) {
        }
    }

    private fun createSoftenedBitmap(source: Bitmap): Bitmap {
        val downscaled = Bitmap.createScaledBitmap(
            source,
            (source.width * 0.25f).toInt().coerceAtLeast(1),
            (source.height * 0.25f).toInt().coerceAtLeast(1),
            true
        )
        return createCircularBitmap(
            Bitmap.createScaledBitmap(downscaled, source.width, source.height, true)
        )
    }

    private fun createCircularBitmap(source: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        val radius = minOf(source.width, source.height) / 2f
        canvas.drawCircle(source.width / 2f, source.height / 2f, radius, paint)
        return output
    }

    private fun orderRank(app: AppInfo): Int {
        if (customOrder.isEmpty()) return Int.MAX_VALUE
        val exactIndex = customOrder.indexOf(app.componentKey)
        if (exactIndex >= 0) return exactIndex
        val legacyIndex = customOrder.indexOf(app.packageName)
        if (legacyIndex >= 0) return legacyIndex
        return Int.MAX_VALUE
    }
}
