package com.suda.androidscrcpy.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.suda.androidscrcpy.MainVM

@Composable
fun Home(mainVM: MainVM) {
    Box(modifier = Modifier.fillMaxSize()) {
        val adbs = mainVM.adbList.toList()
        LazyColumn {
            items(adbs.size) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text(text = adbs[it], modifier = Modifier.align(Alignment.CenterStart))
                    Button(onClick = {

                    }, modifier = Modifier.align(Alignment.CenterEnd)) {
                        Text(text = "连接")
                    }
                }
            }
        }
    }
}