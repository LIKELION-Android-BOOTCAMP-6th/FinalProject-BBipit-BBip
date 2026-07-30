package com.bbip.bbipit.presentation.permission

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun ServiceRestrictedScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val mainColor: Color = Color(0xFF956AFC)
    val mainAppBg: Color = Color.White

    // Tailwind Slate Colors
    val slate200 = Color(0xFFE2E8F0)
    val slate400 = Color(0xFF94A3B8)
    val slate500 = Color(0xFF64748B)
    val slate900 = Color(0xFF0F172A)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(mainAppBg)
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // 경고 아이콘 영역
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(slate200.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.WarningAmber,
                    contentDescription = "Warning",
                    tint = slate400,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // 제목
            Text(
                text = "서비스 이용 제한",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = slate900,
                letterSpacing = (-0.5).sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 설명
            Text(
                text = "원활한 무전 서비스 이용을 위해\n위치, 알림 및 마이크 권한이 반드시 필요합니다.\n기기 설정에서 권한을 허용해 주세요.",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = slate500,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            // 버튼 영역
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 권한 설정 버튼
                Button(
                    onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .shadow(
                            elevation = 8.dp,
                            shape = RoundedCornerShape(32.dp),
                            ambientColor = mainColor,
                            spotColor = mainColor
                        ),
                    colors = ButtonDefaults.buttonColors(containerColor = mainColor),
                    shape = RoundedCornerShape(32.dp)
                ) {
                    Text(
                        text = "권한 설정하러 가기",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                // 앱 종료 버튼
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(slate200.copy(alpha = 0.6f))
                        .clickable(onClick = {
                            activity?.finish()
                        }),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "앱 종료",
                        color = slate500,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}