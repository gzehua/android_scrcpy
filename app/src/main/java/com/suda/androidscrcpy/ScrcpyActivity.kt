package com.suda.androidscrcpy

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
import android.content.pm.ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
import android.os.Bundle
import android.view.MotionEvent
import android.view.Surface
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import android.widget.ViewSwitcher.KEEP_SCREEN_ON
import androidx.activity.viewModels
import androidx.core.view.updateLayoutParams
import com.suda.androidscrcpy.utils.ADBUtils


class ScrcpyActivity : androidx.activity.ComponentActivity() {



    private val scrcpyVM: ScrcpyVM by viewModels()
    var surfaceView: SurfaceView? = null
    var surface: Surface? = null

    val TIME_INTERVAL: Long = 2000 // 定义两次返回间的时间间隔

    var mBackPressed: Long = 0
    private val device: String? by lazy {
        intent.getStringExtra("device")
    }

    private val withNav: Boolean by lazy {
        intent.getBooleanExtra("withNav", true)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setUpUi(withNav)
        window.addFlags(KEEP_SCREEN_ON)

        scrcpyVM.init(device!!)
        scrcpyVM.rotationLiveData.observe(this){
            if (it == SCREEN_ORIENTATION_PORTRAIT){
                requestedOrientation = SCREEN_ORIENTATION_PORTRAIT
            }else if (it == SCREEN_ORIENTATION_LANDSCAPE){
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
        surfaceView = findViewById<View>(R.id.decoder_surface) as SurfaceView
        surfaceView?.post {
            surfaceView?.run {
                if (decorView.width * (scrcpyVM.screenHeight * 1f / scrcpyVM.screenWidth) < height) {
                    //垂直居中
                    this.updateLayoutParams<ViewGroup.LayoutParams> {
                        width = decorView.width
                        height =
                            (decorView.width * (scrcpyVM.screenHeight * 1f / scrcpyVM.screenWidth)).toInt()
                    }
                } else {
                    //水平居中
                    this.updateLayoutParams<ViewGroup.LayoutParams> {
                        height = decorView.height
                        width =
                            (decorView.height * (scrcpyVM.screenWidth * 1f / scrcpyVM.screenHeight)).toInt()
                    }
                }
            }
        }

        surface = surfaceView!!.holder.surface
        scrcpyVM.surface = surface
        if (withNav) {
            val backButton = findViewById<View>(R.id.back_button) as Button
            val homeButton = findViewById<View>(R.id.home_button) as Button
            val appswitchButton = findViewById<View>(R.id.appswitch_button) as Button
            backButton.setOnClickListener {
                ADBUtils.exec(
                    "adb.bin-arm",
                    "-s",
                    device.toString(),
                    "shell", "input", "keyevent", "4"
                )
            }
            homeButton.setOnClickListener {
                ADBUtils.exec(
                    "adb.bin-arm",
                    "-s",
                    device.toString(),
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
                    device.toString(),
                    "shell",
                    "input",
                    "keyevent",
                    "187"
                )
            }
        }
        surfaceView!!.setOnTouchListener { _, event ->
            touchevent(
                event,
                scrcpyVM.screenWidth,
                scrcpyVM.screenHeight
            )
        }
    }


    private fun touchevent(touch_event: MotionEvent, videoW: Int, videoH: Int): Boolean {
        val buf = intArrayOf(
            touch_event.action,
            touch_event.buttonState,
            touch_event.x.toInt() * videoW / surfaceView!!.width,
            touch_event.y.toInt() * videoH / surfaceView!!.height
        )
        val array =
            ByteArray(buf.size * 4) // https://stackoverflow.com/questions/2183240/java-integer-to-byte-array
        for (j in buf.indices) {
            val c = buf[j]
            array[j * 4] = (c and -0x1000000 shr 24).toByte()
            array[j * 4 + 1] = (c and 0xFF0000 shr 16).toByte()
            array[j * 4 + 2] = (c and 0xFF00 shr 8).toByte()
            array[j * 4 + 3] = (c and 0xFF).toByte()
        }
        scrcpyVM.mActionQueue.offer(array)
        return true
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