package com.bbip.bbipit.presentation.auth.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.bbip.bbipit.R
import com.bbip.bbipit.core.ui.theme.Typography
import com.bbip.bbipit.core.ui.theme.fontDefault
import com.bbip.bbipit.core.ui.theme.primary
import com.bbip.bbipit.presentation.base.BackgroundBox

@Composable
fun SignInScreen(navController: NavController) {

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    BackgroundBox {
        Box(modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center) {
            Card(modifier = Modifier.fillMaxHeight(0.8f).fillMaxWidth(0.8f),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFEDEDED)),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)) {
                Column(modifier = Modifier.padding(10.dp).fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(17.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {

                    Image(painter = painterResource(R.drawable.logo),
                        contentDescription = "로고",
                        modifier = Modifier.size(120.dp).padding(top = 20.dp)
                    )

                    Text("BBip-It", style = Typography.titleLarge)
                    Text("심장이 반응하는 거리, 삐빗-", style = Typography.bodySmall)

                    Column(modifier = Modifier.fillMaxWidth().padding(7.dp),
                        verticalArrangement = Arrangement.spacedBy(17.dp),
                        horizontalAlignment = Alignment.Start) {
                        Spacer(modifier = Modifier.height(17.dp))
                        Text("이메일", style = Typography.bodyMedium, fontWeight = FontWeight.Bold)
                        InputField(value = email,
                            onValueChange = {email = it},
                            placeholder = "이메일을 입력해주세요")
                        Text("비밀번호", style = Typography.bodyMedium, fontWeight = FontWeight.Bold)
                        InputField(value = password,
                            onValueChange = {password = it},
                            placeholder = "비밀번호를 입력해주세요",
                            isPassword = true)

                        Button({},
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
                    }

                    Row() {
                        Text("계정이 없으신가요? ", style = Typography.bodySmall)
                        Text("회원가입", style = Typography.bodySmall, color = fontDefault, fontWeight = FontWeight.Bold)
                    }

                }

            }

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