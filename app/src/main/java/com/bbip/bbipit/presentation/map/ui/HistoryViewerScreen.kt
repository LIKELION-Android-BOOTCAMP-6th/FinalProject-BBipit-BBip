package com.bbip.bbipit.presentation.map.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.bbip.bbipit.R
import com.bbip.bbipit.core.ui.theme.recording
import com.bbip.bbipit.domain.entity.History
import com.bbip.bbipit.domain.entity.HistoryComment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.widget.Toast
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.bbip.bbipit.presentation.map.viewmodel.HistoryUiState
import com.bbip.bbipit.presentation.map.viewmodel.HistoryViewModel

// 단일 이미지 단위 타임라인 조각 모델
data class StoryItem(
    val history: History,
    val imageUrl: String?,
    val imageIndex: Int,
    val totalImagesInHistory: Int
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryViewerScreen(
    myUid: String = "",
    histories: List<History>, // 전체 히스토리 리스트 스트림
    initialHistoryId: String, // 초기 진입 히스토리 식별자
    comments: List<HistoryComment>,
    isFromFriendList: Boolean = false,
    viewModel: HistoryViewModel,
    onHistoryChanged: (String) -> Unit, // 히스토리 변경 콜백 (댓글 리스너 갱신용)
    onDismiss: () -> Unit,
    onLikeToggle: (History) -> Unit,
    onCommentSubmit: (String, String) -> Unit,
    onDeleteClick: (String) -> Unit,
    onHistoryUpdate: (String, String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    val listState = rememberLazyListState()

    val filteredHistories = remember(histories, initialHistoryId, isFromFriendList) {
        val initialHistory = histories.find { it.id == initialHistoryId } ?: return@remember emptyList()

        if (isFromFriendList) {
            // [조건 A - 친구 화면에서 진입]: 섞여 들어온 전체 스트림에서 클릭한 해당 친구(userId)가 작성한 '모든' 최근 발자취 필터링
            histories.filter { it.userId == initialHistory.userId }
        } else {
            // [조건 B - 맵 화면에서 진입]: 전체 스트림에서 오직 내가 마커로 선택한 '해당 히스토리 1개'만 독립 노출
            histories.filter { it.id == initialHistoryId }
        }
    }

    val storyTimeline = remember(filteredHistories) {
        filteredHistories.flatMap { history ->
            if (history.imageUrls.isEmpty()) {
                listOf(StoryItem(history, null, 0, 1))
            } else {
                history.imageUrls.mapIndexed { index, url ->
                    StoryItem(history, url, index, history.imageUrls.size)
                }
            }
        }
    }

    // 진입 히스토리 식별자 기준 초기 페이지 인덱스 계산
    val startIndex = remember(storyTimeline, initialHistoryId) {
        val index = storyTimeline.indexOfFirst { it.history.id == initialHistoryId }
        if (index != -1) index else 0
    }

    val totalPages = storyTimeline.size.coerceAtLeast(1)
    val pagerState = rememberPagerState(
        initialPage = startIndex,
        pageCount = { totalPages }
    )

    val currentHistory = remember(storyTimeline, pagerState.currentPage) {
        storyTimeline.getOrNull(pagerState.currentPage)?.history
    } ?: return
    var timeLeftText by remember { mutableStateOf("") }

    var isPaused by remember { mutableStateOf(false) }
    var commentInput by remember { mutableStateOf("") }
    val storyDuration = 3000
    var progressTicks by remember { mutableStateOf(0) }
    val isKeyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            // 토스트 메시지 표시
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()

            // 토스트를 띄운 후 ViewModel의 에러 메시지 상태를 초기화
            viewModel.clearErrorMessage()
        }
    }

    LaunchedEffect(currentHistory.createdAt) {
        while (true) {
            val now = System.currentTimeMillis()

            // 새로 만든 함수를 사용하여 텍스트 설정
            timeLeftText = currentHistory.getRemainingHoursText(now)

            if (currentHistory.isExpired(now)) {
                break
            }

            // 1시간 미만일 때 분 단위를 실시간으로 보여주려면 30초~1분 정도 딜레이가 적당
            delay(60000)
        }
    }

    LaunchedEffect(comments.size) {
        if (comments.isNotEmpty()) {
            listState.animateScrollToItem(comments.size - 1)
        }
    }

    // 현재 히스토리 식별자 변경 감지 및 상위 레이어 보고
    LaunchedEffect(currentHistory.id) {
        onHistoryChanged(currentHistory.id)
    }

    LaunchedEffect(isKeyboardVisible) {
        if (!isKeyboardVisible) {
            focusManager.clearFocus(force = true)
            isPaused = false
        }
    }

    // 3초 타이머 기반 페이지 자동 전환 제어
    LaunchedEffect(pagerState.currentPage, isPaused) {
        if (!isPaused) {
            progressTicks = 0
            while (progressTicks < storyDuration) {
                delay(30)
                progressTicks += 30
            }

            // 다음 페이지 이동 또는 첫 페이지 루프 처리
            if (pagerState.currentPage < totalPages - 1) {
                scope.launch {
                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                }
            } else {
                scope.launch {
                    pagerState.animateScrollToPage(0)
                }
            }
        }
    }

    BackHandler(enabled = true) {
        if (isPaused) {
            keyboardController?.hide()
            focusManager.clearFocus(force = true)
            isPaused = false
        } else {
            onDismiss()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = false
        ) { page ->
            val story = storyTimeline.getOrNull(page)
            if (story?.imageUrl != null) {
                AsyncImage(
                    model = story.imageUrl,
                    contentDescription = "스토리 이미지",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize().background(Color(0xFF1E1E24)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_footprints_icon),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.1f),
                        modifier = Modifier.size(120.dp)
                    )
                }
            }
        }

        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.5f),
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.7f)
                    )
                )
            )
        )

        // 제스처 마스크 영역 격리
        Box(
            modifier = Modifier.fillMaxSize().padding(top = 120.dp, bottom = 340.dp).pointerInput(totalPages) {
                detectTapGestures(
                    onPress = {
                        try {
                            isPaused = true
                            awaitRelease()
                        } finally {
                            isPaused = false
                        }
                    },
                    onTap = { offset ->
                        val isLeft = offset.x < size.width * 0.5f
                        if (isLeft) {
                            if (pagerState.currentPage > 0) {
                                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) }
                            }
                        } else {
                            if (pagerState.currentPage < totalPages - 1) {
                                scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                            } else {
                                scope.launch { pagerState.animateScrollToPage(0) }
                            }
                        }
                        keyboardController?.hide()
                    }
                )
            }
        )

        // 상단 인디케이터 제어바
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .align(Alignment.TopCenter)
                .pointerInput(Unit) {}
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(totalPages) { index ->
                    val animatedProgress by animateFloatAsState(
                        targetValue = when {
                            index < pagerState.currentPage -> 1f
                            index == pagerState.currentPage -> progressTicks.toFloat() / storyDuration
                            else -> 0f
                        },
                        animationSpec = tween(durationMillis = 30, easing = LinearEasing),
                        label = "StoryBarProgress"
                    )

                    Box(
                        modifier = Modifier.weight(1f).height(3.dp).background(Color.White.copy(alpha = 0.3f), CircleShape)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxHeight().fillMaxWidth(animatedProgress).background(Color.White, CircleShape)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AsyncImage(
                        model = currentHistory.userProfileImage.ifEmpty { "https://via.placeholder.com/150" },
                        contentDescription = "유저 프로필 이미지",
                        modifier = Modifier.size(36.dp).clip(CircleShape).border(2.dp, Color.White, CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Column {
                        Text(
                            text = currentHistory.userNickname,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.3).sp
                        )
                        Text(
                            text = "실시간 발자취",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 본인 작성 히스토리 검증 기반 삭제 기능 활성화
                    if (currentHistory.userId == myUid) {
                        IconButton(
                            onClick = {
                                isPaused = true // 삭제 팝업 노출 시 진행 상태 일시정지
                                onDeleteClick(currentHistory.id)
                            },
                            modifier = Modifier.size(32.dp).background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "내 발자취 삭제",
                                tint = recording, // 빨간색 강조 디자인 활용
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = { isPaused = !isPaused },
                        modifier = Modifier.size(25.dp).background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = "일시정지 토글",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp).background(Color.Black.copy(alpha = 0.4f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "스토리 닫기",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = timeLeftText,
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // 하단 복합 메타 패널
        Column(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).navigationBarsPadding().imePadding().padding(horizontal = 16.dp).padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 댓글 수집 패널
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 150.dp).background(Color.Black.copy(alpha = 0.35f), RoundedCornerShape(16.dp)).border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp)).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "COMMENTS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White.copy(alpha = 0.5f),
                    letterSpacing = 1.sp
                )

                if (comments.isEmpty()) {
                    Text(
                        text = "첫 코멘트를 남겨보세요! 💬",
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.4f),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(bottom = 8.dp)
                    ) {
                        items(comments) { comment ->
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = comment.userNickname,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF956AFC),
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Text(
                                    text = comment.text,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.95f),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            // 카드 본문 컨텐츠 레이아웃
            Card(
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth().border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(32.dp))
            ) {
                // 개별 카드의 수정 상태 모니터링을 위한 내부 상태 변수 선언
                var isEditingMode by remember { mutableStateOf(false) }
                var editedContent by remember(currentHistory.content) { mutableStateOf(currentHistory.content) }
                val contentFocusRequester = remember { FocusRequester() }
                val contentFocusManager = LocalFocusManager.current

                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val (iconResId, bgColor, iconColor) = when (currentHistory.category) {
                                "무전" -> Triple(R.drawable.ic_walkie_talkie_icon, Color(0xFFFAF5FF), Color(0xFFA855F7))
                                "카페" -> Triple(R.drawable.ic_cafe_icon, Color(0xFFFFFBEB), Color(0xFFD97706))
                                "음식" -> Triple(R.drawable.ic_restaurant_icon, Color(0xFFFFF1F2), Color(0xFFF43F5E))
                                "운동" -> Triple(R.drawable.ic_exercise_icon, Color(0xFFECFDF5), Color(0xFF10B981))
                                else -> Triple(R.drawable.ic_daily_icon, Color(0xFFEEF2FF), Color(0xFF6366F1))
                            }

                            Row(
                                modifier = Modifier.background(bgColor, RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = iconResId),
                                    contentDescription = null,
                                    tint = iconColor,
                                    modifier = Modifier.size(12.dp)
                                )
                                Text(
                                    text = currentHistory.category,
                                    color = iconColor,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }

                            Text(
                                text = "📍 ${currentHistory.placeName}",
                                color = Color.White.copy(alpha = 0.95f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        val isLiked = currentHistory.isLikedByUser(myUid)
                        val likeCount = currentHistory.likedUserIds.size

                        // 좋아요 패널
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {

                            // 본인 작성 히스토리일 때만 제어 버튼 노출
                            if (currentHistory.userId == myUid) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(
                                            if (isEditingMode) Color(0xFF10B981).copy(alpha = 0.2f) else Color.White.copy(
                                                alpha = 0.1f
                                            ),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (isEditingMode) Color(0xFF10B981) else Color.White.copy(
                                                alpha = 0.1f
                                            ),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            if (isEditingMode) {
                                                // 1. 완료 상태 진입 시: 포커스 해제, 타이머 재개, 뷰모델 통신 호출
                                                contentFocusManager.clearFocus()
                                                keyboardController?.hide()
                                                isEditingMode = false
                                                isPaused = false

                                                // 공백이 아닐 때만 뷰모델 수정 함수 트리거
                                                if (editedContent.trim().isNotEmpty()) {
                                                    onHistoryUpdate(
                                                        currentHistory.id,
                                                        editedContent
                                                    )
                                                }
                                            } else {
                                                // 2. 수정 모드 진입 시: 타이머 일시정지 및 포커스 요청
                                                isPaused = true
                                                isEditingMode = true
                                                contentFocusRequester.requestFocus()
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isEditingMode) Icons.Default.Check else Icons.Default.Edit, // 수정 중일 땐 체크 아이콘으로 변경 (Icons.Default.Check 추가 필요)
                                        contentDescription = if (isEditingMode) "수정 완료" else "발자취 수정",
                                        tint = if (isEditingMode) Color(0xFF10B981) else Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            if (likeCount > 0) {
                                Text(
                                    text = likeCount.toString(),
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        if (isLiked) Color(0xFFF43F5E).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.1f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (isLiked) Color(0xFFF43F5E) else Color.White.copy(alpha = 0.1f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { onLikeToggle(currentHistory) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                    contentDescription = "좋아요 버튼",
                                    tint = if (isLiked) Color(0xFFF43F5E) else Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (isEditingMode) {
                        BasicTextField(
                            value = editedContent,
                            onValueChange = { editedContent = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(contentFocusRequester),
                            textStyle = TextStyle(
                                color = Color.White.copy(alpha = 0.95f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                lineHeight = 20.sp
                            ),
                            cursorBrush = SolidColor(Color.White)
                        )
                    } else {
                        val annotatedBodyText = buildAnnotatedString {
                            withStyle(style = SpanStyle(
                                color = Color.White.copy(alpha = 0.95f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            ) {
                                append(currentHistory.content)
                            }

                            //  만약 수정된 문서라면 본문 뒤에 한 칸 띄우고 작고 흐린 회색 텍스트 추가
                            if (currentHistory.isEdited) {
                                withStyle(style = SpanStyle(
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Normal
                                )) {
                                    append(" (수정됨)")
                                }
                            }
                        }

                        Text(
                            text = annotatedBodyText,
                            lineHeight = 20.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 하단 인터랙션 바 배너
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(1f).background(Color.Black.copy(alpha = 0.4f), CircleShape).border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape).padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    if (commentInput.isEmpty()) {
                        Text(
                            text = "${currentHistory.userNickname}님의 발자취에 댓글 달기...",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    BasicTextField(
                        value = commentInput,
                        onValueChange = { commentInput = it },
                        textStyle = TextStyle(color = Color.White, fontSize = 13.dp.value.sp, fontWeight = FontWeight.Bold),
                        modifier = Modifier.fillMaxWidth().onFocusChanged { focusState -> if (focusState.isFocused) isPaused = true },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (commentInput.trim().isNotEmpty()) {
                                    onCommentSubmit(currentHistory.id, commentInput)
                                    commentInput = ""
                                    keyboardController?.hide()
                                    focusManager.clearFocus(force = true)
                                    isPaused = false
                                }
                            }
                        )
                    )
                }

                Box(
                    modifier = Modifier.size(48.dp).background(Color.White.copy(alpha = 0.1f), CircleShape).border(1.dp, Color.White.copy(alpha = 0.1f), CircleShape).clickable {
                        if (commentInput.trim().isNotEmpty()) {
                            onCommentSubmit(currentHistory.id, commentInput)
                            commentInput = ""
                            keyboardController?.hide()
                            focusManager.clearFocus(force = true)
                            isPaused = false
                        }
                    },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "댓글 전송",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}