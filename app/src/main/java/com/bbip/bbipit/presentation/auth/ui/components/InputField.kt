package com.bbip.bbipit.presentation.auth.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.bbip.bbipit.core.ui.theme.Typography

@Composable
fun InputField(value: String, onValueChange: (String) -> Unit,
               placeholder: String,
               isPassword: Boolean = false,
               keyboardType: KeyboardType,
               errorText: String? = null){

    var passwordVisible by remember { mutableStateOf(false) }

    val isError = errorText !=null

    Column(modifier = Modifier.fillMaxWidth()) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = Typography.bodyMedium,
            isError = isError,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent
            ),
            shape = RoundedCornerShape(60.dp),

            placeholder = {
                Text(
                    text = placeholder,
                    style = Typography.bodySmall
                )
            },
            modifier = Modifier.fillMaxWidth()
                .shadow(
                    elevation = 6.dp,
                    shape = RoundedCornerShape(60.dp)
                ),
            keyboardOptions =  KeyboardOptions(keyboardType = keyboardType),
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

        if (isError && !errorText.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = errorText,
                color = Color.Red,
                style = Typography.bodySmall,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }


}