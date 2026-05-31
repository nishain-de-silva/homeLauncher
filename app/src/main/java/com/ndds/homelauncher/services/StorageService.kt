package com.ndds.homelauncher.services

import android.content.Context
import android.content.Context.MODE_PRIVATE
import androidx.core.content.edit
import com.ndds.homelauncher.AppInfo

class StorageService(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("default", MODE_PRIVATE)
    fun getPinnedApps(): ArrayList<String> {
        val jsonAppArray = sharedPreferences.getString("appList", "")
        if (jsonAppArray!!.isEmpty()) return arrayListOf()
        return ArrayList(jsonAppArray.split("|"))
    }
    fun getWidgetList(): ArrayList<Int> {
        val stringList = sharedPreferences.getString("widgetList", "")!!
        if (stringList.isEmpty()) return arrayListOf()
        return ArrayList(stringList.split(",").map { it.toInt() })
    }
    fun updateWidgetList(newData: List<Int>) {
        sharedPreferences.edit {
            putString("widgetList",newData.joinToString(separator = ",", transform = { it.toString() } ))
        }
    }
    fun getWidgetID(): Int {
        return sharedPreferences.getInt("widgetID", -1)
    }
    fun saveWidgetID(id: Int) {
        return sharedPreferences.edit{
            putInt("widgetID",id)
        }
    }
    fun removeWidgetID() {
        return sharedPreferences.edit {
            remove("widgetID")
        }
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