package com.suda.androidscrcpy

import android.app.ActivityOptions
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.suda.androidscrcpy.home.Home
import com.suda.androidscrcpy.ui.theme.AndroidScrcpyTheme


class MainActivity : ComponentActivity() {


    private val mainVM: MainVM by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)



        setContent {
            AndroidScrcpyTheme {
                // A surface container using the "background" color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    val adbInit by remember {
                        mainVM.adbInit
                    }
                    if (adbInit) {
                        Home(mainVM)
                        LaunchedEffect(key1 = null) {
                            repeatOnLifecycle(Lifecycle.State.RESUMED){
                                mainVM.startAdbDevices()
                            }
                        }
                    } else {
                        Greeting("初始化中")
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //杀死进程防止termux_api 起不来
        android.os.Process.killProcess(android.os.Process.myPid())
    }
}

@Composable
fun Greeting(name: String) {
    Box(modifier = Modifier.fillMaxSize()) {
        Text(
            text = name,
            modifier = Modifier.align(alignment = Alignment.Center)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    AndroidScrcpyTheme {
        Greeting(name = "ADB")
    }
}