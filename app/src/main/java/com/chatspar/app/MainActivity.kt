package com.chatspar.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.chatspar.app.ui.navigation.AppNavGraph
import com.chatspar.app.ui.theme.ChatSparTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChatSparTheme {
                AppNavGraph()
            }
        }
    }
}
