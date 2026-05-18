package com.bbip.bbipit.presentation.auth.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.bbip.bbipit.core.navigation.Routes
import com.bbip.bbipit.core.ui.theme.Typography
import com.bbip.bbipit.core.ui.theme.background
import com.bbip.bbipit.core.ui.theme.primary
import com.bbip.bbipit.presentation.auth.ui.components.AgreeDialog
import com.bbip.bbipit.presentation.auth.ui.components.InputField
import com.bbip.bbipit.presentation.auth.viewmodel.SignInEvent
import com.bbip.bbipit.presentation.auth.viewmodel.SignUpEvent
import com.bbip.bbipit.presentation.auth.viewmodel.SignUpViewModel
import com.bbip.bbipit.presentation.base.ConfirmDialog
import com.bbip.bbipit.presentation.base.ShowToast

enum class TermsType {
    PRIVACY, // 개인정보 처리방침
    SERVICE  // 서비스 이용약관
}

@Composable
fun SignUpScreen(navController: NavController, viewModel: SignUpViewModel = hiltViewModel()) {

    LaunchedEffect(Unit) {
        viewModel.event.collect { event ->
            when(event){
                SignUpEvent.NavigateToSignIn -> navController.popBackStack()
            }
        }
    }
    val focusManager = LocalFocusManager.current

    var checkPw by remember { mutableStateOf("") }
    var showAgreeDialog by remember { mutableStateOf(false) }
    var currentTermsType by remember { mutableStateOf(TermsType.PRIVACY) }
    var isAgreed by remember { mutableStateOf(false) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val isAllEntered by remember {
        derivedStateOf {
            uiState.name.isNotBlank() && uiState.email.isNotBlank() &&
                    uiState.password.isNotBlank() && (uiState.password == uiState.checkPw)
                    && isAgreed
        }
    }

    Scaffold(modifier = Modifier.fillMaxSize().background(background).systemBarsPadding()) {
        innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(vertical = 35.dp, horizontal = 23.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
            indication = null
        ){
            focusManager.clearFocus()
        },
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(17.dp, alignment = Alignment.CenterVertically),
        ) {

            Spacer(Modifier.height(30.dp))

            Box(modifier = Modifier.shadow(elevation = 3.dp, shape = RoundedCornerShape(20.dp))
                .background(Color.White, shape = RoundedCornerShape(20.dp))
                .padding(12.dp)
                .clickable {
                    navController.popBackStack()
                },
                contentAlignment = Alignment.Center,

                ){
                Icon(imageVector = Icons.Default.ArrowBackIosNew,
                    contentDescription = "뒤로가기",
                    tint = Color.Gray)
            }

            Text("BBip-It", style = Typography.titleLarge)
            Text("정보를 입력하고 새로 BBip-It을 시작해 보세요.", style = Typography.bodySmall)

            Spacer(Modifier.height(17.dp))

            InputField(
                value = uiState.name,
                onValueChange = { viewModel.onUpdateName(it) },
                placeholder = "이름",
                keyboardType = KeyboardType.Text
            )

            InputField(
                value = uiState.email,
                onValueChange = { viewModel.onUpdateEmail(it) },
                placeholder = "이메일",
                keyboardType = KeyboardType.Email,
                errorText = uiState.emailError
            )

            InputField(
                value = uiState.password,
                onValueChange = { viewModel.onUpdatePassword(it) },
                placeholder = "비밀번호, 대소문자+특수문자 혼합 8자리 이상",
                isPassword = true,
                keyboardType = KeyboardType.Password,
                errorText = uiState.pwError
            )

            InputField(
                value = uiState.checkPw,
                onValueChange = { viewModel.onUpdateCheckPw(it) },
                placeholder = "비밀번호 확인",
                isPassword = true,
                keyboardType = KeyboardType.Password
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        showAgreeDialog = true
                        viewModel.getTerms(TermsType.PRIVACY)
                    }
            ) {
                Icon(
                    imageVector = if (isAgreed) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "약관 동의",
                    tint = if (isAgreed) primary else Color.LightGray,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "서비스 이용약관 및 개인정보 처리방침에 모두 동의합니다.",
                    style = Typography.bodySmall
                )
            }


            Button({viewModel.signUp()},
                enabled = isAllEntered,
                colors = ButtonDefaults.buttonColors(primary, disabledContainerColor = Color.Gray),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(60.dp),
                elevation = ButtonDefaults.buttonElevation(8.dp)
            ) {
                Text("회원가입", style = Typography.bodyLarge, color = Color.White)
            }

            uiState.error?.let {
                Text(it, style = Typography.bodySmall, color = Color.Red)
            }



        }

    }

    if (showAgreeDialog){
        AgreeDialog(
            terms = uiState.terms, type = TermsType.PRIVACY, isSignUp = true,
            onNext = {
                if (currentTermsType == TermsType.PRIVACY) {
                    currentTermsType = TermsType.SERVICE
                    viewModel.getTerms(TermsType.SERVICE)
                } else {
                    showAgreeDialog = false
                    isAgreed = true
                }

            },
            onDismissRequest = {
                isAgreed = it
                showAgreeDialog = false
            }
        )
    }
    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Gray.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.LightGray)
        }
    }

    if (uiState.isNotiShown){
        ConfirmDialog("입력한 메일 계정에서 인증을 진행해주세요.",
            isSingleBtn = true,
            onConfirm = {
                viewModel.onUpdateNotiShown(false)
                viewModel.moveToSignIn()

            },
            onDismiss = {}
        )
    }
}


