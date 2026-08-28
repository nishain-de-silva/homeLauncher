package com.ndds.homelauncher.services

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.util.Log
import androidx.core.content.edit
import com.ndds.homelauncher.models.AppInfo
import org.json.JSONArray
import org.json.JSONObject

class StorageService(context: Context) {
    private val sharedPreferences = context.getSharedPreferences("default", MODE_PRIVATE)
    fun getPinnedApps(): ArrayList<String> {
        val jsonAppArray = sharedPreferences.getString("appList", "")
        if (jsonAppArray!!.isEmpty()) return arrayListOf()
        return ArrayList(jsonAppArray.split("|"))
    }
    fun getAppDescriptions(): HashMap<String, JSONArray> {
        val jsonMapString = sharedPreferences.getString("appDescriptionDB", null)
        val map: HashMap<String, JSONArray> = hashMapOf()
        if (jsonMapString == null) return map
        val jsonObject = JSONObject(jsonMapString)
        val packageNames = jsonObject.keys()
        for (packageName in packageNames) {
            map[packageName] = JSONArray(jsonObject.getString(packageName))
        }
        return map
    }

    fun addToAppDescription(packageName: String, words: JSONArray) {
        val jsonMapString = sharedPreferences.getString("appDescriptionDB", null) ?: return
        val jsonObject = JSONObject(jsonMapString)

        jsonObject.put(packageName, words)
        sharedPreferences.edit {
            putString("appDescriptionDB", jsonObject.toString())
        }
    }

    fun removeFromAppDescription(packageName: String) {
        val jsonMapString = sharedPreferences.getString("appDescriptionDB", null) ?: return
        val jsonObject = JSONObject(jsonMapString)
        jsonObject.remove(packageName)
        sharedPreferences.edit {
            putString("appDescriptionDB", jsonObject.toString())
        }
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

    fun recordLastUsedApp(id: String) {
        sharedPreferences.edit { putString("lastUsedApp", id) }
    }

    fun getLastUsedAppID(): String? {
        return sharedPreferences.getString("lastUsedApp", null)
    }
}