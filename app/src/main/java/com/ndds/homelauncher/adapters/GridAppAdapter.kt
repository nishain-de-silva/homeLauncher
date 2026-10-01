package com.ndds.homelauncher.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import com.ndds.homelauncher.AppDescriptionPattern
import com.ndds.homelauncher.models.AppInfo
import com.ndds.homelauncher.MainActivity
import com.ndds.homelauncher.R
import com.ndds.homelauncher.widgets.CustomTextView
import kotlin.math.min

class GridAppAdapter(
    val appContext: MainActivity,
    var apps: ArrayList<AppInfo>,
) : RecyclerView.Adapter<GridAppAdapter.ViewHolder>() {
    private var searchText: String = ""
    val appDescriptionPattern: AppDescriptionPattern = AppDescriptionPattern()
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
    fun isContainSearchText(searchText: String, inputText: String): Boolean {
        var matchIndex = 0
        var previousCharType = 0
        for (c in inputText) {
            if (matchIndex > 0) {
                if (c.lowercaseChar() == searchText[matchIndex]) {
                    matchIndex++
                    if (matchIndex == searchText.length)
                        return true
                } else
                    matchIndex = 0
            }
            var charType: Int
            if (c == ' ') charType = 0
            else if (c in 'a'..'z') charType = 1
            else charType = 2

            if ((charType == 1 && previousCharType != 1) || (charType == 2 && previousCharType != 2)) {
                if (matchIndex == 0) {
                    if (c.lowercaseChar() == searchText[0]) {
                        matchIndex++
                        if (matchIndex == searchText.length)
                            return true
                    }
                }
            }
            previousCharType = charType
        }
        return false
    }

    fun refresh() {
        val searchText = searchText.trim().lowercase()
        if (searchText.isEmpty())
            filteredApps = apps
        else {
            val newData: ArrayList<AppInfo?> = arrayListOf()
            apps.forEach { original ->
                if (isContainSearchText(searchText, original.name))
                    newData.add(original)
            }

            if (newData.isEmpty() && searchText.length >= 4) {
                val searchWords = searchText.split(" ")
                val scores: HashMap<String, Int> = hashMapOf()
                for (searchWord in searchWords) {
                    val match = appDescriptionPattern.getMatch(searchWord)
                    if (match != null) {
                        match.packageNames.forEach {
                            scores[it] = 1 + (scores[it] ?: 0)
                        }
                    }
                }
                if (!scores.isEmpty()) {
                    var max = -1
                    var maxScorePackage = ""
                    scores.forEach {
                        if (it.value > max) {
                            max = it.value
                            maxScorePackage = it.key
                        }
                    }
                    newData.addAll(apps.filter({ scores.containsKey(it.packageName) }))
                }
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