package com.bbip.bbipit.presentation.map.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import dagger.hilt.android.AndroidEntryPoint


/**
 홈 스크린

 */
@Composable
fun MapScreen(navController: NavController) {
    Text("홈")
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