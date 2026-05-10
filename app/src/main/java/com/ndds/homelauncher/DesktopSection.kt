package com.ndds.homelauncher

import android.app.WallpaperManager
import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.hardware.camera2.CameraManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ndds.homelauncher.adapters.PinnedAppAdapter
import com.ndds.homelauncher.services.ModalService
import com.ndds.homelauncher.services.StorageService
import com.ndds.homelauncher.widgets.CustomTextView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class DesktopSection(val appContext: MainActivity) {
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    var rootView: ViewGroup = appContext.findViewById(R.id.home_section)
    var adapter: PinnedAppAdapter
    var pinnedAppList: ArrayList<String>
    var isFlashLightOn = false
    fun initiateWallpaperControls() {
        val pickMedia = appContext.registerForActivityResult(ActivityResultContracts.PickVisualMedia(), { uri ->
            // Callback is invoked after the user selects a media item or closes the
            // photo picker.
            if (uri != null) {
                try {
                    val imageStream = appContext.contentResolver.openInputStream(uri)
                    val inputImage = BitmapFactory.decodeStream(imageStream)
                    imageStream?.close() // Important to close the stream
                    val wallpaperManager = WallpaperManager.getInstance(appContext)
                    wallpaperManager.setBitmap(inputImage)
                    appContext.openFileOutput("wallpaper.jpg", Context.MODE_PRIVATE).use {
                        inputImage.compress(Bitmap.CompressFormat.JPEG, 95, it)
                    }
                } catch (e: Exception) {
                    throw RuntimeException(e)
                }
            } else {
                Log.d("PhotoPicker", "No media selected")
            }
        })

        rootView.setOnClickListener({
            if (adapter.isEditMode) {
                toggleEditState()
            } else
                ModalService(appContext).showSettings({ setting ->
                    if (setting == ModalService.Setting.WallpaperChange)
                        pickMedia.launch(
                            PickVisualMediaRequest.Builder()
                                .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                .build()
                        )
                    else if (setting == ModalService.Setting.ActivateEditPinList) {
                        toggleEditState()
                    }
                })
        })
    }

    fun addApp(app: AppInfo) {
        StorageService(appContext).saveAppToPin(app)
        adapter.addApp(app)
        pinnedAppList.add(app.id)
        app.isPinned = true
    }

    fun unpinApp(app: AppInfo) {
        if (adapter.removeApp(app)) {
            StorageService(appContext).removeAppPin(app)
            app.isPinned = false
            pinnedAppList.remove(app.id)
        }
    }

    fun reportUninstall(packageName: CharSequence) {
        if (adapter.removeApp(packageName)) {
            pinnedAppList = ArrayList(adapter.appList.map { it.id })
            StorageService(appContext).updatePinnedApps(adapter.appList)
        }
    }
    fun dismissEditStateIfNeeded() {
        if (adapter.isEditMode)
            toggleEditState()
    }
    fun toggleEditState() {
        val v = rootView.findViewById<ImageView>(R.id.action_edit_fav_list)
        if (adapter.isEditMode) {
            v.setImageDrawable(AppCompatResources.getDrawable(appContext, R.drawable.edit))
            pinnedAppList = ArrayList(adapter.appList.map { it.id })
            StorageService(appContext).updatePinnedApps(adapter.appList)
        } else
            v.setImageDrawable(AppCompatResources.getDrawable(appContext, R.drawable.check))
        adapter.isEditMode = !adapter.isEditMode
    }

     init {
         initiateWallpaperControls()
        pinnedAppList = StorageService(appContext).getPinnedApps()
        val appListView = rootView.findViewById<RecyclerView>(R.id.fav_app_list)
        val itemTouchHelper = ItemTouchHelper(object: ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun isLongPressDragEnabled(): Boolean {
                return false
            }

            override fun isItemViewSwipeEnabled(): Boolean {
                return adapter.isEditMode
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                val fromPos = viewHolder.adapterPosition
                val toPos = target.adapterPosition
                val previousItem = adapter.appList[fromPos]
                adapter.appList[fromPos] = adapter.appList[toPos]
                adapter.appList[toPos] = previousItem
                adapter.notifyItemMoved(fromPos, toPos)
                return true
            }

            override fun onSwiped(
                viewHolder: RecyclerView.ViewHolder,
                direction: Int
            ) {
                unpinApp(adapter.appList[viewHolder.adapterPosition])
            }
        })

        adapter = PinnedAppAdapter(appContext, arrayListOf(), itemTouchHelper)
        appListView.layoutManager = LinearLayoutManager(appContext)
        appListView.adapter = adapter
        rootView.findViewById<View>(R.id.action_edit_fav_list).setOnClickListener {
            toggleEditState()
        }
        rootView.findViewById<View>(R.id.swipe_overlay).setOnTouchListener(object: View.OnTouchListener {
            var downX = 0f
            var downY = 0f
            override fun onTouch(
                v: View,
                motionEvent: MotionEvent
            ): Boolean {
                if (motionEvent.action == MotionEvent.ACTION_DOWN) {
                    downX = motionEvent.x
                    downY = motionEvent.y
                } else if (motionEvent.action == MotionEvent.ACTION_UP) {
                    if (adapter.isEditMode) {
                        toggleEditState()
                        return true
                    }

                    if (abs(downX - motionEvent.x) < 10 && abs(downY - motionEvent.y) < 10) {
                        appContext.appDrawer.openAppDrawer(true)
                        v.performClick()
                    } else {
                        val xDisplacement = abs(motionEvent.x - downX)
                        val yDisplacement = abs(motionEvent.y - downY)
                        if (xDisplacement > yDisplacement) {
                            if (xDisplacement > 20) {
                                if (motionEvent.x - downX > 0) {
                                    val pm = appContext.packageManager
                                    val cameraLaunchIntent = pm.getLaunchIntentForPackage(
                                        appContext.packageManager.resolveActivity(
                                            Intent(MediaStore.ACTION_IMAGE_CAPTURE),
                                            PackageManager.MATCH_DEFAULT_ONLY)!!.activityInfo.packageName
                                    )
                                    appContext.startActivity(cameraLaunchIntent)
                                } else {
                                    val cameraManager = appContext.getSystemService(CameraManager::class.java)
                                    val cameraId = cameraManager.cameraIdList[0]
                                    isFlashLightOn = !isFlashLightOn
                                    cameraManager.setTorchMode(cameraId, isFlashLightOn)
                                }
                            }
                        } else {
                            if (yDisplacement > 20) {
                                if (motionEvent.y - downY > 0) {
                                    try {
                                        val statusBarService = appContext.getSystemService("statusbar")
                                        val statusBarManager = Class.forName("android.app.StatusBarManager")
                                        val expandMethod = statusBarManager.getMethod("expandSettingsPanel")

                                        expandMethod.invoke(statusBarService)
                                    } catch (e: Exception) {
                                        Toast.makeText(appContext, "action not supported", Toast.LENGTH_SHORT).show()
                                        e.printStackTrace()
                                    }
                                } else {
                                    appContext.appDrawer.openAppDrawer(false)
                                }
                            }
                        }
                    }
                }
                return true
            }
        })
        itemTouchHelper.attachToRecyclerView(appListView)
    }

    fun updateData(packageRecognizer: PatternRecognizer) {
        val newData = ArrayList<AppInfo>()
        val iterator = pinnedAppList.iterator()
        var diff = false
        while (iterator.hasNext()) {
            val pinnedPackageName = iterator.next()
            val appInfo = packageRecognizer.getAppInfo(pinnedPackageName)
            if (appInfo != null) {
                appInfo.isPinned = true
                newData.add(appInfo)
            } else {
                iterator.remove()
                diff = true
            }
        }
        adapter.appList = newData
        adapter.notifyDataSetChanged()
        if (diff) {
            StorageService(appContext).updatePinnedApps(newData)
        }
    }

    fun updateTimestamp() {
        appContext.findViewById<CustomTextView>(R.id.time).text = SimpleDateFormat("hh:mm", Locale.getDefault()).format(Date())
        appContext.findViewById<CustomTextView>(R.id.date).text = SimpleDateFormat("EEEE, MMM dd", Locale.getDefault()).format(Date())
    }
    fun onResume() {
        updateTimestamp()
        updateConnectivityStatus(null)
        val connectivityManager = appContext.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        if (networkCallback != null)
            connectivityManager.unregisterNetworkCallback(networkCallback!!)
        networkCallback = object: ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                Handler(Looper.getMainLooper()).post {
                    updateConnectivityStatus(network)
                }
            }

            override fun onUnavailable() {
                Handler(Looper.getMainLooper()).post {
                    updateConnectivityStatus(null)
                }
            }

            override fun onLost(network: Network) {
                Handler(Looper.getMainLooper()).post {
                    updateConnectivityStatus(null)
                }
            }
        }
        connectivityManager.registerDefaultNetworkCallback(networkCallback!!)
    }

    fun onPause() {
        if (networkCallback != null) {
            val connectivityManager = appContext.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.unregisterNetworkCallback(networkCallback!!)
            networkCallback = null
        }
    }
    fun updateConnectivityStatus(network: Network?) {
        var wifiConnected: Boolean
        var cellularNetworkConnected: Boolean
        var networkCapabilities: NetworkCapabilities? = null
        var connectionStatus = "No network"
        var network = network
        val connectivityManager =
            appContext.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        if (network == null)
            network = connectivityManager.activeNetwork
        if (network != null) {
            networkCapabilities =
                connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        }

        if (networkCapabilities != null) {
            wifiConnected = networkCapabilities.hasTransport(
                NetworkCapabilities.TRANSPORT_WIFI)
            cellularNetworkConnected = networkCapabilities.hasTransport(
                NetworkCapabilities.TRANSPORT_CELLULAR)
            if (wifiConnected && cellularNetworkConnected)
                connectionStatus = "Wifi and Mobile data connected"
            else if (wifiConnected)
                connectionStatus = "Wifi connected"
            else if (cellularNetworkConnected)
                connectionStatus = "Mobile data connected"
            else
                connectionStatus = "Unknown connection"
        }
        rootView.findViewById<CustomTextView>(R.id.connection_status).text = connectionStatus
    }
}