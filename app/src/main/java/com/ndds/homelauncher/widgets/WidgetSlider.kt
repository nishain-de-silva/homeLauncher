package com.ndds.homelauncher.widgets

import android.animation.ValueAnimator
import android.app.Activity
import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.marginStart
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ndds.homelauncher.MainActivity
import com.ndds.homelauncher.R
import com.ndds.homelauncher.adapters.HomeWidgetListAdapter
import com.ndds.homelauncher.adapters.ReArrangeWidgetAdapter
import com.ndds.homelauncher.services.StorageService
import kotlin.math.abs

class WidgetSlider @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
): FrameLayout(context, attrs, defStyleAttr) {
    var page = 0
    lateinit var appContext: MainActivity
    lateinit var widgetIDs: ArrayList<Int>
    lateinit var initialView: View
    val APPWIDGET_HOST_ID = 1024
    lateinit var adapter: HomeWidgetListAdapter
    lateinit var appWidgetManager: AppWidgetManager

    private lateinit var requestWidgetBindFlow: ActivityResultLauncher<Intent>
    var appWidgetHost: AppWidgetHost? = null
    lateinit var longPressRunnable: Runnable
    var longPressHandler: Handler? = null

    private var onUserBindGranted: ((isGranted: Boolean) -> Unit)? = null

    fun load(appContext: MainActivity) {
        this.appContext = appContext
        initialView = getChildAt(0)
        widgetIDs = StorageService(context).getWidgetList()
        appWidgetManager = AppWidgetManager.getInstance(context)
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

    fun promptToAddWidget() {
        appContext.modal.showWidgetList {
                selectedWidget ->
            val appWidgetManager = AppWidgetManager.getInstance(appContext)
            if (appWidgetHost == null) {
                appWidgetHost = AppWidgetHost(appContext, APPWIDGET_HOST_ID)
                appWidgetHost!!.startListening()
            }
            val appWidgetHost = appWidgetHost!!
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
                        addPage(newID)
                    } else {
                        appWidgetHost.deleteAppWidgetId(newID)
                        if (widgetIDs.isEmpty()) {
                            appWidgetHost.stopListening()
                            this.appWidgetHost = null
                        }
                    }
                }
                requestWidgetBindFlow.launch(intent)
            } else {
                addPage(newID)
            }
        }
    }
    private fun loadPage(isLeftSwipe: Boolean) {
        var nextView: View
        var targetHeight: Int
        if (page == 0) {
            nextView = initialView
            nextView.visibility = VISIBLE
            targetHeight = initialView.height
            if (appWidgetHost != null) {
                appWidgetHost!!.stopListening()
                appWidgetHost = null
            }
        } else {
            if (appWidgetHost == null) {
                appWidgetHost = AppWidgetHost(appContext, APPWIDGET_HOST_ID)
                appWidgetHost!!.startListening()
            }
            val widgetID = widgetIDs[page - 1]
            val hostView = buildWidgetView(widgetID)
            val widgetHeight = hostView.layoutParams.height
            targetHeight = widgetHeight
            nextView = hostView
        }

        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = 500
        animator.interpolator = DecelerateInterpolator()
        val closingView = getChildAt(childCount - 1)
        nextView.translationX = if (isLeftSwipe) width.toFloat() else -width.toFloat()
        if (page != 0)
            addView(nextView)
        val layoutParams = layoutParams
        val currentHeight = height
        animator.addUpdateListener { animator ->
            if (isLeftSwipe) {
                closingView.translationX = -animator.animatedFraction * width
                nextView.translationX = width * (1 - animator.animatedFraction)
            } else {
                closingView.translationX = animator.animatedFraction * width
                nextView.translationX = -width * (1 - animator.animatedFraction)
            }
            nextView.scaleX = 0.5f + animator.animatedFraction * 0.5f
            nextView.scaleY = 0.5f + animator.animatedFraction * 0.5f
            closingView.scaleX = 0.5f + (1 - animator.animatedFraction) * 0.5f
            closingView.scaleY = 0.5f + (1 - animator.animatedFraction) * 0.5f

            if (animator.animatedFraction == 1f) {
                if (closingView == initialView) {
                    closingView.visibility = GONE
                } else {
                    if (Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.BAKLAVA_1) {
                        (closingView as AppWidgetHostView).stopVisibilityTracking()
                    }
                    removeView(closingView)
                }
                layoutParams.height = LayoutParams.WRAP_CONTENT
            } else
                layoutParams.height = (currentHeight + ((targetHeight - currentHeight) * animator.animatedFraction)).toInt()
            setLayoutParams(layoutParams)
        }
        animator.start()
    }

    fun addPage(id: Int) {
        if (widgetIDs.isEmpty()) {
            widgetIDs.add(id)
            page = 1
        } else {
            widgetIDs.add(page, id)
            page++
        }
        StorageService(context).updateWidgetList(widgetIDs)
        loadPage(true)
    }
    fun buildWidgetView(id: Int): AppWidgetHostView {
        val widgetInfo = appWidgetManager.getAppWidgetInfo(id)
        val hostView = appWidgetHost!!.createView(
            context.applicationContext,
            id,
            widgetInfo
        )
        hostView.setAppWidget(id, widgetInfo)
        val widgetWidth = width - (2 * 35 * context.resources.displayMetrics.density).toInt()
        val widgetHeight = (widgetWidth * (widgetInfo.minHeight.toFloat() / widgetInfo.minWidth.toFloat())).toInt()
        val layoutParams =  FrameLayout.LayoutParams(
            widgetWidth,
            widgetHeight
        )
        layoutParams.gravity = Gravity.CENTER_HORIZONTAL
        hostView.layoutParams = layoutParams
        return hostView
    }
    private fun reArrangeWidgets() {
        val content = LayoutInflater.from(context).inflate(R.layout.re_arrange_widget_modal, null)
        val reArrangeWidgetList = content.findViewById<RecyclerView>(R.id.reArrangeWidgetList)
        lateinit var adapter: ReArrangeWidgetAdapter
        val itemTouchHelper = ItemTouchHelper(object: ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
            override fun isLongPressDragEnabled(): Boolean {
                return false
            }

            override fun isItemViewSwipeEnabled(): Boolean {
                return false
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPos = viewHolder.adapterPosition
                val toPos = target.adapterPosition
                val previousItem = adapter.data[fromPos]
                adapter.data[fromPos] = adapter.data[toPos]
                adapter.data[toPos] = previousItem
                adapter.notifyItemMoved(fromPos, toPos)
                return true
            }

            override fun onSwiped(
                p0: RecyclerView.ViewHolder,
                p1: Int
            ) {
                TODO("Not yet implemented")
            }

        })
        adapter = ReArrangeWidgetAdapter(
            appContext,
            ArrayList(widgetIDs.map {
                Pair(it, appWidgetManager.getAppWidgetInfo(it))
            }),
            itemTouchHelper
        )

        val sheetDialog = appContext.sheetDialog
        reArrangeWidgetList.adapter = adapter
        reArrangeWidgetList.layoutManager = LinearLayoutManager(appContext)
        itemTouchHelper.attachToRecyclerView(reArrangeWidgetList)

        sheetDialog.setContentView(content)
        val currentWidgetID = widgetIDs[page - 1]
        sheetDialog.show({
            val updatedData = ArrayList(adapter.data.map { it.first })
            val swappedID = updatedData[page - 1]
            if (swappedID != currentWidgetID) {
                val hostView = buildWidgetView(swappedID)
                if (Build.VERSION.SDK_INT_FULL >= Build.VERSION_CODES_FULL.BAKLAVA_1) {
                    hostView.startVisibilityTracking()
                }
                removeView(getChildAt(1))
                addView(hostView)
            }

            widgetIDs = updatedData
            StorageService(appContext).updateWidgetList(updatedData)
        })
    }
    private fun handleLongPress() {
        if (page == 0) return
        val sheetDialog = appContext.sheetDialog
        sheetDialog.setContentView(sheetDialog.buildContent(
            "Widget Controls",
            arrayOf("Add Widget", "Re-arrange widgets","Remove Widget"),
            { selectionOptionIndex ->
                if (selectionOptionIndex == 0)
                    promptToAddWidget()
                else if (selectionOptionIndex == 1) {
                    reArrangeWidgets()
                }
                else if (selectionOptionIndex == 2) {
                    removeWidget()
                    sheetDialog.dismiss()
                }
            }
        ))
        sheetDialog.show()
        isLongPressPerformed = true
    }
    private fun removeWidget() {
        if (page == 0) return
        widgetIDs.removeAt(page - 1)
        page--
        StorageService(context).updateWidgetList(widgetIDs)
        loadPage(false)
        Toast.makeText(context, "Widget removed", Toast.LENGTH_SHORT).show()
    }
    var downX = 0f
    var downY = 0f
    var isLongPressPerformed = false
    var isDragActionPerformed = false
    fun purgeLongPressHandler() {
        if (longPressHandler != null) {
            longPressHandler!!.removeCallbacks(longPressRunnable)
            longPressHandler = null
        }
    }

    override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            downX = event.x
            downY = event.y
            longPressRunnable = Runnable { handleLongPress() }
            longPressHandler = Handler(Looper.getMainLooper())
            longPressHandler!!.postDelayed(longPressRunnable,
                ViewConfiguration.getLongPressTimeout().toLong())
            isLongPressPerformed = false
            isDragActionPerformed = false
            return false
        } else if (event.action == MotionEvent.ACTION_MOVE) {
            if (abs(downX - event.x) > 15 || abs(downY - event.y) > 15) {
                purgeLongPressHandler()
                if (!isDragActionPerformed && abs(downX - event.x) > abs(downY - event.y)) {
                    isDragActionPerformed = true
                    val isLeftSwipe = downX > event.x
                    if (isLeftSwipe && page == widgetIDs.size) {
                        if (widgetIDs.isEmpty())
                            return false
                        page = 0
                    } else if (!isLeftSwipe && page == 0) {
                        if (widgetIDs.isEmpty())
                            return false
                        page = widgetIDs.size
                    } else {
                        if (isLeftSwipe)
                            page++
                        else
                            page--
                    }
                    loadPage(isLeftSwipe)
                    return true
                }
                return false
            }
        } else if (event.action == MotionEvent.ACTION_UP) {
            purgeLongPressHandler()
            if (isLongPressPerformed || isDragActionPerformed)
                return true
        }
        return false
    }
}