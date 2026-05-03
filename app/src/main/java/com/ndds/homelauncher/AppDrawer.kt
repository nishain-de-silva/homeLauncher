package com.ndds.homelauncher

import android.animation.ValueAnimator
import android.app.WallpaperManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RenderEffect
import android.graphics.Shader
import android.hardware.display.DisplayManager
import android.os.Build
import android.util.Log
import android.view.Display
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import androidx.core.graphics.luminance
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File
import java.util.Random
import kotlin.math.max
import kotlin.math.min

class AppDrawer(
    val appContext: MainActivity,
    val containerView: ViewGroup,
    val desktopSection: DesktopSection,
    ) {
    var adapter: GridAppAdapter
    var isDrawerOpen = false
    val patternRecognizer: PatternRecognizer = PatternRecognizer()

    var rootView: ViewGroup = LayoutInflater.from(appContext).inflate(R.layout.app_drawer, null) as ViewGroup

    init {
        rootView.visibility = View.GONE
        containerView.addView(rootView)
        val recyclerView = rootView.findViewById<RecyclerView>(R.id.app_list)
        val gridLayout = GridLayoutManager(appContext, 4)
        recyclerView.layoutManager = gridLayout
        adapter = GridAppAdapter(appContext, arrayListOf())
        recyclerView.adapter = adapter
        val searchBar = rootView.findViewById<EditText>(R.id.search_bar)
        searchBar.doAfterTextChanged { text ->
            adapter.applySearch(text.toString())
        }
        searchBar.setOnEditorActionListener { textView, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                adapter.launchFirstApp()
                val imm = appContext.getSystemService(InputMethodManager::class.java)
                imm.hideSoftInputFromWindow(searchBar.windowToken, 0)
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }
    }
    fun openAppDrawer(focusSearchBar: Boolean) {
        if (isDrawerOpen) return
        rootView.alpha = 0f
        rootView.visibility = View.VISIBLE
        val valueAnimator = ValueAnimator.ofInt(containerView.height, 0)
        valueAnimator.interpolator = DecelerateInterpolator()
        valueAnimator.duration = 400
        rootView.findViewById<EditText>(R.id.search_bar).setText("")

        val homeSection = desktopSection.rootView
        val wallpaperImage = getWallpaperWidget()
//        wallpaperImage.setImageBitmap(generateMask(containerView))
        wallpaperImage.visibility = View.VISIBLE
        valueAnimator.addUpdateListener { animator ->
            rootView.translationY = (animator.animatedValue as Int).toFloat()
            rootView.alpha = animator.animatedFraction
            wallpaperImage.alpha = animator.animatedFraction
            homeSection.alpha = 1 - animator.animatedFraction
            if (animator.animatedFraction == 1f) {
                homeSection.visibility = View.GONE
                if (focusSearchBar)
                    rootView.findViewById<EditText>(R.id.search_bar).let {
                        val imm = appContext.getSystemService(InputMethodManager::class.java)
                        it.requestFocus()
                        imm.showSoftInput(it, InputMethodManager.SHOW_IMPLICIT)
                    }
                isDrawerOpen = true
            }
        }

        valueAnimator.start()
    }
    fun closeDrawerImmediately() {
        desktopSection.rootView.alpha = 1f
        desktopSection.rootView.visibility = View.VISIBLE
        val wallpaperImage = getWallpaperWidget()
        wallpaperImage.visibility = View.GONE
        val imm = appContext.getSystemService(InputMethodManager::class.java)
        rootView.findViewById<EditText>(R.id.search_bar).let {
            if (it.isFocused)
                imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
        isDrawerOpen = false
        rootView.visibility = View.GONE
    }
    fun getWallpaperWidget() = (containerView.parent as ViewGroup).findViewById<ImageView>(R.id.wallpaper)
    fun closeDrawer() {
        if (!isDrawerOpen) return
        val valueAnimator = ValueAnimator.ofInt(0, containerView.findViewById<ViewGroup>(R.id.root).height)
        valueAnimator.interpolator = AccelerateInterpolator()
        valueAnimator.duration = 400
        val homeSection = containerView.findViewById<ViewGroup>(R.id.home_section)
        val imm = appContext.getSystemService(InputMethodManager::class.java)
        rootView.findViewById<EditText>(R.id.search_bar).let {
            if (it.isFocused)
                imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
        rootView.alpha = 1f
        val wallpaperImage = getWallpaperWidget()
        valueAnimator.addUpdateListener { animator ->
            rootView.translationY = (animator.animatedValue as Int).toFloat()
            rootView.alpha = 1 - animator.animatedFraction
            wallpaperImage.alpha = 1 - animator.animatedFraction
            homeSection.alpha = animator.animatedFraction
            if (animator.animatedFraction == 1f) {
                isDrawerOpen = false
                rootView.visibility = View.GONE
                wallpaperImage.visibility = View.GONE
            }
        }
        homeSection.visibility = View.VISIBLE
        valueAnimator.start()
    }

    fun addApp(app: AppInfo, isFreshInstall: Boolean) {
        patternRecognizer.addPackage(app, isFreshInstall)
        adapter.apps.add(0, app)
        adapter.refresh()
    }

    fun reportUninstall(packageName: String) {
        adapter.apps.removeAll { a -> a.packageName == packageName }
        patternRecognizer.removePackage(packageName)
        adapter.refresh()
    }

    fun refreshData() {
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        val pm = appContext.packageManager
        val resolveInfoList = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)

        val newData = arrayListOf<AppInfo>()
        patternRecognizer.markStart()
        for (info in resolveInfoList) {
            val app = patternRecognizer.getAppFromDrawer(info, pm)
            newData.add(app)
        }
        newData.sortWith { info, info1 ->
            if (info.isFresh == info1.isFresh)
                 info.name[0].code - info1.name[0].code
            else
                if (info.isFresh) -1 else 1
        }
        patternRecognizer.flushRemovedPackages()
        adapter.apps = newData
        adapter.refresh()
    }
}