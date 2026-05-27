package com.bbip.bbipit.presentation.user.ui

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.bbip.bbipit.core.ui.theme.Typography
import com.bbip.bbipit.core.ui.theme.background
import com.bbip.bbipit.core.ui.theme.subBackground
import com.bbip.bbipit.domain.type.LoginType
import com.bbip.bbipit.domain.type.TermsType
import com.bbip.bbipit.presentation.auth.ui.components.AgreeDialog
import com.bbip.bbipit.presentation.base.ShowToast
import com.bbip.bbipit.presentation.mypage.MyPageViewmodel
import kotlin.math.log

val title = Color(0xFF7E8C9F)
enum class InputType{
    EMAIL, TERMS, LOGOUT
}

@Composable
fun SettingsDrawer(email: String, loginType: String, onClose: () -> Unit, modifier: Modifier, viewModel: MyPageViewmodel) {

    var termsType by remember { mutableStateOf<TermsType?>(null) }

    val terms by viewModel.terms.collectAsState()

    ModalDrawerSheet(modifier = modifier.systemBarsPadding(),
        drawerContainerColor = background
    ) {
        Log.d("받아오는 내용 확인", "$email, $loginType")
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Column(modifier = Modifier.fillMaxSize().padding(10.dp),
                horizontalAlignment = Alignment.Start,
//                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                Spacer(modifier = Modifier.height(20.dp))
                Text("내 계정 정보", style = Typography.bodySmall, color = title, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 10.dp))
                InfoBox(titleText = "이메일", semiText = email, type = InputType.EMAIL, loginType = LoginType.fromString(loginType)) { }

                Text("기타", style = Typography.bodySmall, color = title, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 10.dp))
                InfoBox(titleText = "서비스 이용약관", type = InputType.TERMS,
                    onClick = {
                        Log.d("드로어", terms)
                        viewModel.getTerms(TermsType.SERVICE)
                        termsType = TermsType.SERVICE
                    }
                )

                InfoBox(titleText = "개인정보처리방침", type = InputType.TERMS) {
                    viewModel.getTerms(TermsType.PRIVACY)
                    termsType = TermsType.PRIVACY
                }
                InfoBox(titleText = "로그아웃", type = InputType.LOGOUT) { viewModel.onChangeSignOutDialog(true)}
            }
        }

    }

    termsType?.let { currentType ->
        AgreeDialog(
            terms = terms,
            type = currentType,
            onNext = {},
            onDismissRequest = { termsType = null }
        )
    }
}

@Composable
fun InfoBox(titleText: String, semiText: String? = null, type: InputType, loginType: LoginType? = null, onClick: () -> Unit){


    Card(modifier = Modifier.padding(10.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(subBackground),
        onClick = {onClick()}
    ) {
        when(type){
            InputType.EMAIL -> {
                Column(modifier = Modifier.fillMaxWidth().padding(17.dp),
                    horizontalAlignment = Alignment.Start) {

                    Text(titleText, style = Typography.bodySmall, color = title, fontWeight = FontWeight.Bold )

                    Row(modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(semiText!!, style = Typography.bodyMedium)

                        if (loginType != LoginType.EMAIL){
                            Image(
                                painter = painterResource(loginType!!.img!!),
                                contentDescription = "로그인 타입",
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }
            }
            InputType.TERMS -> {
                Row(modifier = Modifier.fillMaxWidth().padding(17.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(titleText, style = Typography.bodySmall, color = title, fontWeight = FontWeight.Bold )

                    IconButton(onClick =  {onClick()} ) {
                        Icon(imageVector = Icons.Default.ChevronRight, tint = Color.LightGray, modifier = Modifier.size(30.dp), contentDescription = "약관 보기")
                    }

                }

            }
            InputType.LOGOUT -> {
                Row(modifier = Modifier.fillMaxWidth().padding(17.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(titleText, style = Typography.bodySmall, color = title, fontWeight = FontWeight.Bold )

                    IconButton(onClick =  {onClick()} ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Logout, tint = Color.LightGray, modifier = Modifier.size(30.dp), contentDescription = "로그아웃")
                    }
                }
            }
        }

    }

}