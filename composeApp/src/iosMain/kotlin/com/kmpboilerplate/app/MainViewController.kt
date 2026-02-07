package com.kmpboilerplate.app

import androidx.compose.ui.window.ComposeUIViewController
import com.kmpboilerplate.infrastructure.config.bootstrap

fun MainViewController() =
    ComposeUIViewController(
        configure = { bootstrap() },
    ) { App() }
