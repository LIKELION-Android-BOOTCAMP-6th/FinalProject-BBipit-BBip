package com.bbip.bbipit.presentation.auth.ui.components

import android.text.Html
import android.text.method.ScrollingMovementMethod
import android.widget.TextView
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.content.res.ResourcesCompat
import com.bbip.bbipit.R
import com.bbip.bbipit.core.ui.theme.Typography
import com.bbip.bbipit.core.ui.theme.fontDefault
import com.bbip.bbipit.core.ui.theme.primary
import com.bbip.bbipit.core.ui.theme.subBackground
import com.bbip.bbipit.presentation.auth.ui.TermsType

@Composable
fun AgreeDialog(terms: String, type: TermsType, isSignUp: Boolean = false, onNext: () -> Unit, onDismissRequest: (Boolean) -> Unit, ){
    val scrollState: ScrollState = rememberScrollState()
    var isScrollEnd by remember { mutableStateOf(false) }

    //스크롤 상태 파악, 끝까지 내렸으면 다음 내용으로 넘어가는 버튼 활성화
    LaunchedEffect(scrollState.value, scrollState.maxValue) {
        if (scrollState.maxValue == 0) {
            isScrollEnd = true
        } else {
            isScrollEnd = scrollState.value >= (scrollState.maxValue - 5)
        }
    }
    //스크롤 초기화. 다음 장으로 넘어갔을 시 최상단으로 위치 변경
    LaunchedEffect(type, terms) {
        scrollState.scrollTo(0)
        isScrollEnd = false
    }

    Dialog(onDismissRequest = {}) {
        Card(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.8f).padding(5.dp)
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = subBackground
            )
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                IconButton(
                    onClick = { onDismissRequest(false) },
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "닫기",
                        tint = Color.Gray
                    )
                }
            }
            Column(modifier = Modifier.fillMaxSize()) {
                //html 스타일 그대로 적용하기 위해 안드로이드뷰 사용
                AndroidView(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .weight(1f)
                        .padding(16.dp)
                        .verticalScroll(scrollState),
                    factory = { context ->
                        TextView(context).apply {
                            textSize = Typography.bodySmall.fontSize.value
                            setTextColor(fontDefault.toArgb())
                            setLineSpacing(Typography.bodyMedium.lineHeight.value, 1.0f)
                            typeface = ResourcesCompat.getFont(context, R.font.suit_variable)
                            text = Html.fromHtml(terms, Html.FROM_HTML_MODE_LEGACY)
                            movementMethod = ScrollingMovementMethod()
                        }
                    },
                    update = { textView ->
                        // 데이터가 변경되면 텍스트뷰 내용 갱신
                        textView.text = Html.fromHtml(terms, Html.FROM_HTML_MODE_LEGACY)
                        textView.scrollTo(0, 0)

                    }
                )
                if(isSignUp){
                    Button(
                        onClick = onNext,
                        enabled = isScrollEnd,
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primary, disabledContainerColor = Color.Gray),

                        shape = RoundedCornerShape(20.dp)) {
                        Text(if(type == TermsType.PRIVACY)"동의하고 다음" else "동의하고 가입하기", style = Typography.bodyMedium, color = subBackground)
                    }
                }

            }
        }
    }
}