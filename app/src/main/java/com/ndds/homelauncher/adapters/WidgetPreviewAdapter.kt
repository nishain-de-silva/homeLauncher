package com.ndds.homelauncher.adapters

import android.annotation.SuppressLint
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.ndds.homelauncher.R
import com.ndds.homelauncher.utils.WidgetPreviewModel
import kotlin.text.isNotEmpty

class WidgetPreviewAdapter(val appContext: Context,
                           val onPress: (item: AppWidgetProviderInfo) -> Unit,
                           val data: List<WidgetPreviewModel>
): ArrayAdapter<WidgetPreviewModel>(appContext,0,data) {

    @SuppressLint("ResourceType")
    private fun loadWidgetsInGroup(widgetInfo: WidgetPreviewModel, widgetContainer: ViewGroup) {
        val widgetContext = context.createPackageContext(
            widgetInfo.packageName,
            0
        );
        for (widget in widgetInfo.widgets) {
            val widgetRow = LayoutInflater.from(appContext).inflate(R.layout.widget_preview_item, null)
            val widgetPreviewImage = widgetRow.findViewById<ImageView>(R.id.widget_preview_image)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && widget.previewLayout != 0) {
                widgetRow.findViewById<ViewGroup>(R.id.widget_preview_container)
                    .addView(LayoutInflater.from(
                        widgetContext
                    ).inflate(widget.previewLayout, null))
                widgetPreviewImage.visibility = View.GONE
            } else {
                widgetPreviewImage.setImageDrawable(widget.loadPreviewImage(context, 0))
            }

            widgetRow.findViewById<TextView>(R.id.widget_description).let {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    var title = widget.loadLabel(context.packageManager)
                    val description = widget.loadDescription(context)
                    if (!description.isNullOrEmpty())
                        title = "$title - $description"
                    if (title.isNotEmpty()) {
                        it.text = title
                        return@let
                    }
                }
                it.visibility = View.GONE
            }

            widgetRow.setOnClickListener {
                onPress(widget)
            }
            widgetContainer.addView(widgetRow)
        }
    }
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var row = (convertView ?: LayoutInflater.from(appContext).inflate(R.layout.widget_group_row, parent, false)) as ViewGroup

        val widgetInfo = data[position]
        val widgetContainer = row.findViewById<ViewGroup>(R.id.widget_container_per_app)
        widgetContainer.visibility = if (widgetInfo.expanded) View.VISIBLE else View.GONE
        if (widgetInfo.expanded) {
            widgetContainer.removeAllViews()
            loadWidgetsInGroup(widgetInfo, widgetContainer)
        }
        row.setOnClickListener {
            widgetInfo.expanded = !widgetInfo.expanded
            if (widgetInfo.expanded) {
                widgetContainer.removeAllViews()
                loadWidgetsInGroup(widgetInfo, widgetContainer)
                widgetContainer.visibility = View.VISIBLE
            } else {
                widgetContainer.visibility = View.GONE
            }
        }


        row.findViewById<TextView>(R.id.appName).text = widgetInfo.name
        row.findViewById<ImageView>(R.id.widget_app_icon).setImageDrawable(widgetInfo.icon)

        return row
    }
}