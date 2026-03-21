package com.example.wlauncher.data.iconpack

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

data class IconPackInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable?
)

class IconPackManager(private val context: Context) {

    fun getInstalledIconPacks(): List<IconPackInfo> {
        val pm = context.packageManager
        val packs = mutableMapOf<String, IconPackInfo>()

        // 通过 action 扫描
        val actions = listOf(
            "org.adw.launcher.THEME",
            "com.novalauncher.THEME",
            "com.gau.go.launcherex.theme",
            "com.dlto.atom.launcher.THEME",
            "com.teslacoilsw.launcher.THEME",
            "com.fede.launcher.THEME_ICONPACK",
            "com.anddoes.launcher.THEME",
            "com.oneplus.launcher.ICONPACK",
            "com.sec.android.app.launcher.THEME",
            "com.samsung.launcher.ICONPACK",
            "ch.deletescape.lawnchair.ICONPACK",
            "app.flavor.bear.ICONPACK",
            "is.shortcut",
            "android.intent.action.MAIN"
        )

        val categories = listOf(
            "com.anddoes.launcher.THEME",
            "com.teslacoilsw.launcher.THEME",
            "com.samsung.theme.appiconpack.category.Multi",
            "android.theme.appiconpack"
        )

        for (action in actions) {
            try {
                val intent = Intent(action)
                val resolved = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)
                for (ri in resolved) {
                    val pkg = ri.activityInfo.packageName
                    // 过滤掉明显不是图标包的（需要有 appfilter.xml）
                    if (pkg !in packs && hasAppFilter(pkg)) {
                        packs[pkg] = IconPackInfo(pkg, ri.loadLabel(pm).toString(), ri.loadIcon(pm))
                    }
                }
            } catch (_: Exception) {}
        }

        for (cat in categories) {
            try {
                val intent = Intent(Intent.ACTION_MAIN).addCategory(cat)
                val resolved = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)
                for (ri in resolved) {
                    val pkg = ri.activityInfo.packageName
                    if (pkg !in packs && hasAppFilter(pkg)) {
                        packs[pkg] = IconPackInfo(pkg, ri.loadLabel(pm).toString(), ri.loadIcon(pm))
                    }
                }
            } catch (_: Exception) {}
        }

        // 兜底：扫描所有安装的 app，检查是否有 appfilter.xml
        try {
            val allApps = pm.getInstalledApplications(0)
            for (app in allApps) {
                if (app.packageName !in packs && hasAppFilter(app.packageName)) {
                    packs[app.packageName] = IconPackInfo(
                        app.packageName,
                        pm.getApplicationLabel(app).toString(),
                        pm.getApplicationIcon(app)
                    )
                }
            }
        } catch (_: Exception) {}

        return packs.values.toList().sortedBy { it.label.lowercase() }
    }

    private fun hasAppFilter(pkg: String): Boolean {
        return try {
            val ctx = context.createPackageContext(pkg, Context.CONTEXT_IGNORE_SECURITY)
            try { ctx.assets.open("appfilter.xml").close(); true } catch (_: Exception) {
                try {
                    val res = context.packageManager.getResourcesForApplication(pkg)
                    res.getIdentifier("appfilter", "xml", pkg) != 0
                } catch (_: Exception) { false }
            }
        } catch (_: Exception) { false }
    }

    fun loadAppFilter(iconPackPkg: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        try {
            val res = context.packageManager.getResourcesForApplication(iconPackPkg)

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
                val resId = res.getIdentifier("appfilter", "xml", iconPackPkg)
                if (resId != 0) parseAppFilter(res.getXml(resId), map)
            }
        } catch (_: Exception) {}
        return map
    }

    private fun parseAppFilter(parser: XmlPullParser, map: MutableMap<String, String>) {
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG && parser.name == "item") {
                val component = parser.getAttributeValue(null, "component")
                val drawable = parser.getAttributeValue(null, "drawable")
                if (component != null && drawable != null) {
                    val clean = component.removePrefix("ComponentInfo{").removeSuffix("}")
                    map[clean] = drawable
                }
            }
            event = parser.next()
        }
    }

    fun loadIconDrawable(iconPackPkg: String, drawableName: String): Drawable? {
        return try {
            val res = context.packageManager.getResourcesForApplication(iconPackPkg)
            val resId = res.getIdentifier(drawableName, "drawable", iconPackPkg)
            if (resId != 0) res.getDrawable(resId, null) else null
        } catch (_: Exception) { null }
    }
}
