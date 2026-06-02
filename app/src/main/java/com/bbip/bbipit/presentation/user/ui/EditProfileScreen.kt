package com.bbip.bbipit.presentation.mypage

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.toRoute
import coil.compose.AsyncImage
import com.bbip.bbipit.core.navigation.Routes
import com.bbip.bbipit.core.ui.theme.PurpleGrey80
import com.bbip.bbipit.core.ui.theme.Typography
import com.bbip.bbipit.core.ui.theme.background
import com.bbip.bbipit.core.ui.theme.fontDefault
import com.bbip.bbipit.core.ui.theme.primary
import com.bbip.bbipit.core.ui.theme.recording
import com.bbip.bbipit.core.ui.theme.subBackground
import com.bbip.bbipit.presentation.base.LoadingBox
import com.bbip.bbipit.presentation.base.ShowToast
import com.bbip.bbipit.presentation.base.UserStatusType


data class EditProfileUiState(
    val nickname: String = "",
    val status: String = "",
    val profileImageUrl: String = "",
    val isBottomSheetVisible: Boolean = false,
    val isNicknameError: Boolean = false, // 예외처리 위함 공백일 경우
    val isLoading: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavController,
    viewModel: EditProfileViewModel = hiltViewModel()
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()


    // 상태 메시지 옵션 리스트
    val statusOptions = UserStatusType.allTexts

    val backStackEntry = remember(navController) { navController.currentBackStackEntry }
    val args = remember(backStackEntry) { backStackEntry?.toRoute<Routes.EditProfile>() }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isOverInput by remember(uiState.nickname) { mutableStateOf(uiState.nickname.length > 12) } //닉네임 글자수 제한

    val context = LocalContext.current

    // 뷰모델의 토스트 메시지 수집 및 직접 표시
    LaunchedEffect(Unit) {
        viewModel.toastMessage.collect { message ->
            // 상태 변수에 담지 말고 즉시 띄우기
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    val displayModel = selectedImageUri ?: uiState.profileImageUrl

    LaunchedEffect(args) {
        if (args != null) {
            viewModel.initWithUserData(
                currentNickname = args.currentNickname,
                currentStatus = args.currentStatus,
                profileImageUrl = args.profileImageUrl
            )
        }
    }

    // 갤러리 선택 런처
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            println("갤러리 이미지 선택됨: $uri")
        }
    }

    LaunchedEffect(Unit) {
        viewModel.saveSuccessEvent.collect { isSuccess ->
            if (isSuccess) {
                navController.popBackStack() // 성공 시 마이페이지로 복귀
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "프로필 편집",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        style = Typography.bodyMedium
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로가기",
                            tint = fontDefault
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = background)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                Box(
                    modifier = Modifier.size(140.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    // 1. 프로필 이미지 표시부
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .background(subBackground, shape = CircleShape)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // AsyncImage는 로딩, 에러 처리를 라이브러리가 자동으로 수행
                        val hasImage = (displayModel is Uri) || (displayModel is String && displayModel.toString().isNotEmpty())

                        if (hasImage) {
                            AsyncImage(
                                model = displayModel,
                                contentDescription = "프로필 이미지",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            // 이미지가 없으면 기본 아이콘 표시
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "기본 프로필",
                                modifier = Modifier.size(64.dp),
                                tint = Color.LightGray
                            )
                        }
                    }

                    // 카메라 변경 버튼 플로팅
                    IconButton(
                        onClick = {
                            // 갤러리 바로 열기
                            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White, shape = CircleShape)
                            .padding(2.dp),
                        colors = IconButtonDefaults.iconButtonColors(contentColor = primary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "프로필 사진 변경",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(36.dp))

                // 닉네임 입력 필드
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "닉네임",
                        fontSize = 13.sp,
                        color = if (uiState.isNicknameError) recording else PurpleGrey80,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )
                    OutlinedTextField(
                        value = uiState.nickname,
                        onValueChange = { viewModel.updateNickname(it) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        isError = uiState.isNicknameError || isOverInput,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            errorBorderColor = recording // 에러 시 테두리 색상
                        ),
                        singleLine = true,
                        trailingIcon = {
                            Text(
                                text = "${uiState.nickname.length}/12",
                                color = if (isOverInput) recording else Color.DarkGray,
                                fontSize = 12.sp,
                                style = Typography.labelSmall,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    )

                    // 경고 메시지 표시
                    if (uiState.isNicknameError || isOverInput ) {
                        Text(
                            text = if(isOverInput) "닉네임은 최대 12글자 입니다." else "닉네임을 입력하세요.",
                            color = recording,
                            fontSize = 12.sp,
                            style = Typography.bodySmall,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 상태 메시지 선택 필드 (누르면 바텀시트가 열림)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "상태 메시지",
                        fontSize = 13.sp,
                        color = fontDefault,
                        style = Typography.bodyMedium,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(62.dp)
                            .background(Color.White, shape = RoundedCornerShape(24.dp))
                            .clickable { viewModel.setBottomSheetVisibility(true) }
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = uiState.status,
                            style = Typography.bodyMedium
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "선택창 열기",
                            tint = Color.LightGray
                        )
                    }
                }
            }

            // 하단 고정 변경사항 저장 버튼
            Button(
                onClick = {
                    viewModel.saveProfileChanges(selectedImageUri)
                },
                enabled = uiState.nickname.isNotBlank() && !isOverInput,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, bottom = 32.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primary,
                    disabledContainerColor = Color.LightGray),
                shape = RoundedCornerShape(28.dp)
            ) {
                Text(
                    text = "변경사항 저장",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = Typography.bodyMedium
                )
            }
        }
        if (uiState.isLoading){
            LoadingBox()
        }
    }

    // 상태 메시지 선택 시트
    if (uiState.isBottomSheetVisible) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.setBottomSheetVisibility(false) },
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color.LightGray) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "상태 메시지 선택",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = fontDefault,
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                // 시안 캡처화면에 있는 6개 리스트 뷰 아이템 생성
                statusOptions.forEach { option ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable {
                                viewModel.updateStatus(option)
                                viewModel.setBottomSheetVisibility(false) // 시트 닫기
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = background.copy(alpha = 0.5f)
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = option,
                                fontSize = 15.sp,
                                color = fontDefault,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "닫기",
                    color = Color.Gray,
                    fontSize = 15.sp,
                    modifier = Modifier
                        .clickable { viewModel.setBottomSheetVisibility(false) }
                        .padding(vertical = 8.dp)
                )
            }
        }
    }
}