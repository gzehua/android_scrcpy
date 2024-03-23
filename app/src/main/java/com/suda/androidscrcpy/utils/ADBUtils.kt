package com.suda.androidscrcpy.utils

import android.app.Application
import android.os.Build
import com.termux.shared.logger.Logger
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object ADBUtils {

    private lateinit var context: Application
    private lateinit var binPath: String
    private lateinit var libPath: String

    private val libBinFile = arrayListOf(
        "adb.bin-arm",
        "adb_termux",
        "libz.so.1",
        "libzstd.so.1",
    )

    private val assetFile = arrayListOf(
        "am",
        "am.apk",
        "termux-api",
        "termux-toast",
        "termux-usb",
        "termux-vibrate",
    )


    fun exec(binName: String, vararg args: String): String {
        val sb = StringBuilder()
        try {
            val pb = ProcessBuilder("$binPath/$binName", *args)
            pb.environment()["PATH"] = "${binPath}:${pb.environment()["PATH"]}"
            pb.environment()["TMPDIR"] = "${binPath}"
            pb.environment()["HOME"] = binPath
            pb.environment()["LD_LIBRARY_PATH"] = "${binPath}:${libPath}"
//            pb.environment()["RUST_LOG"] = "debug termux-adb devices";
            pb.environment()["TERMUX_EXPORT_FD"] = "true";

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
