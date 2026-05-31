package com.ndds.homelauncher.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import com.ndds.homelauncher.AppInfo
import com.ndds.homelauncher.MainActivity
import com.ndds.homelauncher.R
import com.ndds.homelauncher.services.ModalService
import com.ndds.homelauncher.widgets.CustomTextView

class GridAppAdapter(
    val appContext: MainActivity,
    var apps: ArrayList<AppInfo>,
) : RecyclerView.Adapter<GridAppAdapter.ViewHolder>() {
    private var searchText: String = ""
    var filteredApps: List<AppInfo?> = arrayListOf()
    class ViewHolder(view: View, itemType: Int) :  RecyclerView.ViewHolder(view) {
        lateinit var icon: ImageView
        lateinit var name: CustomTextView
        lateinit var indicator: ImageView
        init {
            if (itemType == 0) {
                icon = view.findViewById(R.id.icon)
                name = view.findViewById(R.id.name)
                indicator = view.findViewById(R.id.indicator)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        if (viewType == 0) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.grid_item_app, parent, false)
            return ViewHolder(view,0)
        } else {
            val blankView = View(appContext)
            blankView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0
            )
            return ViewHolder(blankView,1)
        }
    }

    fun applySearch(searchText: String) {
        this.searchText = searchText
        refresh()
    }

    fun refresh() {
        val searchText = searchText.trim()
        if (searchText.isEmpty())
            filteredApps = apps
        else {
            val newData: ArrayList<AppInfo?> = arrayListOf()
            apps.forEach { original ->
                if (original.name.split(" ").any { it.startsWith(searchText, true) })
                    newData.add(original)
            }
            if (newData.isEmpty())
                apps.forEach { original ->
                    if (original.name.split(" ").any { it.contains(searchText, true) })
                        newData.add(original)
                }
            if (newData.size % 4 != 0) {
                val index = newData.size - (newData.size % 4)
                val count = 4 - (newData.size % 4)
                for (i in 0 until count) {
                    newData.add(index, null)
                }
            }
            filteredApps = newData
        }
        this.notifyDataSetChanged()
    }


    override fun getItemCount() = filteredApps.size

    override fun getItemViewType(position: Int): Int {
        return if (filteredApps[position] == null) 1 else 0
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = filteredApps[position] ?: return
        holder.icon.setImageDrawable(app.icon)
        holder.name.text = app.name
        if (app.isFresh || app.isLastUsed) {
            holder.indicator.visibility = View.VISIBLE
            holder.indicator.setImageDrawable(
                AppCompatResources.getDrawable(
                    appContext,
                    if (app.isLastUsed) R.drawable.recent else R.drawable.download_cloud
                )
            )
        } else
            holder.indicator.visibility = View.GONE

        holder.itemView.setOnClickListener {
            appContext.launchApp(app)
        }
        holder.itemView.setOnLongClickListener {
            appContext.modal.showAppDetail(app, true)
            return@setOnLongClickListener true
        }
    }

    fun launchFirstApp() {
        if (filteredApps.isNotEmpty())
            appContext.launchApp(filteredApps.first{ it != null }!!)
    }
}