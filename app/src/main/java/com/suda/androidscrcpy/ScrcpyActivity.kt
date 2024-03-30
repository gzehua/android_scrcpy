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
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.suda.androidscrcpy.utils.ADBUtils

const val TIME_INTERVAL: Long = 2000 // 定义两次返回间的时间间隔

class ScrcpyActivity : androidx.activity.ComponentActivity() {

    private var mBackPressed: Long = 0
    private val mScrcpyVM: ScrcpyVM by viewModels()
    private var mSurface: Surface? = null

    private val mSurfaceView: SurfaceView by lazy {
        findViewById(R.id.decoder_surface)
    }
    private val container: View by lazy {
        findViewById(R.id.container)
    }

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
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mScrcpyVM.init(mDevice!!)
        setUpUi(withNav)
        registerReceiver(mUsbUnPlugReceiver, IntentFilter(ACTION_USB_DEVICE_DETACHED))
        mScrcpyVM.rotationLiveData.observe(this) {
            if (it == SCREEN_ORIENTATION_PORTRAIT) {
                requestedOrientation = SCREEN_ORIENTATION_PORTRAIT
            } else if (it == SCREEN_ORIENTATION_LANDSCAPE) {
                requestedOrientation = SCREEN_ORIENTATION_LANDSCAPE
            }
        }
        mSurfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                mScrcpyVM.surfaceCreated(mSurfaceView.holder.surface)
            }

            override fun surfaceChanged(
                holder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                mScrcpyVM.surfaceDestroyed()
            }
        })
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setUpUi(withNav: Boolean) {
        setContentView(if (withNav) R.layout.surface_nav else R.layout.surface_no_nav)
        WindowCompat.getInsetsController(window, window.decorView)
            .hide(WindowInsetsCompat.Type.statusBars())
        resetSurface()
        mSurface = mSurfaceView.holder.surface
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

        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (mBackPressed + TIME_INTERVAL > System.currentTimeMillis()) {
                    finish()
                } else {
                    Toast.makeText(baseContext, "再按一次退出应用", Toast.LENGTH_SHORT).show()
                }
                mBackPressed = System.currentTimeMillis()
            }
        })
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
}