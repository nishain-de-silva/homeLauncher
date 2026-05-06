package com.ndds.homelauncher

import android.animation.ValueAnimator
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.View.MeasureSpec
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.core.net.toUri
import androidx.core.view.updatePadding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.ndds.homelauncher.widgets.ModalItem
import com.ndds.homelauncher.widgets.WallpaperImageView


class ModalService (val appContext: MainActivity){
    enum class Setting {
        WallpaperChange,
        ActivateEditPinList
    }

    fun showAppDetail(app: AppInfo, isAppDrawer: Boolean) {
        val dialogView = LayoutInflater.from(appContext).inflate(R.layout.app_detail_dropdown, null)
        val sheetDialog = appContext.sheetDialog// BottomSheetDialog(appContext, R.style.normalTextView)
        sheetDialog.setContentView(dialogView)
        sheetDialog.show()

        dialogView.findViewById<TextView>(R.id.appName).text = app.name
        val flags = appContext.packageManager.getApplicationInfo(app.packageName, PackageManager.GET_META_DATA).flags
        val uninstallButton = dialogView.findViewById<ModalItem>(R.id.uninstallAppBtn)
        if (flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
            uninstallButton.setLabel("cannot uninstall system app")
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
                .setData("package:${app.packageName}".toUri()))
        }
        dialogView.isNestedScrollingEnabled = true
        dialogView.findViewById<ModalItem>(R.id.pinOrUnpinAppBtn).let {
            val canPinApp = isAppDrawer && !app.isPinned
            if (canPinApp)
                it.setLabel("Add to desktop")
            else
                it.setLabel("Remove from desktop")
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
            shortcutContainer.adapter = shortcutListAdapter(sheetDialog, app.packageName, appContext, shortcuts)
        }
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
        val sheetDialog = appContext.sheetDialog//BottomSheetDialog(appContext, R.style.normalTextView)
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