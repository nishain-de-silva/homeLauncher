package com.ndds.homelauncher

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.os.PersistableBundle
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

class WidgetActivity: AppCompatActivity() {
    val APPWIDGET_HOST_ID = 1024
    val REQUEST_PICK_WIDGET = 44
    val REQUEST_CONFIG_WIDGET = 55
    lateinit var appWidgetManager: AppWidgetManager
    lateinit var containerLayout: ViewGroup
    lateinit var appWidgetHost: AppWidgetHost
    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)
        setContentView(R.layout.widget_layout)

        appWidgetManager = AppWidgetManager.getInstance(this)
        appWidgetHost = AppWidgetHost(this, APPWIDGET_HOST_ID)
        containerLayout = findViewById<ViewGroup>(R.id.widget_container)
        appWidgetHost.startListening()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.setOnApplyWindowInsetsListener(containerLayout) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
                        or WindowInsetsCompat.Type.displayCutout()
                        or WindowInsetsCompat.Type.ime()
            )
            v.updatePadding(bottom = bars.bottom, top = bars.top)
            WindowInsetsCompat.CONSUMED
        }
        findViewById<View>(R.id.add_widget).setOnClickListener {
            chooseWidgetToAdd()
        }
    }

    fun chooseWidgetToAdd() {
        val appWidgetId = appWidgetHost.allocateAppWidgetId()

        val pickIntent = Intent(AppWidgetManager.ACTION_APPWIDGET_PICK).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }

        startActivityForResult(pickIntent, REQUEST_PICK_WIDGET)
    }

    override fun onDestroy() {
        super.onDestroy()
        appWidgetHost.stopListening()
    }

    fun configureWidget(appWidgetId: Int) {
        val appWidgetInfo = appWidgetManager.getAppWidgetInfo(appWidgetId)

        if (appWidgetInfo.configure != null) {
            val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE).apply {
                component = appWidgetInfo.configure
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            }
            startActivityForResult(intent, REQUEST_CONFIG_WIDGET)
        } else {
            addWidgetToScreen(appWidgetId)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_PICK_WIDGET && resultCode == RESULT_OK) {
            val appWidgetId = data?.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                -1
            ) ?: return

            configureWidget(appWidgetId)
        }
    }

    fun addWidgetToScreen(appWidgetId: Int) {
        val appWidgetInfo = appWidgetManager.getAppWidgetInfo(appWidgetId)

        val hostView = appWidgetHost.createView(
            this,
            appWidgetId,
            appWidgetInfo
        )

        hostView.setAppWidget(appWidgetId, appWidgetInfo)

        containerLayout.addView(hostView)
    }
}