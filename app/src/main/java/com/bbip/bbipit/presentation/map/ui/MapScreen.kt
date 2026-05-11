package com.bbip.bbipit.presentation.map.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bbip.bbipit.core.component.BackgroundBox


/**
 홈 스크린

 */
@Composable
fun MapScreen(navController: NavController) {
    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        BackgroundBox {
            Column(
                modifier = Modifier
                    .padding(innerPadding) //이너 패딩 필수 안 그러면 상태바랑 겹침
                    .padding(10.dp)
            ) {
                Text("홈")

            }
        }
    }
}

//                            val seoul = LatLng(37.5665, 126.9780)
//                            val cameraPositionState = rememberCameraPositionState {
//                                position = CameraPosition.fromLatLngZoom(seoul, 12f)
//                            }
//
//                            GoogleMap(
//                                modifier = Modifier.fillMaxSize(),
//                                cameraPositionState = cameraPositionState
//                            )