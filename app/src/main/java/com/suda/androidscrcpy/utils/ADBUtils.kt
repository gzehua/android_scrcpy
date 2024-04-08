package com.suda.androidscrcpy.utils

import android.app.Application
import android.os.Build
import com.termux.shared.logger.Logger
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object ADBUtils {

    val SC_DEVICE_SERVER_PATH = "/data/local/tmp/scrcpy-server.jar"


    private lateinit var context: Application
    lateinit var binPath: String
        private set
    private lateinit var libPath: String

    private val libBinFile = arrayListOf(
        "adb_termux"
    )

    private val assetFile = arrayListOf(
        "am",
        "am.apk",
        "termux-api",
        "termux-toast",
        "termux-usb",
        "termux-vibrate",
        "scrcpy-server.jar"
    )

    private val libSoFile = arrayListOf(
        "libtermux-api.so",
    )


    fun exec(binName: String, vararg args: String): String {
        val sb = StringBuilder()
        val pb = ProcessBuilder("$binPath/$binName", *args)

        try {
            pb.environment()["PATH"] = "${binPath}"
            pb.environment()["TMPDIR"] = "${binPath}"
            pb.environment()["HOME"] = binPath
            pb.environment()["LD_LIBRARY_PATH"] = "${binPath}"
//            pb.environment()["RUST_LOG"] = "debug termux-adb devices";
//            pb.environment()["TERMUX_EXPORT_FD"] = "true";
            pb.redirectErrorStream(true)

            // 执行命令
            val process = pb.start()

            // 获取命令的输出流
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                // 输出命令的每一行结果
                Logger.logDebug("$line")
                sb.append(line).append("\n")
            }
            if (sb.isNotEmpty()) {
                sb.deleteCharAt(sb.length - 1)
            }
            // 等待命令执行完成
            val exitCode = process.waitFor()
            Logger.logDebug("命令执行完毕，退出码：$exitCode")
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        Logger.logDebug(pb.command().joinToString(" ") + "|" + sb.toString())
        return sb.toString()
    }

    fun pair(binName: String, ipPort: String, code: String): String {
        val sb = StringBuilder()
        val pb = ProcessBuilder("$binPath/$binName", "pair", ipPort)

        try {
            pb.environment()["PATH"] = "${binPath}"
            pb.environment()["TMPDIR"] = "${binPath}"
            pb.environment()["HOME"] = binPath
            pb.environment()["LD_LIBRARY_PATH"] = "${binPath}"
            pb.redirectErrorStream(true)

            // 执行命令
            val process = pb.start()

            val dataOutputStream = DataOutputStream(process.outputStream)
            dataOutputStream.writeBytes(code)
            dataOutputStream.writeBytes("\n")
            dataOutputStream.flush()

            // 获取命令的输出流
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                // 输出命令的每一行结果
                Logger.logDebug("$line")
                sb.append(line).append("\n")
            }
            if (sb.isNotEmpty()) {
                sb.deleteCharAt(sb.length - 1)
            }
            // 等待命令执行完成
            val exitCode = process.waitFor()
            Logger.logDebug("命令执行完毕，退出码：$exitCode")
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        Logger.logDebug(pb.command().joinToString(" ") + "|" + sb.toString())
        return sb.toString()
    }


    fun exec2(binName: String, vararg args: String): String {
        val sb = StringBuilder()
        val pb = ProcessBuilder("$binPath/$binName", *args)

        try {
            pb.environment()["PATH"] = "${binPath}"
            pb.environment()["TMPDIR"] = "${binPath}"
            pb.environment()["HOME"] = binPath
            pb.environment()["LD_LIBRARY_PATH"] = "${binPath}"
//            pb.environment()["RUST_LOG"] = "debug termux-adb devices";
//            pb.environment()["TERMUX_EXPORT_FD"] = "true";
            pb.redirectErrorStream(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                pb.redirectOutput(File(binPath, "out.log"))
                pb.redirectError(File(binPath, "err.log"))
            }
            // 执行命令
            val process = pb.start()

            // 获取命令的输出流
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                // 输出命令的每一行结果
                Logger.logDebug("$line")
                sb.append(line).append("\n")
            }
            if (sb.isNotEmpty()) {
                sb.deleteCharAt(sb.length - 1)
            }
            // 等待命令执行完成
            val exitCode = process.waitFor()

            try {
                Logger.logError(FileReader(File(binPath, "out.log")).readText())

            } catch (e: Exception) {
                e.printStackTrace()
            }

            Logger.logDebug("命令执行完毕，退出码：$exitCode")
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        Logger.logDebug(pb.command().joinToString(" ") + "|" + sb.toString())
        return sb.toString()
    }


    fun initEnv(context: Application) {
        this.context = context
        val libFolder = "${context.applicationInfo.nativeLibraryDir}"
        val binFolder = File("/data/data/${context.packageName}/files/usr/", "bin")
        if (!binFolder.exists()) {
            binFolder.mkdirs()
        }
        binPath = binFolder.absolutePath
        libPath = libFolder
        for (so in libBinFile) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                File("$binFolder/$so").delete()
                val target: Path = Paths.get("$libFolder/${so}.so")
                val link: Path = Paths.get("$binFolder/$so")
                Files.createSymbolicLink(link, target)
            }
        }
        for (so in libSoFile) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                File("$binFolder/$so").delete()
                val target: Path = Paths.get("$libFolder/${so}")
                val link: Path = Paths.get("$binFolder/$so")
                Files.createSymbolicLink(link, target)
            }
        }
        for (bin in assetFile) {
            AssetsUtil.copyAssetFileToInternalStorage(
                context.assets,
                "$bin",
                "$binFolder${File.separator}$bin"
            )
            Runtime.getRuntime().exec("chmod +x ${File(binFolder, "$bin")}").waitFor()
        }

        if (!File("${binFolder.parentFile}/libexec").exists()) {
            File("${binFolder.parentFile}/libexec").mkdirs()
        }
        AssetsUtil.copyAssetFileToInternalStorage(
            context.assets,
            "termux-callback",
            "${binFolder.parentFile}/libexec/termux-callback"
        )
        Runtime.getRuntime()
            .exec("chmod +x ${File("${binFolder.parentFile}/libexec", "termux-callback")}")
            .waitFor()
    }
}
