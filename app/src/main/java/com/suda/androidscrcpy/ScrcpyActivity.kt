package com.suda.androidscrcpy

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.hardware.usb.UsbManager.ACTION_USB_DEVICE_DETACHED
import android.os.Bundle
import android.view.Surface
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import android.widget.ViewSwitcher.KEEP_SCREEN_ON
import androidx.activity.viewModels
import androidx.core.view.updateLayoutParams
import com.suda.androidscrcpy.utils.ADBUtils


class ScrcpyActivity : androidx.activity.ComponentActivity() {

    private val TIME_INTERVAL: Long = 2000 // 定义两次返回间的时间间隔

    private val mSurfaceView: SurfaceView by lazy {
        findViewById(R.id.decoder_surface)
    }
    private val container: View by lazy {
        findViewById(R.id.container)
    }

    private val mScrcpyVM: ScrcpyVM by viewModels()
    var mSurface: Surface? = null


    var mBackPressed: Long = 0
    private val mDevice: String? by lazy {
        intent.getStringExtra("device")
    }

    private val withNav: Boolean by lazy {
        intent.getBooleanExtra("withNav", true)
    }

    private val mUsbUnPlugReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val device = intent!!.getParcelableExtra(UsbManager.EXTRA_DEVICE) as UsbDevice?
            device?.run {
                //todo 判断机型
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mScrcpyVM.init(mDevice!!)

        setUpUi(withNav)
        window.addFlags(KEEP_SCREEN_ON)

        registerReceiver(mUsbUnPlugReceiver, IntentFilter(ACTION_USB_DEVICE_DETACHED))
        mScrcpyVM.rotationLiveData.observe(this) {
            if (it == SCREEN_ORIENTATION_PORTRAIT) {
                requestedOrientation = SCREEN_ORIENTATION_PORTRAIT
            } else if (it == SCREEN_ORIENTATION_LANDSCAPE) {
                requestedOrientation = SCREEN_ORIENTATION_LANDSCAPE
            }
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    private fun setUpUi(withNav: Boolean) {
        setContentView(if (withNav) R.layout.surface_nav else R.layout.surface_no_nav)
        val decorView = window.decorView
        decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
        resetSurface()
        mSurface = mSurfaceView.holder.surface
        mScrcpyVM.mSurface = mSurface
        if (withNav) {
            val backButton = findViewById<View>(R.id.back_button)
            val homeButton = findViewById<View>(R.id.home_button)
            val appswitchButton = findViewById<View>(R.id.appswitch_button)
            backButton.setOnClickListener {
                ADBUtils.exec(
                    "adb.bin-arm",
                    "-s",
                    mDevice.toString(),
                    "shell", "input", "keyevent", "4"
                )
            }
            homeButton.setOnClickListener {
                ADBUtils.exec(
                    "adb.bin-arm",
                    "-s",
                    mDevice.toString(),
                    "shell",
                    "input",
                    "keyevent",
                    "3"
                )
            }
            appswitchButton.setOnClickListener {
                ADBUtils.exec(
                    "adb.bin-arm",
                    "-s",
                    mDevice.toString(),
                    "shell",
                    "input",
                    "keyevent",
                    "187"
                )
            }
        }
        mSurfaceView.setOnTouchListener { _, event ->
            mScrcpyVM.offerTouchEvent(event, mSurfaceView.width, mSurfaceView.height)
            true
        }
    }

    private fun resetSurface() {
        mSurfaceView?.post {
            mSurfaceView?.run {
                val screenWidth = mScrcpyVM.mScreenWidth
                val screenHeight = mScrcpyVM.mScreenHeight
                val decorView = container
                if (decorView.width * (screenHeight * 1f / screenWidth) < height) {
                    //垂直居中
                    this.updateLayoutParams<ViewGroup.LayoutParams> {
                        width = container.width
                        height =
                            (decorView.width * (screenHeight * 1f / screenWidth)).toInt()
                    }
                } else {
                    //水平居中
                    this.updateLayoutParams<ViewGroup.LayoutParams> {
                        height = decorView.height
                        width =
                            (decorView.height * (screenWidth * 1f / screenHeight)).toInt()
                    }
                }
            }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mUsbUnPlugReceiver)
    }

    var resumeFromPause = false

    override fun onStop() {
        super.onStop()
        resumeFromPause = true
        mScrcpyVM.pause()
    }

    override fun onStart() {
        super.onStart()
        if (resumeFromPause) {
            mScrcpyVM.resume(mSurfaceView.holder.surface)
        }
        resumeFromPause = false
    }

    override fun onBackPressed() {
        if (mBackPressed + TIME_INTERVAL > System.currentTimeMillis()) {
            super.onBackPressed()
            return
        } else {
            Toast.makeText(baseContext, "再按一次退出应用", Toast.LENGTH_SHORT).show()
        }
        mBackPressed = System.currentTimeMillis()
    }
}