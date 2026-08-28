package com.ndds.homelauncher.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.ndds.homelauncher.models.AppInfo
import com.ndds.homelauncher.DesktopSection
import com.ndds.homelauncher.MainActivity
import com.ndds.homelauncher.R
import com.ndds.homelauncher.services.ModalService
import com.ndds.homelauncher.widgets.CustomTextView
import kotlin.math.abs

class PinnedAppAdapter(
    val appContext: MainActivity,
    val desktop: DesktopSection,
    var appList: ArrayList<AppInfo>,
    val itemTouchHelper: ItemTouchHelper
): RecyclerView.Adapter<PinnedAppAdapter.Item>() {
    class Item(view: View): RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.icon)
        val name: CustomTextView = view.findViewById(R.id.name)
        val dragHandle: View = view.findViewById(R.id.dragHandle)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onBindViewHolder(holder: Item, position: Int) {
        val app = appList[position]
        holder.icon.setImageBitmap(app.icon)
        holder.name.text = app.name
        holder.dragHandle.visibility = if (desktop.isEditMode) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener {
            if (!desktop.isEditMode)
                appContext.launchApp(app)
        }
        holder.itemView.setOnTouchListener(object : View.OnTouchListener {
            var downX = 0f
            var downY = 0f
            var hasDragStarted = false;
            val isEditMode = desktop.isEditMode
            override fun onTouch(
                view: View,
                event: MotionEvent
            ): Boolean {
                if (event.action == MotionEvent.ACTION_DOWN) {
                    downX = event.x
                    downY = event.y
                    hasDragStarted = false
                    return isEditMode
                } else if (event.action == MotionEvent.ACTION_MOVE) {
                    if (abs(downY - event.y) < 10 && Math.abs(downX - event.x) < 10)
                        return isEditMode
                    if (Math.abs(downY - event.y) > Math.abs(downX - event.x)) {
                        if (desktop.isEditMode && !hasDragStarted) {
                            hasDragStarted = true
                            itemTouchHelper.startDrag(holder)
                        }
                    }
                }
                return isEditMode
            }
        })
        holder.itemView.setOnLongClickListener {
            if (desktop.isEditMode) return@setOnLongClickListener false
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