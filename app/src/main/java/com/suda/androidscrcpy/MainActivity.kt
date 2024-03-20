package com.suda.androidscrcpy

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.suda.androidscrcpy.ui.theme.AndroidScrcpyTheme
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths


class MainActivity : ComponentActivity() {

    val androidFiles = arrayListOf(
        "adb.bin-arm.so",
        "libadb.so",
        "libadb_termux.so",
        "libabsl_bad_variant_access.so",
        "libabsl_base.so",
        "libabsl_city.so",
        "libabsl_cord.so",
        "libabsl_cord_internal.so",
        "libabsl_cordz_functions.so",
        "libabsl_cordz_handle.so",
        "libabsl_cordz_info.so",
        "libabsl_crc32c.so",
        "libabsl_crc_cord_state.so",
        "libabsl_crc_internal.so",
        "libabsl_die_if_null.so",
        "libabsl_examine_stack.so",
        "libabsl_exponential_biased.so",
        "libabsl_hash.so",
        "libabsl_int128.so",
        "libabsl_log_globals.so",
        "libabsl_log_internal_check_op.so",
        "libabsl_log_internal_format.so",
        "libabsl_log_internal_globals.so",
        "libabsl_log_internal_log_sink_set.so",
        "libabsl_log_internal_message.so",
        "libabsl_log_internal_nullguard.so",
        "libabsl_log_internal_proto.so",
        "libabsl_log_sink.so",
        "libabsl_low_level_hash.so",
        "libabsl_malloc_internal.so",
        "libabsl_raw_hash_set.so",
        "libabsl_raw_logging_internal.so",
        "libabsl_spinlock_wait.so",
        "libabsl_stacktrace.so",
        "libabsl_status.so",
        "libabsl_statusor.so",
        "libabsl_str_format_internal.so",
        "libabsl_strerror.so",
        "libabsl_strings.so",
        "libabsl_strings_internal.so",
        "libabsl_symbolize.so",
        "libabsl_synchronization.so",
        "libabsl_throw_delegate.so",
        "libabsl_time.so",
        "libabsl_time_zone.so",
        "libbrotlicommon.so",
        "libbrotlidec.so",
        "libbrotlienc.so",
        "libc++_shared.so",
        "liblz4.so",
        "libprotobuf.so",
        "libusb-1.0.so",
        "libz.so.1",
        "libzstd.so.1",
        "libtermux-api.so",
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        val libFolder = "${applicationInfo.nativeLibraryDir}"

        val binFolder = File("/data/data/${packageName}/files/usr/", "bin")
        if (!binFolder.exists()) {
            binFolder.mkdirs()
        }
        for (so in androidFiles) {


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                File("$binFolder/$so").delete()
                val target: Path = Paths.get("$libFolder/${so}.so")
                val link: Path = Paths.get("$binFolder/$so")
                Files.createSymbolicLink(link, target)
            }

//            File("binFolder/")
        }

//        binFolder.listFiles().forEach {
//            Log.d("ADB",it.absolutePath)
//        }

        File(binFolder, "adb").writeText("$libFolder/libadb.so.so \\\$@")
        Runtime.getRuntime().exec("chmod +x ${File(binFolder, "adb")}").waitFor()
        exec(binFolder.absolutePath, libFolder)

        setContent {
            AndroidScrcpyTheme {
                // A surface container using the "background" color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Greeting("Android")
                }
            }
        }
    }
}

fun exec(binPath: String, nativePath: String) {
    try {

        val pb = ProcessBuilder("$nativePath/adb.bin-arm.so.so","start-server")
        pb.environment()["PATH"] = "${binPath}:${nativePath}:${pb.environment()["PATH"]}"
        pb.environment()["TMPDIR"] = binPath
        pb.environment()["HOME"] = binPath
        pb.environment()["LD_LIBRARY_PATH"] = binPath
        pb.directory(File(binPath))
        // 执行命令
        val process = pb.start()


        // 获取命令的输出流
        val reader = BufferedReader(InputStreamReader(process.inputStream))
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            // 输出命令的每一行结果
            Log.d("ADB", "$line")
        }

        // 等待命令执行完成
        val exitCode = process.waitFor()
        println("命令执行完毕，退出码：$exitCode")
    } catch (e: IOException) {
        e.printStackTrace()
    } catch (e: InterruptedException) {
        e.printStackTrace()
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AndroidScrcpyTheme {
        Greeting("Android")
    }
}