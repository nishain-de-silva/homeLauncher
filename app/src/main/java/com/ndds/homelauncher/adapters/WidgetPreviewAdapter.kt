package com.ndds.homelauncher.adapters

import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.ndds.homelauncher.R

class WidgetPreviewAdapter(val appContext: Context,
                           val onPress: (item: AppWidgetProviderInfo) -> Unit,
                           val data: List<AppWidgetProviderInfo>
): ArrayAdapter<AppWidgetProviderInfo>(appContext,0,data) {
    val packageManager = appContext.packageManager
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var row = (convertView ?: LayoutInflater.from(appContext).inflate(R.layout.widget_row, parent, false)) as ViewGroup

        val widgetInfo = data[position]
        row.findViewById<TextView>(R.id.widget_description).text = widgetInfo.loadLabel(packageManager)
        row.findViewById<ImageView>(R.id.widget_app_icon).setImageDrawable(widgetInfo.loadIcon(appContext, 0))
        row.findViewById<ImageView>(R.id.widget_preview).setImageDrawable(widgetInfo.loadPreviewImage(appContext, 0))
        row.setOnClickListener {
            onPress(widgetInfo)
        }
        return row
    }
}