package com.ndds.homelauncher

import android.content.ComponentName
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
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
        val freshAppIndicator: View = view.findViewById(R.id.freshAppIndicator)
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
        else
            filteredApps = apps.filter { original -> original.name.split(" ").any { it.startsWith(searchText, true) } }
        this.notifyDataSetChanged()
    }


    override fun getItemCount() = filteredApps.size

    private fun launchApp(app: AppInfo) {
        val intent = Intent().setComponent(ComponentName(app.packageName, app.activityName))
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(intent)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = filteredApps[position]
        holder.icon.setImageDrawable(app.icon)
        holder.name.text = app.name
        holder.freshAppIndicator.visibility = if (app.isFresh) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener {
            launchApp(app)
        }
        holder.itemView.setOnLongClickListener {
            ModalService(appContext).showAppDetail(app, true)
            return@setOnLongClickListener true
        }
    }

    fun launchFirstApp() {
        if (filteredApps.isNotEmpty())
            launchApp(filteredApps[0])
    }
}