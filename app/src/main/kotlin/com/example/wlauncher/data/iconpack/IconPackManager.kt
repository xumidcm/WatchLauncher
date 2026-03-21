package com.example.wlauncher.data.iconpack

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory

data class IconPackInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable?
)

class IconPackManager(private val context: Context) {

    /**
     * 扫描已安装的图标包（兼容 ADW/Nova/Samsung/Launcher3 规范）
     */
    fun getInstalledIconPacks(): List<IconPackInfo> {
        val pm = context.packageManager
        val packs = mutableMapOf<String, IconPackInfo>()

        val themes = listOf(
            "org.adw.launcher.THEME",
            "com.novalauncher.THEME",
            "com.gau.go.launcherex.theme",
            "com.dlto.atom.launcher.THEME"
        )

        for (action in themes) {
            val intent = Intent(action)
            val resolved = pm.queryIntentActivities(intent, PackageManager.GET_META_DATA)
            for (ri in resolved) {
                val pkg = ri.activityInfo.packageName
                if (pkg !in packs) {
                    packs[pkg] = IconPackInfo(
                        packageName = pkg,
                        label = ri.loadLabel(pm).toString(),
                        icon = ri.loadIcon(pm)
                    )
                }
            }
        }
        return packs.values.toList().sortedBy { it.label.lowercase() }
    }

    /**
     * 从图标包中加载 appfilter.xml，返回 ComponentName -> drawableName 映射
     */
    fun loadAppFilter(iconPackPkg: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        try {
            val pm = context.packageManager
            val res = pm.getResourcesForApplication(iconPackPkg)

            // 尝试从 assets 读取 appfilter.xml
            val assetStream = try {
                val assetManager = context.createPackageContext(
                    iconPackPkg, Context.CONTEXT_IGNORE_SECURITY
                ).assets
                assetManager.open("appfilter.xml")
            } catch (_: Exception) { null }

            if (assetStream != null) {
                val factory = XmlPullParserFactory.newInstance()
                val parser = factory.newPullParser()
                parser.setInput(assetStream, "UTF-8")
                parseAppFilter(parser, map)
                assetStream.close()
            } else {
                // 尝试从 xml 资源读取
                val resId = res.getIdentifier("appfilter", "xml", iconPackPkg)
                if (resId != 0) {
                    val parser = res.getXml(resId)
                    parseAppFilter(parser, map)
                }
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
                    // component 格式: ComponentInfo{pkg/cls} 或 pkg/cls
                    val clean = component
                        .removePrefix("ComponentInfo{")
                        .removeSuffix("}")
                    map[clean] = drawable
                }
            }
            event = parser.next()
        }
    }

    /**
     * 从图标包加载指定 drawable
     */
    fun loadIconDrawable(iconPackPkg: String, drawableName: String): Drawable? {
        return try {
            val res = context.packageManager.getResourcesForApplication(iconPackPkg)
            val resId = res.getIdentifier(drawableName, "drawable", iconPackPkg)
            if (resId != 0) res.getDrawable(resId, null) else null
        } catch (_: Exception) { null }
    }
}
