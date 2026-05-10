package com.ndds.homelauncher.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.ndds.homelauncher.AppInfo
import com.ndds.homelauncher.MainActivity
import com.ndds.homelauncher.R
import com.ndds.homelauncher.services.ModalService
import com.ndds.homelauncher.widgets.CustomTextView

class PinnedAppAdapter(
    val appContext: MainActivity,
    var appList: ArrayList<AppInfo>,
    val itemTouchHelper: ItemTouchHelper
): RecyclerView.Adapter<PinnedAppAdapter.Item>() {
    var isEditMode = false
    class Item(view: View): RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.icon)
        val name: CustomTextView = view.findViewById(R.id.name)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: Item, position: Int) {
        val app = appList[position]
        holder.icon.setImageDrawable(app.icon)
        holder.name.text = app.name

        holder.itemView.setOnClickListener {
            if (!isEditMode)
                appContext.launchApp(app)
        }
        holder.itemView.setOnTouchListener(object : View.OnTouchListener {
            var downX = 0f
            var downY = 0f
            override fun onTouch(
                view: View?,
                event: MotionEvent?
            ): Boolean {
                if (event?.action == MotionEvent.ACTION_DOWN) {
                    downX = event.x
                    downY = event.y
                } else if (event?.action == MotionEvent.ACTION_MOVE) {
                    if (isEditMode && Math.abs(downY - event.y) > Math.abs(downX - event.x))
                        itemTouchHelper.startDrag(holder)
                }
                return false
            }
        })
        holder.itemView.setOnLongClickListener {
            if (isEditMode) return@setOnLongClickListener false
            ModalService(appContext).showAppDetail(app, false)
            return@setOnLongClickListener true
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, p1: Int): Item {
        return Item(
            LayoutInflater.from(parent.context).inflate(R.layout.row_item_app, parent, false)
        )
    }
    override fun getItemCount() = appList.size

    fun addApp(app: AppInfo) {
        appList.add(app)
        notifyItemInserted(appList.size)
    }
    fun removeApp(app: AppInfo): Boolean {
        val index = appList.indexOf(app)
        if (index != -1) {
            appList.removeAt(index)
            notifyItemRemoved(index)
            return true
        }
        return false
    }
    fun removeApp(packageName: CharSequence): Boolean {
        val isRemoved = appList.removeAll { it.packageName == packageName }
        if (isRemoved) {
            notifyDataSetChanged()
        }
        return isRemoved
    }
}