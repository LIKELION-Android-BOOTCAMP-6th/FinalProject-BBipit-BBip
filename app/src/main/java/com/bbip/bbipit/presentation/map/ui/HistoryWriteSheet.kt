package com.bbip.bbipit.presentation.map.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 카테고리 선택 구성 데이터 모델
data class CategoryPreset(
    val name: String,
    val value: String, // 서버 전송용 식별값
    val activeBorderColor: Color,
    val activeBgColor: Color,
    val activeTextColor: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryWriteSheet(
    isOpen: Boolean,
    isLoading: Boolean,
    onDismissRequest: () -> Unit,
    onSaveClick: (category: String, placeName: String, content: String) -> Unit,
    selectedImages: List<ByteArray>, // 선택된 이미지 데이터 리스트
    onImagesSelected: (List<ByteArray>) -> Unit // 이미지 선택 완료 콜백
) {
    if (!isOpen) return

    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 키보드 및 포커스 제어를 위한 유틸리티
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // 텍스트 필드들의 포커스를 제어
    val dummyFocusRequester = remember { FocusRequester() }

    var selectedCategory by remember { mutableStateOf("🎙️ 무전") }
    var placeName by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    // 이미지 파일 선택용 시스템 갤러리 계약 등록 (최대 3장 제한)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 3)
    ) { uris ->
        // 갤러리에서 복귀할 때 기존 TextField의 포커스를 제거
        dummyFocusRequester.requestFocus()
        focusManager.clearFocus()
        keyboardController?.hide()

        if (uris.isNotEmpty()) {
            val byteArrayList = uris.mapNotNull { uri ->
                try {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.readBytes()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }
            onImagesSelected(byteArrayList)
        }
    }

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

    Box(modifier = Modifier.fillMaxSize()) {
        ModalBottomSheet(
            onDismissRequest  = {
                if (!isLoading) onDismissRequest()
            },
            sheetState = sheetState,
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
            dragHandle = null,
            // 바텀시트 본체 컴포저블 영역에 직접 클릭 이벤트를 심어 바깥 터치 시 포커스를 풀도록 유도
            modifier = Modifier.clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                dummyFocusRequester.requestFocus()
                focusManager.clearFocus()
                keyboardController?.hide()
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.9f)
                    .padding(horizontal = 24.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        // 시트 내부 빈 공간 터치 시에도 동일하게 처리
                        dummyFocusRequester.requestFocus()
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    }
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(
                        top = 28.dp,
                        bottom = maxOf(
                            24.dp,
                            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                        )
                    ),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // 화면상에는 보이지 않는 투명한 컴포저블에 포커스를 장착
                Box(
                    modifier = Modifier
                        .size(1.dp)
                        .focusRequester(dummyFocusRequester)
                        .focusable()
                )

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
                                        color = if (isSelected) preset.activeBgColor else Color(
                                            0xFFF8FAFC
                                        ),
                                        shape = RoundedCornerShape(24.dp)
                                    )
                                    .border(
                                        width = 2.dp,
                                        color = if (isSelected) preset.activeBorderColor else Color(
                                            0xFFF1F5F9
                                        ),
                                        shape = RoundedCornerShape(24.dp)
                                    )
                                    .clickable {
                                        selectedCategory = preset.name
                                    }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = preset.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isSelected) preset.activeTextColor else Color(
                                        0xFF64748B
                                    )
                                )
                            }
                        }
                    }
                }

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
                        placeholder = {
                            Text(
                                "예: 맛있는 돈카츠 가게, 한강 쉼터 등",
                                color = Color(0xFFCBD5E1),
                                fontSize = 13.sp
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155)
                        ),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color(0xFFF8FAFC).copy(alpha = 0.8f),
                            focusedBorderColor = Color(0xFF956AFC).copy(alpha = 0.2f),
                            unfocusedBorderColor = Color(0xFFF1F5F9),
                        )
                    )
                }

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
                        placeholder = {
                            Text(
                                "이 위치에서의 추억이나 오늘 어떤 즐거운 일이 있었는지 적어보세요...",
                                color = Color(0xFFCBD5E1),
                                fontSize = 13.sp
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155),
                            lineHeight = 20.sp
                        ),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color(0xFFF8FAFC).copy(alpha = 0.8f),
                            focusedBorderColor = Color(0xFF956AFC).copy(alpha = 0.2f),
                            unfocusedBorderColor = Color(0xFFF1F5F9),
                        )
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = "스토리 사진 첨부",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF94A3B8),
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "최대 3장까지 업로드 가능",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF956AFC)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 이미지 프리뷰 목록 구성 및 시스템 갤러리 연동
                        repeat(3) { index ->
                            val imageBytes = selectedImages.getOrNull(index)

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .background(Color(0xFFF8FAFC), RoundedCornerShape(16.dp))
                                    .border(
                                        width = 1.5.dp,
                                        color = Color(0xFFF1F5F9),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable {
                                        // 갤러리 호출 직전에 안전하게 포커스 분산
                                        dummyFocusRequester.requestFocus()
                                        focusManager.clearFocus()
                                        keyboardController?.hide()

                                        galleryLauncher.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (imageBytes != null) {
                                    val bitmap = remember(imageBytes) {
                                        android.graphics.BitmapFactory.decodeByteArray(
                                            imageBytes,
                                            0,
                                            imageBytes.size
                                        )
                                    }
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "선택된 이미지 프리뷰",
                                        modifier = Modifier.fillMaxSize()
                                            .clip(RoundedCornerShape(16.dp)),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "사진 추가",
                                            tint = Color(0xFFCBD5E1),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 저장 버튼
                Button(
                    onClick = {
                        val mappedCategoryValue =
                            categoryPresets.find { it.name == selectedCategory }?.value ?: "일반"
                        onSaveClick(mappedCategoryValue, placeName, content)
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(32.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF956AFC),
                        disabledContainerColor = Color(0xFF956AFC).copy(alpha = 0.6f),
                        disabledContentColor = Color.White
                    )
                ) {
                    if (isLoading) {
                        // 로딩 중일 때는 흰색 스피너 표시
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.5.dp
                        )
                    } else {
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
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {},
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color(0xFF956AFC),
                            strokeWidth = 2.5.dp
                        )
                        Text(
                            text = "소중한 발자취를 서버에 심는 중...",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF334155)
                        )
                    }
                }
            }
        }
    }
}