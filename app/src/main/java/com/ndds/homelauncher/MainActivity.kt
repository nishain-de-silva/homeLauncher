package com.ndds.homelauncher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import java.io.File


class MainActivity : AppCompatActivity() {
    private var appInstallationListener: BroadcastReceiver? = null
    lateinit var appDrawer: AppDrawer
    lateinit var desktopSection: DesktopSection

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_layout)

        desktopSection = DesktopSection(this)
        appDrawer = AppDrawer(this, findViewById(R.id.root), desktopSection)


        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root)) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
                        or WindowInsetsCompat.Type.displayCutout()
                        or WindowInsetsCompat.Type.ime()
            )
            v.updatePadding(
                left = v.paddingLeft,
                top = bars.top,
                right = v.paddingRight,
                bottom = bars.bottom,
            )
            WindowInsetsCompat.CONSUMED
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                appDrawer.closeDrawer()
                desktopSection.dismissEditStateIfNeeded()
            }
        })
        val wallpaper = File(filesDir, "wallpaper.jpg")
        if (wallpaper.exists()) {
            val wallpaperImage = findViewById<ImageView>(R.id.wallpaper)
            wallpaperImage.setImageBitmap(BitmapFactory.decodeFile(wallpaper.absolutePath))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                wallpaperImage.setRenderEffect(RenderEffect.createBlurEffect(80f, 80f, Shader.TileMode.CLAMP))
            }
        }
    }

    private fun registerInstallationReceiver() {
        if (appInstallationListener != null) {
            unregisterReceiver(appInstallationListener)
        }

        val intentFilter = IntentFilter()
        intentFilter.addAction(Intent.ACTION_PACKAGE_ADDED)
        intentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED)
        intentFilter.addAction(Intent.ACTION_TIME_TICK)
        intentFilter.addDataScheme("package")
        appInstallationListener = object : BroadcastReceiver() {
            override fun onReceive(p1: Context?, intent: Intent?) {
                if (intent == null) return
                if (intent.action == Intent.ACTION_TIME_TICK) {
                    desktopSection.updateTimestamp()
                    return
                }
                val appPackageName = intent.data?.encodedSchemeSpecificPart ?: return
                if (intent.action == Intent.ACTION_PACKAGE_ADDED) {
                    val isFreshInstall = !intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)
                    try {
                        val queryIntent = Intent(Intent.ACTION_MAIN, null)
                            .addCategory(Intent.CATEGORY_LAUNCHER)
                            .setPackage(appPackageName)
                        val resolveInfos = packageManager.queryIntentActivities(
                            queryIntent,
                            PackageManager.MATCH_ALL
                        )
                        for (resolveInfo in resolveInfos) {
                            appDrawer.addApp(
                                AppInfo(
                                    resolveInfo.loadLabel(packageManager).toString(),
                                    resolveInfo.activityInfo.applicationInfo.packageName,
                                    resolveInfo.activityInfo.name,
                                    resolveInfo.loadIcon(packageManager)
                                ), isFreshInstall
                            )
                        }
                    } catch (e: PackageManager.NameNotFoundException) {
                        throw RuntimeException(e)
                    }
                } else if (intent.action == Intent.ACTION_PACKAGE_REMOVED) {
                    appDrawer.reportUninstall(appPackageName)
                    desktopSection.reportUninstall(appPackageName)
                }
            }
        }
        ContextCompat.registerReceiver(
            this,
            appInstallationListener,
            intentFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onResume() {
        super.onResume()
        registerInstallationReceiver()
        desktopSection.onResume()
        appDrawer.refreshData()
        desktopSection.updateData(appDrawer.patternRecognizer)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        desktopSection.dismissEditStateIfNeeded()
        appDrawer.closeDrawer()
    }


    override fun onPause() {
        super.onPause()
        if (appInstallationListener != null) {
            unregisterReceiver(appInstallationListener)
            appInstallationListener = null
        }
        desktopSection.onPause()
    }

    override fun onStop() {
        super.onStop()
        if (appInstallationListener != null) {
            unregisterReceiver(appInstallationListener)
            appInstallationListener = null
        }
        appDrawer.closeDrawerImmediately()
    }
}