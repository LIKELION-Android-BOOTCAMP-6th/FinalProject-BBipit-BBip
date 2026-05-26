package com.bbip.bbipit.presentation.user.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.bbip.bbipit.domain.type.LoginType

@Composable
fun SettingsDrawer(email: String, LoginType: String, onClose: () -> Unit, modifier: Modifier) {
    ModalDrawerSheet(modifier = modifier) {
        Column(modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text("드로어블 열림")
        }
    }

}