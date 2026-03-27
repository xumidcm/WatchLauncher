package com.example.wlauncher.data.repository

import android.app.ActivityOptions
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import com.example.wlauncher.CrashActivity
import com.example.wlauncher.data.model.AppInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val DEFAULT_ICON_CACHE_SIZE = 224

class AppRepository(private val context: Context) {

    private val _apps = MutableStateFlow<List<AppInfo>>(emptyList())
    val apps: StateFlow<List<AppInfo>> = _apps.asStateFlow()

    private var customOrder: List<String> = emptyList()
    private var hiddenComponents: Set<String> = emptySet()

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

    fun setHiddenComponents(hidden: Set<String>) {
        hiddenComponents = hidden
        reorder()
    }

    fun refresh(iconSize: Int = DEFAULT_ICON_CACHE_SIZE) {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(launcherIntent, 0)
        val myPackage = context.packageName
        val targetIconSize = iconSize.coerceAtLeast(96)

        _apps.value = resolveInfos
            .filterNot { it.activityInfo.packageName == myPackage && it.activityInfo.name == "com.example.wlauncher.LauncherActivity" }
            .distinctBy { "${it.activityInfo.packageName}/${it.activityInfo.name}" }
            .map { resolveInfo ->
                val iconDrawable = resolveInfo.loadIcon(pm)
                val cachedBitmap = createCircularBitmap(
                    iconDrawable.toBitmap(targetIconSize, targetIconSize, Bitmap.Config.ARGB_8888)
                )
                AppInfo(
                    label = resolveInfo.loadLabel(pm).toString(),
                    packageName = resolveInfo.activityInfo.packageName,
                    activityName = resolveInfo.activityInfo.name,
                    installedAt = resolveInstalledAt(pm, resolveInfo.activityInfo.packageName),
                    icon = iconDrawable,
                    cachedIcon = cachedBitmap.asImageBitmap(),
                    cachedBlurredIcon = createSoftenedBitmap(cachedBitmap).asImageBitmap()
                )
            }
            .filterNot { hiddenComponents.contains(it.componentKey) }
            .let(::applyOrdering)
    }

    fun launchApp(appInfo: AppInfo): Boolean {
        val pm = context.packageManager
        val explicitIntent = buildExplicitLaunchIntent(appInfo)
        val fallbackIntent = pm.getLaunchIntentForPackage(appInfo.packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val targetIntent = when {
            canLaunch(pm, explicitIntent) -> explicitIntent
            fallbackIntent != null && canLaunch(pm, fallbackIntent) -> fallbackIntent
            else -> null
        }

        if (targetIntent == null) {
            showLaunchError(
                appInfo = appInfo,
                detail = "未找到可启动入口，或系统返回了无效的桌面入口。"
            )
            return false
        }

        return try {
            val options = ActivityOptions.makeCustomAnimation(
                context,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            )
            context.startActivity(targetIntent, options.toBundle())
            true
        } catch (error: SecurityException) {
            showLaunchError(appInfo, error.stackTraceToString())
            false
        } catch (error: ActivityNotFoundException) {
            showLaunchError(appInfo, error.stackTraceToString())
            false
        } catch (error: RuntimeException) {
            showLaunchError(appInfo, error.stackTraceToString())
            false
        }
    }

    fun destroy() {
        try {
            context.unregisterReceiver(packageReceiver)
        } catch (_: Exception) {
        }
    }

    private fun applyOrdering(apps: List<AppInfo>): List<AppInfo> {
        if (customOrder.isNotEmpty()) {
            return apps.sortedWith(
                compareBy<AppInfo> { orderRank(it) }
                    .thenBy { it.label.lowercase() }
            )
        }
        return apps.sortedWith(
            compareBy<AppInfo> { it.installedAt }
                .thenBy { it.label.lowercase() }
        )
    }

    private fun reorder() {
        _apps.value = applyOrdering(_apps.value.filterNot { hiddenComponents.contains(it.componentKey) })
    }

    private fun buildExplicitLaunchIntent(appInfo: AppInfo): Intent {
        return Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
            component = ComponentName(appInfo.packageName, appInfo.activityName)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    private fun canLaunch(packageManager: PackageManager, intent: Intent): Boolean {
        val resolved = packageManager.resolveActivity(intent, 0) ?: return false
        val activityInfo = resolved.activityInfo ?: return false
        if (!activityInfo.enabled) return false
        if (!activityInfo.exported) return false
        return true
    }

    private fun resolveInstalledAt(packageManager: PackageManager, packageName: String): Long {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0L)).firstInstallTime
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0).firstInstallTime
            }
        } catch (_: Exception) {
            0L
        }
    }

    private fun showLaunchError(appInfo: AppInfo, detail: String) {
        val errorIntent = CrashActivity.createErrorIntent(
            context = context,
            title = "应用打开失败",
            summary = appInfo.label,
            detail = detail,
            packageName = appInfo.packageName
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val launchedErrorPage = runCatching {
            context.startActivity(errorIntent)
            true
        }.getOrDefault(false)

        if (!launchedErrorPage) {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${appInfo.packageName}")).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }
        }
    }

    private fun createSoftenedBitmap(source: Bitmap): Bitmap {
        val downscaled = Bitmap.createScaledBitmap(
            source,
            (source.width * 0.4f).toInt().coerceAtLeast(1),
            (source.height * 0.4f).toInt().coerceAtLeast(1),
            true
        )
        return createCircularBitmap(
            Bitmap.createScaledBitmap(downscaled, source.width, source.height, true)
        )
    }

    private fun createCircularBitmap(source: Bitmap): Bitmap {
        val output = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG).apply {
            isAntiAlias = true
            isFilterBitmap = true
            isDither = true
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
