package com.bbip.bbipit.presentation.user.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.ModeComment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import coil.compose.AsyncImage
import com.bbip.bbipit.core.ui.theme.Typography
import com.bbip.bbipit.core.ui.theme.background
import com.bbip.bbipit.core.ui.theme.primary
import com.bbip.bbipit.domain.entity.History
import com.bbip.bbipit.domain.entity.HistoryComment
import com.bbip.bbipit.presentation.map.ui.HistoryViewerScreen
import com.bbip.bbipit.presentation.map.viewmodel.HistoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyHistoryGridViewerDialog(
    myUid: String,
    histories: List<History>,
    comments: List<HistoryComment>,
    onHistoryChanged: (String) -> Unit,
    viewModel: HistoryViewModel,
    onLikeToggle: (History) -> Unit,
    onCommentSubmit: (String, String) -> Unit,
    onDeleteClick: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onHistoryUpdate: (String, String) -> Unit,
) {
    var selectedCategory by remember { mutableStateOf("전체") }
    val categories = listOf("전체", "🎙️ 무전", "☕ 카페", "🍽️ 맛집", "🏃 운동", "📸 사진", "🌟 일상")

    var isViewerOpen by remember { mutableStateOf(false) }
    var targetHistoryId by remember { mutableStateOf("") }

    val myHistories = remember(histories, myUid) {
        histories.filter { it.userId == myUid }
    }

    val filteredHistories = remember(selectedCategory, myHistories) {
        if (selectedCategory == "전체") {
            myHistories
        } else {
            myHistories.filter { history ->
                selectedCategory.contains(history.category)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        val view = LocalView.current
        val dialogWindow = (view.parent as? DialogWindowProvider)?.window

        SideEffect {
            dialogWindow?.let { window ->
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = true
                insetsController.isAppearanceLightNavigationBars = true
            }
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = background
        ) {
            Column(
                modifier = modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "내 발자취 모아보기",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF0F172A),
                            letterSpacing = (-0.5).sp
                        )
                        Text(
                            text = "내가 남긴 장소별 소중한 순간들",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF94A3B8),
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFF1F5F9), shape = CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "닫기",
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                ScrollableTabRow(
                    selectedTabIndex = categories.indexOf(selectedCategory),
                    edgePadding = 24.dp,
                    divider = {},
                    indicator = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    containerColor = background
                ) {
                    categories.forEach { category ->
                        val isSelected = selectedCategory == category
                        Tab(
                            selected = isSelected,
                            onClick = { selectedCategory = category },
                            text = {
                                Text(
                                    text = category,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                    color = if (isSelected) primary else Color(0xFF94A3B8),
                                    fontSize = 13.sp
                                )
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (filteredHistories.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "📭", fontSize = 40.sp)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "아직 남긴 발자취가 없어요",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF1E293B)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "지도 화면 우측 상단의 핀 버튼을 눌러\n나만의 첫 장소 기록을 등록해 보세요!",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF94A3B8),
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .padding(horizontal = 24.dp)
                    ) {
                        items(filteredHistories) { history ->
                            GridHistoryItem(
                                myUid = myUid,
                                history = history,
                                onClick = {
                                    targetHistoryId = history.id
                                    isViewerOpen = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (isViewerOpen && targetHistoryId.isNotEmpty()) {
        Dialog(
            onDismissRequest = {
                isViewerOpen = false
                targetHistoryId = ""
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            val windowProvider = LocalView.current.parent as? DialogWindowProvider
            windowProvider?.window?.setDimAmount(0.0f)

            HistoryViewerScreen(
                myUid = myUid,
                histories = myHistories,
                initialHistoryId = targetHistoryId,
                comments = comments,
                viewModel = viewModel,
                onHistoryChanged = onHistoryChanged,
                onDismiss = {
                    isViewerOpen = false
                    targetHistoryId = ""
                },
                onLikeToggle = onLikeToggle,
                onCommentSubmit = onCommentSubmit,
                onDeleteClick = { id ->
                    onDeleteClick(id)
                    isViewerOpen = false
                    targetHistoryId = ""
                },
                onHistoryUpdate = onHistoryUpdate
            )
        }
    }
}

@Composable
fun GridHistoryItem(
    myUid: String,
    history: History,
    onClick: () -> Unit
) {
    val themeColor = remember(history.category) {
        when (history.category) {
            "무전" -> Color(0xFF956AFC)
            "카페" -> Color(0xFFD97706)
            "음식" -> Color(0xFFF43F5E)
            "운동" -> Color(0xFF10B981)
            "사진" -> Color(0xFF0284C7)
            "일상" -> Color(0xFF6366F1)
            else -> Color(0xFF64748B)
        }
    }

    val isLiked = remember(history.likedUserIds, myUid) {
        history.isLikedByUser(myUid)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (history.imageUrls.isNotEmpty()) {
                AsyncImage(
                    model = history.imageUrls[0],
                    contentDescription = history.placeName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(themeColor.copy(alpha = 0.15f), themeColor.copy(alpha = 0.05f))
                            )
                        )
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.0f),
                                Color.Black.copy(alpha = 0.4f),
                                Color.Black.copy(alpha = 0.85f)
                            ),
                            startY = 0f
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp)
                    .align(Alignment.TopStart),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.95f),
                    modifier = Modifier.height(22.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 0.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val displayCategory = remember(history.category) {
                            when (history.category) {
                                "무전" -> "🎙️ 무전"
                                "카페" -> "☕ 카페"
                                "음식" -> "🍽️ 맛집"
                                "운동" -> "🏃 운동"
                                "사진" -> "📸 사진"
                                "일상" -> "🌟 일상"
                                else -> "👣 ${history.category}"
                            }
                        }

                        Text(
                            text = displayCategory,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            color = themeColor
                        )
                    }
                }

                if (history.imageUrls.size > 1) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Black.copy(alpha = 0.6f),
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = "멀티 이미지",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                text = "+${history.imageUrls.size - 1}",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = history.placeName,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 0.5.dp,
                    color = Color.White.copy(alpha = 0.15f)
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "좋아요",
                            tint = if (isLiked) Color(0xFFFB7185) else Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "${history.likedUserIds.size}",
                            color = if (isLiked) Color(0xFFFB7185) else Color.White.copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ModeComment,
                            contentDescription = "댓글",
                            tint = Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "${history.content.length}",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}