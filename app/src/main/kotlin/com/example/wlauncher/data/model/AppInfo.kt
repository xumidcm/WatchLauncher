package com.example.wlauncher.data.model

import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap

data class AppInfo(
    val label: String,
    val packageName: String,
    val activityName: String,
    val installedAt: Long = 0L,
    val icon: Drawable,
    val cachedIcon: ImageBitmap
) {
    val componentKey: String
        get() = "$packageName/$activityName"
}
