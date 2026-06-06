package com.ndds.homelauncher

import android.Manifest
import android.app.WallpaperManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import java.io.File
import kotlin.math.min
import androidx.core.net.toUri
import com.ndds.homelauncher.services.ModalService
import com.ndds.homelauncher.utils.SheetDialog
import com.ndds.homelauncher.utils.SnackBar
import com.ndds.homelauncher.widgets.WallpaperImageView


class MainActivity : AppCompatActivity() {
    private lateinit var requestPermission: ActivityResultLauncher<String>
    private var appInstallationListener: BroadcastReceiver? = null
    private var systemEventListener: BroadcastReceiver? = null
    lateinit var appDrawer: AppDrawer
    var lastUsedApp: AppInfo? = null
    lateinit var desktopSection: DesktopSection
    var promptedStorageAccess = false
    val sheetDialog: SheetDialog = SheetDialog(this)
    lateinit var modal: ModalService
    lateinit var snackBar: SnackBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_layout)

        desktopSection = DesktopSection(this)
        appDrawer = AppDrawer(this, findViewById(R.id.root), desktopSection)
        val rootView = findViewById<ViewGroup>(R.id.root)
        snackBar = SnackBar(this, rootView)
        modal = ModalService(this)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
                        or WindowInsetsCompat.Type.displayCutout()
                        or WindowInsetsCompat.Type.ime()
            )
            findViewById<View>(R.id.contrast_backdrop_top).let {
                val contrastParams = it.layoutParams as FrameLayout.LayoutParams
                contrastParams.height = bars.top
                it.layoutParams = contrastParams
            }
            findViewById<View>(R.id.contrast_backdrop_bottom).let {
                val contrastParams = it.layoutParams as FrameLayout.LayoutParams
                contrastParams.height = bars.bottom
                it.layoutParams = contrastParams
            }
            findViewById<View>(R.id.home_section).updatePadding(
                top = bars.bottom,
                bottom = bars.bottom
            )
            sheetDialog.adjustPadding(bars)
            snackBar.topInset = bars.top.toFloat()
            findViewById<View>(R.id.app_list).updatePadding(
                top = bars.top
            )
            findViewById<View>(R.id.appDrawer).updatePadding(
                bottom = bars.bottom
            )
            WindowInsetsCompat.CONSUMED
        }
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (sheetDialog.dismiss()) {
                    appDrawer.closeDrawer()
                    desktopSection.dismissEditStateIfNeeded()
                }
            }
        })
        findViewById<View>(R.id.root).apply {
            post {
                WallpaperManager.getInstance(this@MainActivity).setWallpaperOffsets(windowToken, 0.5f, 0.5f)
            }
        }
        requestPermission = registerForActivityResult(ActivityResultContracts.RequestPermission(), { permissionGranted ->
            if (permissionGranted)
                configureWallpaper()
        })
        configureWallpaper()
    }

    fun configureWallpaper() {
        val wallpaperImage = findViewById<WallpaperImageView>(R.id.wallpaper)
        var bitmap: Bitmap? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val wallpaperManager = WallpaperManager.getInstance(this)
            if (Environment.isExternalStorageManager()) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.READ_MEDIA_IMAGES
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermission.launch(Manifest.permission.READ_MEDIA_IMAGES)
                    return
                }
                val file =
                    wallpaperManager.getWallpaperFile(WallpaperManager.FLAG_SYSTEM)
                if (file != null) {
                    bitmap = BitmapFactory.decodeFileDescriptor(file.fileDescriptor)
                    file.close()
                }
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
            unregisterReceiver(systemEventListener)
        }

        val packageChangeFilter = IntentFilter()
        val systemEventFilter = IntentFilter()

        packageChangeFilter.addAction(Intent.ACTION_PACKAGE_ADDED)
        packageChangeFilter.addAction(Intent.ACTION_PACKAGE_REMOVED)
        packageChangeFilter.addDataScheme("package")

        systemEventFilter.addAction(Intent.ACTION_BATTERY_CHANGED)
        systemEventFilter.addAction(Intent.ACTION_TIME_TICK)
        systemEventListener = object : BroadcastReceiver() {
            override fun onReceive(p0: Context?, intent: Intent?) {
                if (intent == null) return
                if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                    desktopSection.updateBatteryLevel(intent)
                    return
                }
                if (intent.action == Intent.ACTION_TIME_TICK) {
                    desktopSection.updateTimestamp()
                    return
                }
            }
        }

        appInstallationListener = object : BroadcastReceiver() {
            override fun onReceive(p1: Context?, intent: Intent?) {
                if (intent == null) return

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
                        if (isFreshInstall) {
                            val appDetail = packageManager.getApplicationInfo(appPackageName, 0)
                            snackBar.show(
                                "${appDetail.loadLabel(packageManager)} installed",
                                appDetail.loadIcon(packageManager)
                            )
                        }
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
            packageChangeFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        val dataIntent = ContextCompat.registerReceiver(
            this,
            systemEventListener,
            systemEventFilter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        if (dataIntent != null)
            desktopSection.updateBatteryLevel(dataIntent)
    }

    fun launchApp(app: AppInfo) {
        val intent = Intent().setComponent(ComponentName(app.packageName, app.activityName))
            .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        lastUsedApp?.isLastUsed = false
        app.isLastUsed = true
        app.usedCount++
        lastUsedApp = app
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        if (promptedStorageAccess) {
            configureWallpaper()
        }
        registerInstallationReceiver()
        desktopSection.onResume()
        appDrawer.refreshData()
        desktopSection.updateData(appDrawer.patternRecognizer)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (sheetDialog.dismiss()) {
            appDrawer.closeDrawer()
            desktopSection.dismissEditStateIfNeeded()
        }
    }


    override fun onPause() {
        super.onPause()
        if (appInstallationListener != null) {
            unregisterReceiver(appInstallationListener)
            unregisterReceiver(systemEventListener)
            appInstallationListener = null
            systemEventListener = null
        }
        desktopSection.onPause()
    }

    override fun onStop() {
        super.onStop()
        if (appInstallationListener != null) {
            unregisterReceiver(appInstallationListener)
            unregisterReceiver(systemEventListener)
            appInstallationListener = null
            systemEventListener = null
        }
        appDrawer.closeDrawerImmediately()
    }
}