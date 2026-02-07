package com.kmpboilerplate.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.kmpboilerplate.app.ui.screen.CatScreen
import org.koin.compose.KoinContext

@Composable
fun App() {
    KoinContext {
        MaterialTheme {
            Box(modifier = Modifier.safeContentPadding()) {
                CatScreen()
            }
        }
    }
}
