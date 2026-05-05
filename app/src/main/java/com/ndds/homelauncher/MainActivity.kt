package com.ndds.homelauncher

import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.RenderEffect
import android.graphics.Shader
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.view.View
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import java.io.File
import java.io.FileInputStream
import kotlin.math.min
import androidx.core.net.toUri


class MainActivity : AppCompatActivity() {
    private var appInstallationListener: BroadcastReceiver? = null
    lateinit var appDrawer: AppDrawer
    var lastUsedApp: AppInfo? = null
    lateinit var desktopSection: DesktopSection
    var promptedStorageAccess = false

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
        findViewById<View>(R.id.root).apply {
            post {
                WallpaperManager.getInstance(this@MainActivity).setWallpaperOffsets(windowToken, 0.5f, 0.5f)
            }
        }
        configureWallpaper()
    }

    fun configureWallpaper() {
        val wallpaperImage = findViewById<ImageView>(R.id.wallpaper)
        var bitmap: Bitmap
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                val file = WallpaperManager.getInstance(this)
                    .getWallpaperFile(WallpaperManager.FLAG_SYSTEM)!!
                bitmap = BitmapFactory.decodeFileDescriptor(file.fileDescriptor)
                file.close()
                if (promptedStorageAccess)
                    promptedStorageAccess = false
            } else {
                if (!promptedStorageAccess) {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                    intent.data = "package:${packageName}".toUri()
                    startActivity(intent)
                    promptedStorageAccess = true
                    return
                } else {
                    val wallpaper = File(filesDir, "wallpaper.jpg")
                    if (wallpaper.exists()) {
                        bitmap = BitmapFactory.decodeFile(wallpaper.absolutePath)
                    } else
                        return
                }
            }
        } else {
            val wallpaper = File(filesDir, "wallpaper.jpg")
            if (wallpaper.exists()) {
                bitmap = BitmapFactory.decodeFile(wallpaper.absolutePath)
            } else
                return
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            wallpaperImage.setImageBitmap(bitmap)
//            wallpaperImage.setRenderEffect(RenderEffect.createBlurEffect(80f,80f, Shader.TileMode.CLAMP))
        } else {
//            wallpaperImage.setImageBitmap(
//                blurImageBackwardCompatible(bitmap, 80f)
//            )
        }
    }

    fun blurImageBackwardCompatible(image: Bitmap, radius: Float): Bitmap {
        val output = Bitmap.createBitmap(image)

        val rs = RenderScript.create(this)
        val input = Allocation.createFromBitmap(rs, image)
        val outputAlloc = Allocation.createFromBitmap(rs, output)

        val blur = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
        blur.setRadius(min(radius, 25f))
        blur.setInput(input)
        blur.forEach(outputAlloc)

        outputAlloc.copyTo(output)
        rs.destroy()

        return output
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

    fun launchApp(app: AppInfo) {
        val intent = Intent().setComponent(ComponentName(app.packageName, app.activityName))
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        lastUsedApp?.isLastUsed = false
        app.isLastUsed = true
        lastUsedApp = app
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        if (promptedStorageAccess) {
            configureWallpaper()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                Environment.isExternalStorageManager()) {

            }
        }
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