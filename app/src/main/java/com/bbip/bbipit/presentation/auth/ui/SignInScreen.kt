package com.bbip.bbipit.presentation.auth.ui

import android.util.Log
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.bbip.bbipit.R
import com.bbip.bbipit.core.navigation.Routes
import com.bbip.bbipit.core.ui.theme.Typography
import com.bbip.bbipit.core.ui.theme.background
import com.bbip.bbipit.core.ui.theme.primary
import com.bbip.bbipit.presentation.auth.ui.components.InputField
import com.bbip.bbipit.presentation.auth.viewmodel.SignInEvent
import com.bbip.bbipit.presentation.auth.viewmodel.SignInViewModel
import com.bbip.bbipit.presentation.base.ConfirmDialog
import com.bbip.bbipit.presentation.base.ShowToast

@Composable
fun SignInScreen(navController: NavController, viewModel: SignInViewModel = hiltViewModel()) {

    val focusManager = LocalFocusManager.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val isAllEntered by remember {
        derivedStateOf {
            uiState.email.isNotBlank() && uiState.password.isNotBlank()
        }
    }


    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when(event){
                is SignInEvent.NavigateToSignUp ->
                    navController.navigate(Routes.SignUp)
                is SignInEvent.NavigateToHome ->
                    navController.navigate(Routes.Map){
                        popUpTo(0){ inclusive = true }
                    }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(background)) {
        Column(modifier = Modifier.fillMaxSize().padding(30.dp)
            .background(background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ){
                focusManager.clearFocus()
            },
            verticalArrangement = Arrangement.spacedBy(27.dp, alignment = Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally) {

            Image(painter = painterResource(R.drawable.logo),
                contentDescription = "로고",
                modifier = Modifier.size(120.dp).padding(top = 20.dp)
            )
            Text("BBip-It", style = Typography.titleLarge)
            Text("삐빗- 심장이 반응하는 거리", style = Typography.bodySmall)
            Spacer(modifier = Modifier.height(13.dp))
//            Text("이메일", style = Typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
            Log.d("AuthUIDebug", "Compose가 그린 에러 상태: ${uiState.emailError}")
            InputField(
                value = uiState.email,
                onValueChange = { viewModel.onUpdateEmail(it) },
                placeholder = "이메일",
                keyboardType = KeyboardType.Email,
                errorText = uiState.emailError
            )
//            Text("비밀번호", style = Typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
            InputField(
                value = uiState.password,
                onValueChange = { viewModel.onUpdatePassword(it) },
                placeholder = "비밀번호",
                isPassword = true,
                keyboardType = KeyboardType.Password,
                errorText = uiState.pwError
            )

            Button({viewModel.signIn()},
                enabled = isAllEntered,
                colors = ButtonDefaults.buttonColors(primary, disabledContainerColor = Color.Gray),
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(60.dp),
                elevation = ButtonDefaults.buttonElevation(8.dp)
            ) {
                Text("로그인", style = Typography.bodyMedium, color = Color.White, fontWeight = FontWeight.Bold)
            }

            Row(modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically) {
                HorizontalDivider(thickness = 0.7.dp, color = Color.LightGray, modifier = Modifier.padding(end = 7.dp).weight(1f))
                Text("또는", style = Typography.bodySmall)
                HorizontalDivider(thickness = 0.7.dp, color = Color.LightGray, modifier = Modifier.padding(start = 7.dp).weight(1f))
            }
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly) {
                Image(painter = painterResource(R.drawable.ic_signin_google),
                    contentDescription = "구글 소셜 로그인",
                    modifier = Modifier.size(50.dp)
                )
                Image(painter = painterResource(R.drawable.ic_signin_kakao),
                    contentDescription = "카카오 소셜 로그인",
                    modifier = Modifier.size(50.dp)
                )
            }

            Row() {
                Text("계정이 없으신가요? ", style = Typography.bodySmall)
                Text("회원가입",
                    style = Typography.bodySmall,
                    color = primary,
                    fontWeight = FontWeight.Bold,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable(){viewModel.moveToSignUp()})
            }
        }
    }

    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Gray.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = Color.LightGray)
        }
    }
    uiState.error?.let {
        ShowToast(it)
    }
}