package com.bbip.bbipit.presentation.mypage

import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.navigation.NavController
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import coil.compose.AsyncImage
import com.bbip.bbipit.core.navigation.Routes
import com.bbip.bbipit.core.ui.theme.Typography
import com.bbip.bbipit.core.ui.theme.background
import com.bbip.bbipit.core.ui.theme.fontDefault
import com.bbip.bbipit.core.ui.theme.primary
import com.bbip.bbipit.core.ui.theme.subBackground
import com.bbip.bbipit.presentation.auth.viewmodel.SignInEvent
import com.bbip.bbipit.presentation.base.ConfirmDialog
import com.bbip.bbipit.presentation.base.LoadingBox
import com.bbip.bbipit.presentation.base.ShowToast
import com.bbip.bbipit.presentation.main.BottomBarViewModel
import com.bbip.bbipit.presentation.user.ui.SettingsDrawer
import com.google.firebase.auth.FirebaseAuth

val KakaoYellow = Color(0xFFFEE500)


@Composable
fun MyPageScreen(
    navController: NavController,
    viewModel: MyPageViewmodel = hiltViewModel(),
    bottomBarViewModel: BottomBarViewModel =
        hiltViewModel(viewModelStoreOwner = (LocalActivity.current as ComponentActivity)), //드로어블 열렸을 때 하단바 안보이게 하기 위해 하단바 관련 뷰모델 생성
    onCopyIdClick: (String) -> Unit = {},
    onShareKakaoClick: (String) -> Unit = {}
) {
    // 뷰모델의 UI 상태 구독
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isShownDrawer by bottomBarViewModel.isDrawerShown.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    // 화면이 그려지자마자 내 데이터를 서버에서 가져옴
    LaunchedEffect(Unit) {
        viewModel.fetchUserProfile()
    }
    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when(event){
                is MyPageEvent.NavigateToSignIn ->
                    navController.navigate(Routes.SignIn) {
                        popUpTo(0){ inclusive = true }
                    }
            }
        }
    }
    LaunchedEffect(isShownDrawer) {
        if (isShownDrawer) {
            drawerState.open()
        } else {
            drawerState.close()
        }
    }

    LaunchedEffect(drawerState.currentValue) {
        val isDrawerUiOpen = drawerState.isOpen
        if (isDrawerUiOpen != isShownDrawer) {
            bottomBarViewModel.onUpdateDrawerShown(isDrawerUiOpen)
        }
    }

    // 상태 변수에 값이 채워지는 순간, ShowToast 공통 컴포저블 호출
    uiState.toast?.let { message ->
        ShowToast(message = message)
        viewModel.onUpdateToast(null) // 띄운 직후 다시 null로 비워주어야 다음 클릭 때 또 반응합니다.
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                Log.d("마이페이지", "${uiState.email}, ${uiState.loginType}")
                SettingsDrawer(
                    email = uiState.email,
                    loginType = uiState.loginType,
                    modifier = Modifier.fillMaxWidth(0.7f).fillMaxHeight(),
                    onClose = { bottomBarViewModel.onUpdateDrawerShown(false) },
                    viewModel = viewModel
                )

            }
        ) {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = background
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 상단 타이틀 및 설정 헤더 영역
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 24.dp, bottom = 32.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "내 정보",
                                style = Typography.bodyLarge,
                            )
                            IconButton(
                                onClick = {
                                    bottomBarViewModel.onUpdateDrawerShown(true)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "설정",
                                    tint = primary,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }

                        // 프로필 이미지 영역
                        Box(
                            modifier = Modifier
                                .size(130.dp)
                                .background(primary, shape = CircleShape)
                                .padding(4.dp)
                                ,
                            contentAlignment = Alignment.Center,
                        ) {
                            // profileImageUrl이 비어있지 않으면 사진을, 비어있으면 아이콘을 보여줌
                            if (uiState.profileImageUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = uiState.profileImageUrl,
                                    contentDescription = "프로필 이미지",
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.LightGray, shape = CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "기본 프로필",
                                        tint = Color.White,
                                        modifier = Modifier.size(78.dp) // 130.dp의 60%
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // 이름 및 상태 메시지 텍스트 영역
                        Text(
                            text = uiState.nickname,
                            style = Typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 상태 메시지 캡슐
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            shadowElevation = 1.dp,
                            tonalElevation = 0.dp,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        ) {
                            Text(
                                text = uiState.status,
                                style = Typography.bodySmall,
                                color = primary,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // 프로필 편집 버튼
                        Button(
                            onClick = {
                                navController.navigate(
                                    Routes.EditProfile(
                                        currentNickname = uiState.nickname,
                                        currentStatus = uiState.status,
                                        profileImageUrl = uiState.profileImageUrl
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = subBackground
                            ),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 28.dp, vertical = 10.dp),
                            elevation = ButtonDefaults.buttonElevation(1.dp)
                        ) {
                            Text(
                                text = "프로필 편집",
                                color = primary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                style = Typography.bodySmall
                            )
                        }

                        Spacer(modifier = Modifier.height(48.dp))

                        // ID 입체 카드 컴포넌트
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(32.dp),
                            color = subBackground,
                            shadowElevation = 1.dp
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp, horizontal = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "내 CODE : ${uiState.userCode} ",
                                    style = Typography.bodyMedium,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 1.5.sp
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // 고유 ID 텍스트
                                    Text(
                                        text = uiState.uniqueId,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = fontDefault,
                                        modifier = Modifier.padding(end = 12.dp)
                                    )

                                    // 복사 버튼
                                    IconButton(
                                        onClick = { viewModel.copyToClipboard(uiState.userCode) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(background, shape = RoundedCornerShape(8.dp))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "ID 복사하기",
                                            tint = fontDefault,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

//                                    Spacer(modifier = Modifier.width(20.dp))
//
//                                    // 카카오톡 공유 버튼
//                                    IconButton(
//                                        onClick = { onShareKakaoClick(uiState.uniqueId) },
//                                        modifier = Modifier
//                                            .size(36.dp)
//                                            .background(KakaoYellow, shape = RoundedCornerShape(8.dp))
//                                    ) {
//                                        // 💡 실제 카카오 이모지 아이콘 리소스가 있다면 대체 가능합니다!
//                                        Text(
//                                            text = "💬",
//                                            fontSize = 14.sp,
//                                            textAlign = TextAlign.Center
//                                        )
//                                    }
                                }
                            }
                        }
                    }
            }
        }
    }


    }
    if (uiState.isLoading) { LoadingBox() }

    if (uiState.isSignOutDialogShown){
        ConfirmDialog(text = "로그아웃 하시겠습니까?",
            onDismiss = {viewModel.onChangeSignOutDialog(false)},
            onConfirm = {
                viewModel.onChangeSignOutDialog(false)
                viewModel.signOut()
            }
        )
    }
}
