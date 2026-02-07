package com.kmpboilerplate.app

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.kmpboilerplate.infrastructure.config.bootstrap

fun main() =
    application {
        bootstrap()

        Window(
            onCloseRequest = ::exitApplication,
            title = "KMP Boilerplate",
        ) {
            App()
        }
    }
