package com.example.wlauncher.data.model

import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap

data class AppInfo(
    val label: String,
    val packageName: String,
    val activityName: String,
    val icon: Drawable,
    val cachedIcon: ImageBitmap,
    val cachedBlurredIcon: ImageBitmap
)
