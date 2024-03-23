package com.suda.androidscrcpy

import android.app.Application
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.suda.androidscrcpy.utils.ADBUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

const val STATUS_TURN_OFF = 0
const val STATUS_TURN_ON = 1
const val STATUS_WAIT = 2

class MainVM(ctx: Application) : AndroidViewModel(ctx) {

    val adbList = mutableStateListOf<String>()

    private val adbServerStatus = mutableIntStateOf(0)

    val adbInit = mutableStateOf(false)

    private var refreshJob: Job? = null

    init {
        init()
    }

    private fun init() {
        viewModelScope.launch {
            adbInit.value = false
            withContext(Dispatchers.IO) {
                ADBUtils.initEnv(getApplication())
            }
            adbInit.value = true
            startAdbServer()
        }
    }

    private fun startAdbDevices() {
        refreshJob = viewModelScope.launch {
            withContext(Dispatchers.IO) {
                while (true) {
                    val res = ADBUtils.exec("adb_termux", "devices")
                    val list = res.split("\n").toList()
                    adbList.clear()
                    if (list.size > 1) {
                        adbList.addAll(list.subList(1, list.size - 1))
                    }
                    delay(1000)
                }
            }
        }
    }

    fun startAdbServer() {
        if (adbServerStatus.value == STATUS_TURN_ON
            ||
            adbServerStatus.value == STATUS_WAIT
        ) {
            return
        }


        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                ADBUtils.exec("adb_termux", "start-server")
                adbServerStatus.value = STATUS_TURN_ON
                startAdbDevices()
            }
        }
    }

    fun stopAdbServer() {
        if (adbServerStatus.value == STATUS_TURN_OFF
            ||
            adbServerStatus.value == STATUS_WAIT
        ) {
            return
        }
        viewModelScope.launch {

            refreshJob?.cancelAndJoin()

            withContext(Dispatchers.IO) {
                ADBUtils.exec("adb_termux", "stop-server")
                adbServerStatus.value = STATUS_TURN_OFF
            }
        }
    }
}