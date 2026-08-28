package com.ndds.homelauncher

import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.ndds.homelauncher.models.AppInfoBackupData
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import androidx.core.content.edit
import com.ndds.homelauncher.services.WordsTagsExtractor
import com.ndds.homelauncher.utils.EdgeDetector

class SettingsActivity: AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.settings_layout)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rootView)) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
                        or WindowInsetsCompat.Type.displayCutout()
                        or WindowInsetsCompat.Type.ime()
            )
            v.updatePadding(top = bars.top, bottom = bars.bottom)
            WindowInsetsCompat.CONSUMED
        }
        val readDocumentLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument(), {
            uri ->
            if (uri == null)
                return@registerForActivityResult
            val inputStream = contentResolver.openInputStream(uri)!!.reader()
            val jsonString = inputStream.readText()
            inputStream.close()
            try {
                val jsonObject = JSONObject(jsonString)
                val pinnedApps =  jsonObject.getJSONArray("pinnedApps")
                val resolvedData: ArrayList<AppInfoBackupData> = arrayListOf()
                for (i in 0 until pinnedApps.length()) {
                    val appInfo = pinnedApps.getJSONObject(i)
                    resolvedData.add(AppInfoBackupData(
                        appInfo.getString("name"),
                        appInfo.getString("packageName"),
                        appInfo.getString("activityName")
                    ))
                }
                setResult(RESULT_OK, Intent().putExtra("pinnedApps", resolvedData))
                Toast.makeText(this, "App restored successfully", Toast.LENGTH_SHORT).show()
            } catch (_: JSONException) {
                Toast.makeText(this, "Invalid backup file selected", Toast.LENGTH_SHORT).show()
            }
        })
        val createDocumentLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json"), {
            uri ->
                if (uri == null) return@registerForActivityResult
                val outputStream = contentResolver.openOutputStream(uri)!!
                val pinnedApps: ArrayList<AppInfoBackupData> = intent.getParcelableArrayListExtra("pinnedApps")!!
                val jsonObject = JSONObject()
                jsonObject.put("pinnedApps", JSONArray().apply {
                    pinnedApps.forEach {
                        put(it.toJSON())
                    }
                })
                outputStream.write(jsonObject.toString().toByteArray())
                outputStream.flush()
                outputStream.close()
        })
        findViewById<View>(R.id.backup).setOnClickListener {
            createDocumentLauncher.launch("backup.json")
        }
        findViewById<View>(R.id.restore).setOnClickListener {
            readDocumentLauncher.launch(arrayOf("application/json"))
        }
        findViewById<View>(R.id.updateAppDescDB).setOnClickListener {
            extractAllAppDescriptions()
        }
        findViewById<View>(R.id.updateAppIcons).setOnClickListener {
            extractAllAppIcons()
            Toast.makeText(this, "Completed", Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK, Intent().putExtra("iconsUpdated", true))
        }
    }



    fun extractDescriptionFromApp(index: Int, appData: List<ResolveInfo>, descriptions: HashMap<String, JSONArray>) {
        if (index >= appData.size) {
            val sharedPreferences = getSharedPreferences("default", MODE_PRIVATE)
            val jsonObject = JSONObject()
            descriptions.forEach {
                jsonObject.put(it.key, it.value)
            }
            sharedPreferences.edit { putString("appDescriptionDB", jsonObject.toString()) }
            findViewById<ProgressBar>(R.id.descr_download_progress).visibility = View.GONE
            Toast.makeText(this, "Done", Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK, Intent().putExtra("longDescriptionMapping", descriptions))
            return
        }
        val packageName = appData[index].activityInfo.applicationInfo.packageName
        WordsTagsExtractor().extractTags(packageName, {
            extractedWords ->
                if (extractedWords == null)
                    extractDescriptionFromApp(index + 1, appData, descriptions)
                else {
                    descriptions[packageName] = extractedWords
                    findViewById<ProgressBar>(R.id.descr_download_progress).progress = index + 1
                    extractDescriptionFromApp(index + 1, appData, descriptions)
                }
        })
    }
    fun extractAllAppDescriptions() {
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        val pm = packageManager
        val resolveInfoList = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        Log.d("info", "starting to extract descriptions")
        val progressBar = findViewById<ProgressBar>(R.id.descr_download_progress)
        progressBar.max = resolveInfoList.size
        progressBar.progress = 0
        progressBar.visibility = View.VISIBLE
        extractDescriptionFromApp(0, resolveInfoList, HashMap())
    }
    fun extractAllAppIcons() {
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        val pm = packageManager
        val resolveInfoList = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        for (resolveInfo in resolveInfoList) {
            val packageName = resolveInfo.activityInfo.applicationInfo.packageName
            val icon = resolveInfo.loadIcon(pm)
            val bitmap = EdgeDetector().evaluate(icon, Color.WHITE)
            val id = "${resolveInfo.loadLabel(pm)}$packageName"
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, openFileOutput("$id.png", MODE_PRIVATE))
        }
    }
}