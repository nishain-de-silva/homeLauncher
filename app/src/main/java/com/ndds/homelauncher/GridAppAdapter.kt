package com.ndds.homelauncher

import android.content.ComponentName
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import com.ndds.homelauncher.widgets.CustomTextView

class GridAppAdapter(
    val appContext: MainActivity,
    var apps: ArrayList<AppInfo>,
) : RecyclerView.Adapter<GridAppAdapter.ViewHolder>() {
    private var searchText: String = ""
    var filteredApps: List<AppInfo> = apps
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.icon)
        val name: CustomTextView = view.findViewById(R.id.name)
        val indicator: ImageView = view.findViewById(R.id.indicator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.grid_item_app, parent, false)
        return ViewHolder(view)
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
            filteredApps = apps.filter { original ->
                original.name.split(" ").any { it.startsWith(searchText, true) }
            }
            if (filteredApps.isEmpty())
                filteredApps = apps.filter { original ->
                    original.name.split(" ").any { it.contains(searchText, true) }
                }

        }
        this.notifyDataSetChanged()
    }


    override fun getItemCount() = filteredApps.size



    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = filteredApps[position]
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
            ModalService(appContext).showAppDetail(app, true)
            return@setOnLongClickListener true
        }
    }

    fun launchFirstApp() {
        if (filteredApps.isNotEmpty())
            appContext.launchApp(filteredApps[0])
    }
}