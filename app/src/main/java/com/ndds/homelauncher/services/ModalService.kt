package com.ndds.homelauncher.services

import android.app.role.RoleManager
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.os.Process
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CalendarView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.net.toUri
import com.ndds.homelauncher.models.AppInfo
import com.ndds.homelauncher.MainActivity
import com.ndds.homelauncher.R
import com.ndds.homelauncher.adapters.ShortcutListAdapter
import com.ndds.homelauncher.adapters.WidgetPreviewAdapter
import com.ndds.homelauncher.utils.WidgetPreviewModel
import com.ndds.homelauncher.widgets.ModalItem


class ModalService(val appContext: MainActivity) {

    enum class Setting {
        WallpaperChange,
        ActivateEditPinList,
        AddWidget,
        AdvancedSettings
    }



    fun showAppDetail(app: AppInfo, isAppDrawer: Boolean) {
        val dialogView = LayoutInflater.from(appContext).inflate(R.layout.app_detail_dropdown, null)
        val sheetDialog =
            appContext.sheetDialog// BottomSheetDialog(appContext, R.style.normalTextView)
        sheetDialog.setContentView(dialogView)
        sheetDialog.show()

        dialogView.findViewById<TextView>(R.id.appName).text = app.name
        val flags = appContext.packageManager.getApplicationInfo(
            app.packageName,
            PackageManager.GET_META_DATA
        ).flags
        val uninstallButton = dialogView.findViewById<ModalItem>(R.id.uninstallAppBtn)
        if (flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
            uninstallButton.setLabel("Cannot uninstall System app")
            uninstallButton.alpha = 0.5f
        } else {
            uninstallButton.setOnClickListener { v ->
                val uninstallIntent = Intent(Intent.ACTION_DELETE).setData(
                    "package:${app.packageName}".toUri()
                )
                sheetDialog.dismiss()
                appContext.startActivity(uninstallIntent)
            }
        }
        dialogView.findViewById<ModalItem>(R.id.appInfoBtn).setOnClickListener { v ->
            sheetDialog.dismiss()
            appContext.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .setData("package:${app.packageName}".toUri())
            )
        }
        dialogView.findViewById<ModalItem>(R.id.pinOrUnpinAppBtn).let {
            val canPinApp = isAppDrawer && !app.isPinned
            if (canPinApp) {
                it.setLabel("Add to desktop")
                it.setIcon(AppCompatResources.getDrawable(appContext, R.drawable.pin))
            } else {
                it.setLabel("Remove from desktop")
                it.setIcon(AppCompatResources.getDrawable(appContext, R.drawable.unpin))
            }
            it.setOnClickListener { v ->
                sheetDialog.dismiss()
                if (canPinApp) {
                    appContext.desktopSection.addApp(app)
                } else {
                    appContext.desktopSection.unpinApp(app)
                }
            }
        }
        val shortcutContainer = dialogView.findViewById<ListView>(R.id.shortcut_container)
        if (isDefaultHomeLauncher()) {
            val shortcuts = getShortcuts(app.packageName).filter { it.isEnabled }
            if (shortcuts.isEmpty())
                dialogView.findViewById<View>(R.id.divider).visibility = View.GONE
            shortcutContainer.adapter =
                ShortcutListAdapter(sheetDialog, app.packageName, appContext, shortcuts)
        }
    }

    fun showCalender() {
        val sheetDialog = appContext.sheetDialog
        val calendarView = CalendarView(appContext)
        calendarView.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        sheetDialog.setContentView(calendarView)
        sheetDialog.show()
    }

    fun showWidgetList(onWidgetSelected: (AppWidgetProviderInfo) -> Unit) {
        val sheetDialog = appContext.sheetDialog
        val appWidgetManager = AppWidgetManager.getInstance(appContext)
        val initialView = LayoutInflater.from(appContext).inflate(R.layout.no_widget, null) as ViewGroup
        val categoryData = ArrayList<WidgetPreviewModel>()
        val pm = appContext.packageManager
        appWidgetManager.installedProviders.forEach { provider ->
            val group = categoryData.find { it.packageName.equals(provider.provider.packageName) }
            if (group == null) {
                val appInfo = pm.getApplicationInfo(provider.provider.packageName, 0)
                categoryData.add(WidgetPreviewModel(
                    appInfo.loadLabel(pm),
                    provider.provider.packageName,
                    appInfo.loadIcon(pm),
                    arrayListOf(provider)
                ))
            } else {
                group.widgets.add(provider)
            }
        }
        initialView.findViewById<ListView>(R.id.widget_list).adapter = WidgetPreviewAdapter(appContext,
            { selectedWidgetInfo ->
                sheetDialog.dismissListener = {
                    onWidgetSelected(selectedWidgetInfo)
                }
                sheetDialog.dismiss()
            },
            categoryData
        );
        sheetDialog.setContentView(initialView)
        sheetDialog.show()
    }
    private fun getShortcuts(packageName: String): List<ShortcutInfo> {
        val launcherApps = appContext.getSystemService(LauncherApps::class.java)

        val query = LauncherApps.ShortcutQuery().apply {
            setPackage(packageName)
            setQueryFlags(
                LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                        LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
            )
        }
        return launcherApps.getShortcuts(query, Process.myUserHandle()) ?: emptyList()
    }

    fun isDefaultHomeLauncher() = appContext.packageManager.resolveActivity(
        Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
        PackageManager.MATCH_DEFAULT_ONLY
    )?.activityInfo?.packageName == appContext.packageName

    fun showSettings(settingsCallback: (setting: Setting) -> Unit) {
        val dialogView = LayoutInflater.from(appContext).inflate(R.layout.home_settings_modal, null)
        val sheetDialog = appContext.sheetDialog
        sheetDialog.setContentView(dialogView)
        sheetDialog.show()
        dialogView.findViewById<TextView>(R.id.changeWallpaper).setOnClickListener {
            settingsCallback(Setting.WallpaperChange)
            sheetDialog.dismiss()
        }
        dialogView.findViewById<TextView>(R.id.editFavList).setOnClickListener {
            settingsCallback(Setting.ActivateEditPinList)
            sheetDialog.dismiss()
        }
        dialogView.findViewById<TextView>(R.id.add_widget).setOnClickListener {
            settingsCallback(Setting.AddWidget)
        }
        dialogView.findViewById<TextView>(R.id.advanced_settings).setOnClickListener {
            settingsCallback(Setting.AdvancedSettings)
        }
        dialogView.findViewById<TextView>(R.id.defaultLauncherSetup).let {
            if (isDefaultHomeLauncher())
                it.visibility = View.GONE
            else
                it.setOnClickListener {
                    val roleManager = appContext.getSystemService(RoleManager::class.java)
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
                    appContext.startActivityForResult(intent, 456)
                    sheetDialog.dismiss()
                }
        }
    }
}