package com.bbip.bbipit.presentation.map.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 카테고리 프리셋 데이터 모델
data class CategoryPreset(
    val name: String,
    val value: String, // 서버 전송용 값
    val activeBorderColor: Color,
    val activeBgColor: Color,
    val activeTextColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryWriteSheet(
    isOpen: Boolean,
    onDismissRequest: () -> Unit,
    onSaveClick: (category: String, placeName: String, content: String) -> Unit,
) {
    if (!isOpen) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 입력 폼 상태 관리
    var selectedCategory by remember { mutableStateOf("🎙️ 무전") }
    var placeName by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    // 카테고리 목록 정의
    val categoryPresets = remember {
        listOf(
            CategoryPreset("🎙️ 무전", "무전", Color(0xFF956AFC), Color(0xFF956AFC).copy(alpha = 0.05f), Color(0xFF956AFC)),
            CategoryPreset("☕ 카페", "카페", Color(0xFFF59E0B), Color(0xFFFFFBEB), Color(0xFFD97706)),
            CategoryPreset("🍽️ 맛집", "음식", Color(0xFFF43F5E), Color(0xFFFFF1F2), Color(0xFFF43F5E)),
            CategoryPreset("🏃 운동", "운동", Color(0xFF10B981), Color(0xFFECFDF5), Color(0xFF10B981)),
            CategoryPreset("📸 사진", "사진", Color(0xFF0EA5E9), Color(0xFFE0F2FE), Color(0xFF0284C7)),
            CategoryPreset("🌟 일상", "일상", Color(0xFF6366F1), Color(0xFFEEF2FF), Color(0xFF6366F1))
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(
                    top = 28.dp,
                    bottom = maxOf(24.dp, WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding())
                ),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 상단 헤더
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "발자취 남기기",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "내 현재 위치에 소중한 추억을 핀으로 꽂아두세요.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // 닫기 버튼
                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFF1F5F9), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "닫기",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // 카테고리 선택 필드
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "카테고리",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 1.sp
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categoryPresets.forEach { preset ->
                        val isSelected = selectedCategory == preset.name
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isSelected) preset.activeBgColor else Color(0xFFF8FAFC),
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .border(
                                    width = 2.dp,
                                    color = if (isSelected) preset.activeBorderColor else Color(0xFFF1F5F9),
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .clickable { selectedCategory = preset.name }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = preset.name,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isSelected) preset.activeTextColor else Color(0xFF64748B)
                            )
                        }
                    }
                }
            }

            // 장소명 입력 필드
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "기록할 장소 이름",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 1.sp
                )
                OutlinedTextField(
                    value = placeName,
                    onValueChange = { placeName = it },
                    placeholder = { Text("예: 맛있는 돈카츠 가게, 한강 쉼터 등", color = Color(0xFFCBD5E1), fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155)),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color(0xFFF8FAFC).copy(alpha = 0.8f),
                        focusedBorderColor = Color(0xFF956AFC).copy(alpha = 0.2f),
                        unfocusedBorderColor = Color(0xFFF1F5F9),
                    )
                )
            }

            // 본문 내용 입력 필드
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "기록 내용",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 1.sp
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = { Text("이 위치에서의 추억이나 오늘 어떤 즐거운 일이 있었는지 적어보세요...",
                        color = Color(0xFFCBD5E1),
                        fontSize = 13.sp
                    ) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155), lineHeight = 20.sp),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color(0xFFF8FAFC).copy(alpha = 0.8f),
                        focusedBorderColor = Color(0xFF956AFC).copy(alpha = 0.2f),
                        unfocusedBorderColor = Color(0xFFF1F5F9),
                    )
                )
            }

            // 안내 배너
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = Color(0xFF956AFC).copy(alpha = 0.05f), shape = RoundedCornerShape(20.dp))
                    .border(width = 1.dp, color = Color(0xFF956AFC).copy(alpha = 0.1f), shape = RoundedCornerShape(20.dp))
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = "📸", fontSize = 20.sp)
                Text(
                    text = "카테고리에 맞는 전용 무드 감성 사진이\n자동 매핑되어 지도 마커 상세 카드에 아름답게 띄워집니다!",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF956AFC),
                    lineHeight = 14.sp
                )
            }

            // 저장 버튼
            Button(
                onClick = {
                    val mappedCategoryValue = categoryPresets.find { it.name == selectedCategory }?.value ?: "일반"
                    onSaveClick(mappedCategoryValue, placeName, content)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(32.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF956AFC))
            ) {
                Text(
                    text = "이 장소에 기록 남기기",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
        }
    }
}