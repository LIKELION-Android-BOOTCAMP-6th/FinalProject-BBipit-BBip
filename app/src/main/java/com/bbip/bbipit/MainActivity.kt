package com.bbip.bbipit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bbip.bbipit.core.component.BackgroundBox
import com.bbip.bbipit.ui.theme.BbipitTheme
import com.bbip.bbipit.ui.theme.Typography

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BbipitTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color.Gray
                ) { innerPadding ->
                    BackgroundBox {
                        Column(
                            modifier = Modifier
                                .padding(innerPadding)
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text("BBipp", style = Typography.titleLarge)
                            Text("기본 글씨", style = Typography.bodyMedium)
                            Text("힌트", style = Typography.bodySmall)
                        }
                    }

                }
            }
        }
    }
}


