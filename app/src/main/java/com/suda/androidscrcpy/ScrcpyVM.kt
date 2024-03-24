package com.suda.androidscrcpy

import android.app.Application
import android.content.pm.ActivityInfo
import android.hardware.SensorManager
import android.view.Surface
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.suda.androidscrcpy.decoder.VideoDecoder
import com.suda.androidscrcpy.model.ByteUtils
import com.suda.androidscrcpy.model.MediaPacket
import com.suda.androidscrcpy.model.VideoPacket
import com.suda.androidscrcpy.utils.ADBUtils
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

class ScrcpyVM(app: Application) : AndroidViewModel(app) {

    val updateAvailable = AtomicBoolean(false)
    val LetServceRunning = AtomicBoolean(true)
    var videoDecoder: VideoDecoder? = null

    private val _rotationLiveData = MutableLiveData<Int>()
    val rotationLiveData: LiveData<Int> = _rotationLiveData

    var screenWidth = 0
    var screenHeight = 0
    val landscape = false
    var first_time = true

    val resultofRotation = false
    var sensorManager: SensorManager? = null
    var surface: Surface? = null
        set(value) {
            if (field != null) {
                updateAvailable.set(true)
            }
            field = value
        }
    lateinit var device: String


    val inputStream: InputStream? = null
    val mActionQueue = ConcurrentLinkedQueue<ByteArray>()

    fun init(device: String) {
        if (!first_time) {
            return
        }
        this.device = device
        ADBUtils.exec(
            "adb.bin-arm",
            "-s",
            device.toString(),
            "push",
            "${ADBUtils.binPath}/scrcpy-server.jar",
            ADBUtils.SC_DEVICE_SERVER_PATH
        )

        ADBUtils.exec(
            "adb.bin-arm",
            "-s",
            device.toString(),
            "reverse",
            "localabstract:scrcpy",
            "tcp:5005"
        )

        val maxSize = 1920
        computeScreenInfo(maxSize)
        start()
        Thread {
            ADBUtils.exec2(
                "adb.bin-arm",
                "-s",
                device.toString(),
                "shell",
                "CLASSPATH=/data/local/tmp/scrcpy-server.jar app_process / com.genymobile.scrcpy.Server /127.0.0.1 $maxSize 6144000",
            )
        }.start()
    }

    private fun computeScreenInfo(maxSize: Int) {
        val wh = ADBUtils.exec(
            "adb.bin-arm",
            "-s",
            device.toString(),
            "shell",
            "wm size"
        ).replace("Physical size: ", "").split("x")

        var w: Int = wh[0].trimStart().toInt() and 7.inv() // in case it's not a multiple of 8
        var h: Int = wh[1].toInt() and 7.inv()
        if (maxSize > 0) {
            val portrait = h > w
            var major = if (portrait) h else w
            var minor = if (portrait) w else h
            if (major > maxSize) {
                val minorExact = minor * maxSize / major
                // +4 to round the value to the nearest multiple of 8
                minor = minorExact + 4 and 7.inv()
                major = maxSize
            }
            w = if (portrait) minor else major
            h = if (portrait) major else minor
        }
        screenWidth = w
        screenHeight = h
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
        var streamSettings: VideoPacket.StreamSettings? = null
        var attempts = 50
        var serverSocket: ServerSocket? = null
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
                    } catch (e2: Exception) {
                        e2.printStackTrace()
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
                if (serverSocket != null) {
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

    override fun onCleared() {
        super.onCleared()
        ADBUtils.exec(
            "adb.bin-arm",
            "-s",
            device.toString(),
            "reverse",
            "--remove-all"
        )
        LetServceRunning.set(false)
        runCatching {
            videoDecoder?.stop()
        }
    }


    fun loadNewRotation() {
        val temp = screenHeight
        screenHeight = screenWidth
        screenWidth = temp
        if (screenWidth > screenHeight) {
            _rotationLiveData.postValue(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        } else {
            _rotationLiveData.postValue(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        }
    }

}
