package com.ndds.homelauncher

import android.animation.ValueAnimator
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ndds.homelauncher.adapters.GridAppAdapter
import com.ndds.homelauncher.models.AppInfo
import com.ndds.homelauncher.services.StorageService
import com.ndds.homelauncher.widgets.WallpaperImageView
import org.json.JSONArray

class AppDrawer(
    val appContext: MainActivity,
    val containerView: ViewGroup,
    val desktopSection: DesktopSection,
    ) {
    var adapter: GridAppAdapter
    var isDrawerOpen = false
    val patternRecognizer: PatternRecognizer = PatternRecognizer(appContext)

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
        val textClear = rootView.findViewById<View>(R.id.text_clear)
        textClear.visibility = View.GONE
        textClear.setOnClickListener {
            searchBar.text.clear()
        }
        searchBar.doAfterTextChanged { text ->
            adapter.applySearch(text.toString())
            textClear.visibility = if (text == null || text.isNotEmpty()) View.VISIBLE else View.GONE
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
        Thread {
            updateAppDescriptionDB(StorageService(appContext).getAppDescriptions())
        }.start()
    }
    fun updateAppDescriptionDB(payload: HashMap<String, JSONArray>) {
        payload.forEach { (key, words) ->
            for (i in 0 until words.length()) {
                adapter.appDescriptionPattern.addWord(key, words.getString(i))
            }
        }
    }
    fun openAppDrawer(focusSearchBar: Boolean) {
        if (isDrawerOpen) return
        rootView.alpha = 0f
        rootView.visibility = View.VISIBLE
        rootView.findViewById<RecyclerView>(R.id.app_list).scrollToPosition(0)
        val valueAnimator = ValueAnimator.ofInt(containerView.height, 0)

        valueAnimator.interpolator = DecelerateInterpolator()
        valueAnimator.duration = 500

        rootView.findViewById<EditText>(R.id.search_bar).setText("")

        val homeSection = desktopSection.rootView
        val wallpaperImage = getWallpaperWidget()
        wallpaperImage.visibility = View.VISIBLE
        val canAnimateBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && wallpaperImage.hasWallpaper
        if (canAnimateBlur) {
            val valueAnimator2 = ValueAnimator.ofFloat(1f, 40f)
            valueAnimator2.interpolator = DecelerateInterpolator()
            valueAnimator2.duration = 1300
            valueAnimator2.addUpdateListener { animator ->
                val blur = animator.animatedValue as Float
                wallpaperImage.setRenderEffect(
                    RenderEffect.createBlurEffect(
                        blur,
                        blur,
                        Shader.TileMode.CLAMP
                    )
                )
            }
            valueAnimator2.start()
        }
        valueAnimator.addUpdateListener { animator ->
            rootView.translationY = (animator.animatedValue as Int).toFloat()
            rootView.alpha = animator.animatedFraction
            if (!canAnimateBlur) {
                wallpaperImage.alpha = animator.animatedFraction
            }
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
    fun getWallpaperWidget() = (containerView.parent as ViewGroup).findViewById<WallpaperImageView>(R.id.wallpaper)
    fun closeDrawer() {
        if (!isDrawerOpen) return
        val valueAnimator = ValueAnimator.ofInt(0, containerView.findViewById<ViewGroup>(R.id.root).height)
        valueAnimator.interpolator = AccelerateInterpolator()
        valueAnimator.duration = 500
        val homeSection = containerView.findViewById<ViewGroup>(R.id.home_section)
        val imm = appContext.getSystemService(InputMethodManager::class.java)
        rootView.findViewById<EditText>(R.id.search_bar).let {
            if (it.isFocused)
                imm.hideSoftInputFromWindow(it.windowToken, 0)
        }
        rootView.alpha = 1f
        val wallpaperImage = getWallpaperWidget()
        val canAnimateBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && wallpaperImage.hasWallpaper
        if (canAnimateBlur) {
            val valueAnimator2 = ValueAnimator.ofFloat(40f, 1f)
            valueAnimator2.interpolator = AccelerateInterpolator()
            valueAnimator2.duration = 1300
            valueAnimator2.addUpdateListener { animator ->
                val blur = animator.animatedValue as Float
                wallpaperImage.setRenderEffect(
                    RenderEffect.createBlurEffect(
                        blur,
                        blur,
                        Shader.TileMode.CLAMP
                    )
                )
                if (animator.animatedFraction == 1f) {
                    wallpaperImage.visibility = View.GONE
                    wallpaperImage.alpha = 1f
                }
            }
            valueAnimator2.start()
        }
        valueAnimator.addUpdateListener { animator ->
            rootView.translationY = (animator.animatedValue as Int).toFloat()
            rootView.alpha = 1 - animator.animatedFraction
            if (!canAnimateBlur)
                wallpaperImage.alpha = 1 - animator.animatedFraction
            homeSection.alpha = animator.animatedFraction
            if (animator.animatedFraction == 1f) {
                isDrawerOpen = false
                rootView.visibility = View.GONE
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

    fun refreshData(iconColor: Int) {
        val intent = Intent(Intent.ACTION_MAIN, null)
        intent.addCategory(Intent.CATEGORY_LAUNCHER)
        val pm = appContext.packageManager
        val resolveInfoList = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)

        val newData = arrayListOf<AppInfo>()
        val isInitialLoad = patternRecognizer.markStart()
        for (info in resolveInfoList) {
            val app = patternRecognizer.getAppFromDrawer(info, pm, Color.BLACK)
            newData.add(app)
        }
        if (isInitialLoad) {
            val lasUsedID = StorageService(appContext).getLastUsedAppID()
            for (info in newData) {
                if (info.id == lasUsedID) {
                    info.isLastUsed = true
                    appContext.lastUsedApp = info
                    break
                }
            }
        }
        newData.sortWith { info, info1 ->
             if (info.isFresh == info1.isFresh) {
                 if (info.isLastUsed) -1
                 else if (info1.isLastUsed) 1
                 else {
                     val nameOrder = info.name[0].code - info1.name[0].code
                     if (nameOrder == 0)
                         return@sortWith info1.usedCount - info.usedCount
                     return@sortWith nameOrder
                 }
            } else
                if (info.isFresh) -1 else 1
        }
        patternRecognizer.flushRemovedPackages()
        adapter.apps = newData
        adapter.refresh()
    }
}