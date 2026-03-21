package com.example.wlauncher.data.iconpack

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.util.Log
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

data class IconPackInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable?
)

class IconPackManager(private val context: Context) {

    companion object {
        private const val TAG = "IconPackManager"
    }

    fun getInstalledIconPacks(): List<IconPackInfo> {
        val pm = context.packageManager
        val packs = mutableMapOf<String, IconPackInfo>()

        // 所有已知的图标包 intent action
        val actions = listOf(
            // Launcher3 标准
            "com.android.launcher3.action.CHANGE_ICON_PACK",
            // ADW
            "org.adw.launcher.THEME",
            "org.adw.launcher.THEMES",
            "org.adw.launcher.icons.ACTION_PICK_ICON",
            // Nova
            "com.novalauncher.THEME",
            // Go Launcher
            "com.gau.go.launcherex.theme",
            // Atom
            "com.dlto.atom.launcher.THEME",
            // TeslaCoil / Nova
            "com.teslacoilsw.launcher.THEME",
            // Fede
            "com.fede.launcher.THEME_ICONPACK",
            // Apex
            "com.anddoes.launcher.THEME",
            // OnePlus
            "com.oneplus.launcher.ICONPACK",
            "net.oneplus.launcher.icons.ACTION_PICK_ICON",
            // Samsung
            "com.sec.android.app.launcher.THEME",
            "com.samsung.launcher.ICONPACK",
            // Lawnchair
            "ch.deletescape.lawnchair.ICONPACK",
            "ch.deletescape.lawnchair.PICK_ICON",
            // Turbo
            "com.phonemetra.turbo.launcher.THEMES",
            "com.phonemetra.turbo.launcher.icons.ACTION_PICK_ICON",
            // Others
            "app.flavor.bear.ICONPACK",
            "com.motorola.launcher.ACTION_ICON_PACK",
            "com.gridappsinc.launcher.theme.apk_action",
            "com.lge.launcher2.THEME",
            "com.android.dxtop.launcher.THEME",
            "ginlemon.smartlauncher.THEMES",
            "ginlemon.smartlauncher.BUBBLEICONS",
            "home.solo.launcher.free.THEMES",
            "home.solo.launcher.free.ACTION_ICON",
            "com.gtp.nextlauncher.theme",
            "com.tsf.shell.themes",
            "com.zeroteam.zerolauncher.theme",
            "mobi.bbase.ahome.THEME",
            "com.rogro.GDE.THEME.1",
            "com.sonymobile.home.ICON_PACK",
            "com.daeva112.manager.THEME",
        )

        val categories = listOf(
            "com.anddoes.launcher.THEME",
            "com.teslacoilsw.launcher.THEME",
            "com.novalauncher.category.CUSTOM_ICON_PICKER",
            "com.samsung.theme.appiconpack.category.Multi",
            "android.theme.appiconpack",
            "android.intent.category.THEME_SCENE",
        )

        // 步骤 1: 通过 action 扫描
        for (action in actions) {
            try {
                val intent = Intent(action)
                val resolved = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)
                for (ri in resolved) {
                    val pkg = ri.activityInfo.packageName
                    if (pkg !in packs && hasAppFilter(pkg)) {
                        packs[pkg] = IconPackInfo(pkg, ri.loadLabel(pm).toString(), ri.loadIcon(pm))
                        Log.d(TAG, "Found icon pack via action '$action': $pkg")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error scanning action '$action': ${e.message}")
            }
        }

        // 步骤 2: 通过 category 扫描
        for (cat in categories) {
            try {
                val intent = Intent(Intent.ACTION_MAIN).addCategory(cat)
                val resolved = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)
                for (ri in resolved) {
                    val pkg = ri.activityInfo.packageName
                    if (pkg !in packs && hasAppFilter(pkg)) {
                        packs[pkg] = IconPackInfo(pkg, ri.loadLabel(pm).toString(), ri.loadIcon(pm))
                        Log.d(TAG, "Found icon pack via category '$cat': $pkg")
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error scanning category '$cat': ${e.message}")
            }
        }

        // 步骤 3: 通过 LAUNCHER category 扫描所有桌面可见应用，检查 appfilter
        try {
            val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val allLauncher = pm.queryIntentActivities(launcherIntent, 0)
            for (ri in allLauncher) {
                val pkg = ri.activityInfo.packageName
                if (pkg !in packs && hasAppFilter(pkg)) {
                    packs[pkg] = IconPackInfo(pkg, ri.loadLabel(pm).toString(), ri.loadIcon(pm))
                    Log.d(TAG, "Found icon pack via LAUNCHER scan: $pkg")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error scanning LAUNCHER category: ${e.message}")
        }

        // 步骤 4: 兜底 — 扫描所有已安装的 app
        try {
            val allApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            for (app in allApps) {
                if (app.packageName !in packs && hasAppFilter(app.packageName)) {
                    packs[app.packageName] = IconPackInfo(
                        app.packageName,
                        pm.getApplicationLabel(app).toString(),
                        pm.getApplicationIcon(app)
                    )
                    Log.d(TAG, "Found icon pack via fallback scan: ${app.packageName}")
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error in fallback scan: ${e.message}")
        }

        Log.i(TAG, "Total icon packs found: ${packs.size}")
        return packs.values.toList().sortedBy { it.label.lowercase() }
    }

    private fun hasAppFilter(pkg: String): Boolean {
        // 检查方式 1: assets/appfilter.xml
        try {
            val ctx = context.createPackageContext(pkg, Context.CONTEXT_IGNORE_SECURITY)
            try {
                ctx.assets.open("appfilter.xml").close()
                return true
            } catch (_: Exception) {}
        } catch (_: Exception) {}

        // 检查方式 2: res/xml/appfilter
        try {
            val res = context.packageManager.getResourcesForApplication(pkg)
            if (res.getIdentifier("appfilter", "xml", pkg) != 0) return true
        } catch (_: Exception) {}

        return false
    }

    fun loadAppFilter(iconPackPkg: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        try {
            // 优先从 assets 加载
            val assetStream = try {
                context.createPackageContext(iconPackPkg, Context.CONTEXT_IGNORE_SECURITY)
                    .assets.open("appfilter.xml")
            } catch (_: Exception) { null }

            if (assetStream != null) {
                val parser = XmlPullParserFactory.newInstance().newPullParser()
                parser.setInput(assetStream, "UTF-8")
                parseAppFilter(parser, map)
                assetStream.close()
            } else {
                // 从 res/xml 加载
                val res = context.packageManager.getResourcesForApplication(iconPackPkg)
                val resId = res.getIdentifier("appfilter", "xml", iconPackPkg)
                if (resId != 0) parseAppFilter(res.getXml(resId), map)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error loading appfilter for $iconPackPkg: ${e.message}")
        }
        Log.d(TAG, "Loaded ${map.size} icon mappings from $iconPackPkg")
        return map
    }

    private fun parseAppFilter(parser: XmlPullParser, map: MutableMap<String, String>) {
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "item") {
                val component = parser.getAttributeValue(null, "component")
                val drawable = parser.getAttributeValue(null, "drawable")
                if (component != null && drawable != null && component.contains("/")) {
                    val clean = component
                        .removePrefix("ComponentInfo{")
                        .removeSuffix("}")
                    if (clean.contains("/")) {
                        map[clean] = drawable
                    }
                }
            }
            event = parser.next()
        }
    }

    fun loadIconDrawable(iconPackPkg: String, drawableName: String): Drawable? {
        return try {
            val res = context.packageManager.getResourcesForApplication(iconPackPkg)
            val resId = res.getIdentifier(drawableName, "drawable", iconPackPkg)
            if (resId != 0) {
                @Suppress("DEPRECATION")
                res.getDrawable(resId, null)
            } else null
        } catch (_: Exception) { null }
    }
}
