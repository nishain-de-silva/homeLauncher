package com.ndds.homelauncher.adapters

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.ndds.homelauncher.MainActivity
import com.ndds.homelauncher.utils.RecyclerViewOnTouchListener
import com.ndds.homelauncher.widgets.WidgetContainer

class HomeWidgetListAdapter(val appContext: MainActivity,
                            val appWidgetHost: AppWidgetHost,
                            val data: ArrayList<Int>,
                            val parent: View,
                            val onLongPress: (position: Int, HomeWidgetListAdapter) -> Unit,
                            val onTouchListener: RecyclerViewOnTouchListener
): RecyclerView.Adapter<HomeWidgetListAdapter.Item>(){
    class Item(view: View): RecyclerView.ViewHolder(view) {

    }
    val appWidgetManager = AppWidgetManager.getInstance(appContext)
    fun addItem(newValue: Int) {
        data.add(newValue)
        notifyItemInserted(data.size)
    }

    override fun onBindViewHolder(item: Item, position: Int) {
        (item.itemView as WidgetContainer)
            .onTouchInterceptListener = object : View.OnTouchListener {
            var isLongPress = false
            override fun onTouch(
                p0: View?,
                event: MotionEvent
            ): Boolean {
                if (event.action == MotionEvent.ACTION_DOWN) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        isLongPress = true
                        onLongPress(position, this@HomeWidgetListAdapter)
                    }, 500)
                    isLongPress = false
                } else if (event.action == MotionEvent.ACTION_UP) {
                    if (isLongPress)
                        return true
                }
                return false
            }

        }
        item.itemView.setOnTouchListener(object : View.OnTouchListener {
            override fun onTouch(
                view: View,
                event: MotionEvent
            ): Boolean {
                return true
//                return onTouchListener.onTouch(event, item)
            }
        })
    }

    override fun getItemCount(): Int = data.size
    override fun getItemViewType(position: Int): Int {
        return data[position]
    }

    override fun onCreateViewHolder(p0: ViewGroup, type: Int): Item {
        val id = type
        val widgetInfo = appWidgetManager.getAppWidgetInfo(id)
        val hostView = appWidgetHost.createView(
            appContext.applicationContext,
            id,
            widgetInfo
        )
        hostView.setAppWidget(id, widgetInfo)
        val widgetWidth = parent.width - parent.paddingLeft - parent.paddingRight
        val widgetHeight = (widgetWidth * (widgetInfo.minHeight.toFloat() / widgetInfo.minWidth.toFloat())).toInt()
        val containerView = WidgetContainer(appContext)
        containerView.layoutParams = LinearLayout.LayoutParams(
            widgetWidth,
            widgetHeight
        )
        hostView.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        containerView.addView(hostView)
        return Item(containerView)
    }

    fun remove(position: Int) {
        data.removeAt(index = position)
        notifyItemRemoved(position)
    }
}