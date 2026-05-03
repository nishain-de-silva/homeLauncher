package com.ndds.homelauncher

import android.content.Context
import android.content.Context.MODE_PRIVATE
import androidx.core.content.edit

class StorageService(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("default", MODE_PRIVATE)
    fun getPinnedApps(): ArrayList<String> {
        val jsonAppArray = sharedPreferences.getString("appList", "")
        if (jsonAppArray!!.isEmpty()) return arrayListOf()
        return ArrayList(jsonAppArray.split("|"))
    }
    fun updatePinnedApps(updatedList: List<AppInfo>) {
        sharedPreferences.edit {
            putString("appList", updatedList.map { it.id }.joinToString("|"))
        }
    }
    private fun getAppJSONArray(): ArrayList<String> {
        val jsonAppArray = sharedPreferences.getString("appList", "")
        return ArrayList(jsonAppArray!!.split("|"))
    }
    fun saveAppToPin(app: AppInfo) {
        val appArray = getAppJSONArray()
        appArray.add(app.id)
        sharedPreferences.edit { putString("appList", appArray.joinToString("|")) }
    }

    fun removeAppPin(app: AppInfo) {
        val appArray = getAppJSONArray()
        appArray.remove(app.id)
        sharedPreferences.edit { putString("appList", appArray.joinToString("|")) }
    }
}