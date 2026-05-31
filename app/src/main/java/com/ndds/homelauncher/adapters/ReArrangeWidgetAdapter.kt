package com.ndds.homelauncher.adapters

import android.annotation.SuppressLint
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.ndds.homelauncher.R

class ReArrangeWidgetAdapter(
    val context: Context,
    val data: ArrayList<Pair<Int, AppWidgetProviderInfo>>,
    val itemTouchHelper: ItemTouchHelper
): RecyclerView.Adapter<ReArrangeWidgetAdapter.Item>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        type: Int
    ): Item {
        return Item(
            LayoutInflater.from(parent.context).inflate(R.layout.arrange_widget_row, parent, false)
        )
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(
        item: Item,
        position: Int
    ) {
        item.label.text = data[position].second.loadLabel(context.packageManager)
        item.icon.setImageDrawable(data[position].second.loadIcon(context, 0))
        item.itemView.setOnTouchListener { view, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                itemTouchHelper.startDrag(item)
            }
            return@setOnTouchListener true
        }
    }

    override fun getItemCount(): Int = data.size

    class Item(view: View): RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.icon)
        val label: TextView = view.findViewById(R.id.name)
    }
}