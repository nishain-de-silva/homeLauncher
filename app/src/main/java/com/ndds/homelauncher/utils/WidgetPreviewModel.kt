package com.ndds.homelauncher.utils

import android.appwidget.AppWidgetProviderInfo
import android.graphics.drawable.Drawable

data class WidgetPreviewModel(val name: CharSequence, val packageName: String, val icon: Drawable, val widgets: ArrayList<AppWidgetProviderInfo>) {
    var expanded = false
}