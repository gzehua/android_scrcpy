package com.suda.androidscrcpy.home

import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.suda.androidscrcpy.MainVM
import com.suda.androidscrcpy.ScrcpyActivity

@Composable
fun Home(mainVM: MainVM) {
    var showDialog by remember { mutableStateOf(false) }
    var ip by remember { mutableStateOf("192.168.31.138") }
    var port by remember { mutableStateOf("5555") }

    if (showDialog) {
        Dialog(
            onDismissRequest = { showDialog = false },
        ) {
            Card {
                Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "IP", Modifier.width(60.dp))
                        TextField(value = ip, onValueChange = { ip = it })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "PORT", Modifier.width(60.dp))
                        TextField(value = port, onValueChange = { port = it })
                    }
                    Button(onClick = {
                        mainVM.connect(ip, port)
                        showDialog = false
                    }) {
                        Text(text = "添加")
                    }
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Button(onClick = {
            showDialog = true
        }) {
            Text(text = "添加设备")
        }
        Box(modifier = Modifier.fillMaxSize()) {
            val adbs = mainVM.adbList.toList()
            LazyColumn {
                items(adbs.size) {
                    val ctx = LocalView.current.context

                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text(text = adbs[it], modifier = Modifier.align(Alignment.CenterStart))
                        Button(onClick = {
                            ctx.startActivity(Intent(ctx, ScrcpyActivity::class.java).apply {
                                putExtra("device", adbs[it].split("\t")[0])
                                putExtra("withNav", true)
                            })
                        }, modifier = Modifier.align(Alignment.CenterEnd)) {
                            Text(text = "连接")
                        }
                    }
                }
            }
        }
    }
}