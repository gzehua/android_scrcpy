package com.suda.androidscrcpy

import android.annotation.SuppressLint
import android.hardware.SensorManager
import android.os.Bundle
import android.view.MotionEvent
import android.view.Surface
import android.view.SurfaceView
import android.view.View
import android.widget.Button
import com.suda.androidscrcpy.decoder.VideoDecoder
import com.suda.androidscrcpy.model.ByteUtils
import com.suda.androidscrcpy.model.MediaPacket
import com.suda.androidscrcpy.model.VideoPacket
import com.suda.androidscrcpy.model.VideoPacket.StreamSettings
import com.suda.androidscrcpy.utils.ADBUtils
import com.suda.androidscrcpy.utils.ADBUtils.SC_DEVICE_SERVER_PATH
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean


class ScrcpyActivity : androidx.activity.ComponentActivity() {

    private val updateAvailable = AtomicBoolean(false)
    private val LetServceRunning = AtomicBoolean(true)
    private var videoDecoder: VideoDecoder? = null

    private var screenWidth = 0
    private var screenHeight = 0
    private val landscape = false
    private var first_time = true
    private val resultofRotation = false
    var sensorManager: SensorManager? = null

    private val inputStream: InputStream? = null
    private var surfaceView: SurfaceView? = null
    private var surface: Surface? = null

    private val device: String? by lazy {
        intent.getStringExtra("device")
    }

    private val withNav: Boolean by lazy {
        intent.getBooleanExtra("withNav", true)
    }

    private val mActionQueue = ConcurrentLinkedQueue<ByteArray>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        ADBUtils.exec(
            "adb.bin-arm",
            "-s",
            device.toString(),
            "push",
            "${ADBUtils.binPath}/scrcpy-server.jar",
            SC_DEVICE_SERVER_PATH
        )

        ADBUtils.exec(
            "adb.bin-arm",
            "-s",
            device.toString(),
            "reverse",
            "localabstract:scrcpy",
            "tcp:5005"
        )

        screenWidth = 1080
        screenHeight = 1920
        setUpUi(withNav)
        start()

        Thread {
            ADBUtils.exec2(
                "adb.bin-arm",
                "-s",
                device.toString(),
                "shell",
                "CLASSPATH=/data/local/tmp/scrcpy-server.jar app_process / com.genymobile.scrcpy.Server /127.0.0.1 1920 12288000",
            )
        }.start()
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
        surface = surfaceView!!.holder.surface
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
                surfaceView!!.width,
                surfaceView!!.height
            )
        }
    }


    private fun touchevent(touch_event: MotionEvent, displayW: Int, displayH: Int): Boolean {
        val buf = intArrayOf(
            touch_event.action,
            touch_event.buttonState,
            touch_event.x.toInt() * screenWidth / displayW,
            touch_event.y.toInt() * screenHeight / displayH
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
        mActionQueue.offer(array)
        return true
    }

    fun start() {
        val thread = Thread { startConnection() }
        thread.start()
    }

    private fun startConnection() {
        videoDecoder = VideoDecoder()
        videoDecoder!!.start()
        var dataInputStream: DataInputStream
        var dataOutputStream: DataOutputStream
        var socket: Socket? = null
        var streamSettings: StreamSettings? = null
        var attempts = 50
        var serverSocket :ServerSocket?=null
        while (attempts != 0) {
            try {
                serverSocket = ServerSocket(5005)
                socket = serverSocket.accept()
                dataInputStream = DataInputStream(socket.getInputStream())
                dataOutputStream = DataOutputStream(socket.getOutputStream())
                var packetSize: ByteArray
                attempts = 0
                while (LetServceRunning.get()) {
                    try {

                        var event = mActionQueue.poll()
                        while (event != null) {
                            dataOutputStream.write(event, 0, event!!.size)
                            event = mActionQueue.poll()
                        }

                        if (dataInputStream.available() > 0) {

                            packetSize = ByteArray(4)
                            dataInputStream.readFully(packetSize, 0, 4)
                            val size = ByteUtils.bytesToInt(packetSize)
                            val packet = ByteArray(size)
                            dataInputStream.readFully(packet, 0, size)
                            val videoPacket = VideoPacket.fromArray(packet)
                            if (videoPacket.type == MediaPacket.Type.VIDEO) {
                                val data = videoPacket.data
                                if (videoPacket.flag == VideoPacket.Flag.CONFIG || updateAvailable.get()) {
                                    if (!updateAvailable.get()) {
                                        streamSettings = VideoPacket.getStreamSettings(data)
                                        if (!first_time) {
                                            loadNewRotation()
                                            while (!updateAvailable.get()) {
                                                // Waiting for new surface
                                                try {
                                                    Thread.sleep(100)
                                                } catch (e: InterruptedException) {
                                                    e.printStackTrace()
                                                }
                                            }
                                        }
                                    }
                                    updateAvailable.set(false)
                                    first_time = false
                                    videoDecoder!!.configure(
                                        surface,
                                        screenWidth,
                                        screenHeight,
                                        streamSettings!!.sps,
                                        streamSettings.pps
                                    )
                                } else if (videoPacket.flag == VideoPacket.Flag.END) {
                                    // need close stream
                                } else {
                                    videoDecoder!!.decodeSample(
                                        data,
                                        0,
                                        data.size,
                                        0,
                                        videoPacket.flag.flag.toInt()
                                    )
                                }
                            }
                        }
                    } catch (e: IOException) {
                    } catch (e2:IllegalStateException){
                    }
                }
            } catch (e: IOException) {
                try {
                    attempts = attempts - 1
                    Thread.sleep(100)
                } catch (ignore: InterruptedException) {
                }
                //                 Log.e("Scrcpy", e.getMessage());
            } finally {
                if (serverSocket!=null){
                    try {
                        serverSocket.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
                if (socket != null) {
                    try {
                        socket.close()
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ADBUtils.exec(
            "adb.bin-arm",
            "-s",
            device.toString(),
            "reverse",
            "--remove-all"
        )
        LetServceRunning.set(false)
        videoDecoder?.stop()
    }

    fun loadNewRotation() {

    }

}