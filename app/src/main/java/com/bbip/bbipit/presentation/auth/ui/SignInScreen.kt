package com.bbip.bbipit.presentation.auth.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bbip.bbipit.R
import com.bbip.bbipit.core.navigation.Routes
import com.bbip.bbipit.core.ui.theme.Typography
import com.bbip.bbipit.core.ui.theme.background
import com.bbip.bbipit.core.ui.theme.primary
import com.bbip.bbipit.presentation.auth.viewmodel.SignInEvent
import com.bbip.bbipit.presentation.auth.viewmodel.SignInViewModel

@Composable
fun SignInScreen(navController: NavController, viewModel: SignInViewModel = hiltViewModel()) {

    val focusManager = LocalFocusManager.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

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

    Column(modifier = Modifier.fillMaxSize().padding(30.dp)
        .background(background)
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ){
            focusManager.clearFocus()
        },
        verticalArrangement = Arrangement.spacedBy(17.dp, alignment = Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally) {

        Image(painter = painterResource(R.drawable.logo),
            contentDescription = "로고",
            modifier = Modifier.size(120.dp).padding(top = 20.dp)
        )
        Text("삐빗- 심장이 반응하는 거리", style = Typography.bodySmall)
        Spacer(modifier = Modifier.height(17.dp))
        Text("이메일", style = Typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
        InputField(value = email,
            onValueChange = {email = it},
            placeholder = "이메일을 입력해주세요")
        Text("비밀번호", style = Typography.bodyMedium, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
        InputField(value = password,
            onValueChange = {password = it},
            placeholder = "비밀번호를 입력해주세요",
            isPassword = true)

        Button({viewModel.signIn()},
            colors = ButtonDefaults.buttonColors(primary),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
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
                modifier = Modifier.clickable(){viewModel.signUp()})
        }
    }



}


@Composable
fun InputField(value: String, onValueChange: (String) -> Unit, placeholder: String, isPassword: Boolean = false){

    var passwordVisible by remember { mutableStateOf(false) }

    TextField(
        value = value,
        onValueChange = onValueChange,
        textStyle = Typography.bodyMedium,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent
        ),
        shape = RoundedCornerShape(8.dp),

        placeholder = {
            Text(
                text = placeholder,
                style = Typography.bodySmall
            )
        },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions =  KeyboardOptions(keyboardType = if(!isPassword) KeyboardType.Email else KeyboardType.Password),
        singleLine = true,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation()
        else VisualTransformation.None,
        trailingIcon = {
            if (isPassword){
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = if(passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "비밀번호 마스킹",
                        tint = Color.LightGray)
                }
            }

        }
    )

}