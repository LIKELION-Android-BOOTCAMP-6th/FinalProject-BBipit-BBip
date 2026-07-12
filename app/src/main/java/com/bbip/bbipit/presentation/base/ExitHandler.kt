package com.bbip.bbipit.presentation.base

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.bbip.bbipit.core.extension.findActivity

@Composable
fun ExitHandler(message: String = "한 번 더 누르면 종료됩니다.", interval: Long = 2000){
    val context = LocalContext.current.findActivity()

    var backPressedTime by remember { mutableLongStateOf(0L) }
    BackHandler {
        if(System.currentTimeMillis() - backPressedTime <= interval){
            context?.finish()
        } else{
            backPressedTime = System.currentTimeMillis()
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}