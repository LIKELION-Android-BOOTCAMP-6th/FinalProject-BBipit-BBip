package com.bbip.bbipit.presentation.permission

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController

@Composable
fun PermissionRequestScreen(
    onGrantPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    // 앱의 메인 테마 색상 (Tailwind의 text-[#956AFC])
    val mainColor = Color(0xFF956AFC)

    // 다이얼로그나 오버레이 형태로 띄우기 위해 Dialog 사용
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x660F172A)) // bg-slate-900/40 (투명도 약 40%)
                .padding(24.dp), // p-6
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(40.dp)) // rounded-[2.5rem]
                    .background(Color.White)
                    .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(40.dp)) // border-slate-100
                    .padding(28.dp) // p-7
            ) {
                // 상단 경고 아이콘
                Box(
                    modifier = Modifier
                        .size(56.dp) // w-14 h-14
                        .clip(RoundedCornerShape(16.dp)) // rounded-2xl
                        .background(Color(0xFFFFF1F2)) // bg-rose-50
                        .border(1.dp, Color(0xFFFFE4E6), RoundedCornerShape(16.dp)), // border-rose-100
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.WarningAmber,
                        contentDescription = "Warning",
                        tint = Color(0xFFF43F5E), // text-rose-500
                        modifier = Modifier.size(28.dp) // w-7 h-7
                    )
                }

                Spacer(modifier = Modifier.height(20.dp)) // mb-5

                // 제목
                Text(
                    text = "권한 허용이 필요해요!",
                    fontSize = 20.sp, // text-[20px]
                    fontWeight = FontWeight.Black, // font-black
                    color = Color(0xFF0F172A), // text-slate-900
                    letterSpacing = (-0.5).sp // tracking-tight
                )

                Spacer(modifier = Modifier.height(8.dp)) // mb-2

                val descriptionText = buildAnnotatedString {
                    append("핵심 기능인 ")
                    withStyle(style = SpanStyle(color = mainColor)) {
                        append("지도 확인")
                    }
                    append(", ")
                    withStyle(style = SpanStyle(color = mainColor)) {
                        append("실시간 알림")
                    }
                    append(" 및 ")
                    withStyle(style = SpanStyle(color = mainColor)) {
                        append("실시간 무전")
                    }
                    append("을 위해\n아래 권한을 꼭 허용해 주세요.")
                }

                Text(
                    text = descriptionText,
                    fontSize = 13.sp, // text-[13px]
                    fontWeight = FontWeight.Bold, // font-bold
                    color = Color(0xFF64748B), // text-slate-500
                    lineHeight = 20.sp // leading-relaxed
                )

                Spacer(modifier = Modifier.height(24.dp)) // mb-6

                // 권한 목록 컨테이너
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp)) // rounded-[1.5rem]
                        .background(Color(0xFFF8FAFC)) // bg-slate-50
                        .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(24.dp)) // border-slate-100
                        .padding(16.dp), // p-4
                    verticalArrangement = Arrangement.spacedBy(12.dp) // space-y-3
                ) {
                    // 위치 권한 항목
                    PermissionItem(
                        icon = Icons.Outlined.LocationOn,
                        iconTint = Color(0xFF3B82F6), // text-blue-500
                        title = "위치 (필수)",
                        subtitle = "친구에게 내 위치를 표시하기 위해 필요해요. \n(정확한 위치 표시를 위해 앱이 사용 중이 아닌 상태에서도 수집됩니다.)"
                    )

                    // 알림 권한 항목
                    PermissionItem(
                        icon = Icons.Outlined.Notifications,
                        iconTint = mainColor, // text-[#956AFC]
                        title = "알림 (필수)",
                        subtitle = "무전이 만료되기 전 재빠르게 알려드릴게요. \n(무전, 친구 요청 알림 등이 표시됩니다.)"
                    )

                    // 💡 [추가] 마이크 권한 레이아웃 항목 반영
                    PermissionItem(
                        icon = Icons.Filled.Mic,
                        iconTint = Color(0xFF10B981), // 테일윈드 green-500 계열 색상 매핑
                        title = "마이크 (필수)",
                        subtitle = "친구들과 실시간 음성 무전 송신"
                    )
                }

                Spacer(modifier = Modifier.height(32.dp)) // mb-8

                // 버튼 영역
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp) // gap-2
                ) {
                    Button(
                        onClick = onGrantPermission,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp) // py-4의 대략적인 높이
                            .shadow(
                                elevation = 8.dp, // shadow-lg
                                shape = RoundedCornerShape(28.dp),
                                ambientColor = mainColor,
                                spotColor = mainColor
                            ),
                        colors = ButtonDefaults.buttonColors(containerColor = mainColor),
                        shape = RoundedCornerShape(28.dp) // rounded-[1.8rem]
                    ) {
                        Text(
                            text = "설정에서 권한 허용하기",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(28.dp))
                            .background(Color.White)
                            .clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "다음에 할게요",
                            color = Color(0xFF94A3B8), // text-slate-400
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// 반복되는 권한 항목을 위한 내부 컴포저블 함수
@Composable
fun PermissionItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp) // gap-3
    ) {
        // 아이콘 배경 박스
        Box(
            modifier = Modifier
                .size(40.dp) // w-10 h-10
                .clip(RoundedCornerShape(12.dp)) // rounded-xl
                .background(Color.White)
                .border(1.dp, Color(0xFFF1F5F9), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp) // w-5 h-5
            )
        }

        // 텍스트 영역
        Column {
            Text(
                text = title,
                fontSize = 13.sp, // text-[13px]
                fontWeight = FontWeight.Black, // font-black
                color = Color(0xFF1E293B) // text-slate-800
            )
            Spacer(modifier = Modifier.height(2.dp)) // mt-0.5
            Text(
                text = subtitle,
                fontSize = 11.sp, // text-[11px]
                fontWeight = FontWeight.Bold, // font-bold
                color = Color(0xFF94A3B8) // text-slate-400
            )
        }
    }
}
