package com.ndds.homelauncher

import android.animation.ValueAnimator
import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isGone
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ndds.homelauncher.adapters.HomeWidgetListAdapter
import com.ndds.homelauncher.services.StorageService
import com.ndds.homelauncher.utils.RecyclerViewOnTouchListener
import com.ndds.homelauncher.widgets.ModalItem
import com.ndds.homelauncher.widgets.WidgetSlider
import kotlin.math.abs

class WidgetHandler(val appContext: MainActivity,val widgetSlider: WidgetSlider) {
    private var requestWidgetBindFlow: ActivityResultLauncher<Intent>
    val APPWIDGET_HOST_ID = 1024
    val appWidgetHost = AppWidgetHost(appContext, APPWIDGET_HOST_ID)

    private var onUserBindGranted: ((isGranted: Boolean) -> Unit)? = null
    init {
        requestWidgetBindFlow = appContext.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult(),
            { activityResult ->
                if (activityResult.resultCode == Activity.RESULT_OK) {
                    onUserBindGranted?.invoke(true)
                    onUserBindGranted = null
                } else if (activityResult.resultCode == Activity.RESULT_CANCELED) {
                    onUserBindGranted?.invoke(false)
                    onUserBindGranted = null
                }
            })
    }

    private fun promptToAddWidget() {
        appContext.modal.showWidgetList {
                selectedWidget ->
            val appWidgetManager = AppWidgetManager.getInstance(appContext)
            val newID = appWidgetHost.allocateAppWidgetId()
            if (!appWidgetManager.bindAppWidgetIdIfAllowed(newID, selectedWidget.provider)) {
                val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, newID)
                intent.putExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_PROVIDER,
                    selectedWidget.provider
                )
                onUserBindGranted = { isGranted ->
                    if (isGranted) {
                        widgetSlider.addPage(newID)
                    } else
                        appWidgetHost.deleteAppWidgetId(newID)
                }
                requestWidgetBindFlow.launch(intent)
            } else {
                widgetSlider.addPage(newID)
            }
        }
    }
    fun addWidget() {
        promptToAddWidget()
    }
}