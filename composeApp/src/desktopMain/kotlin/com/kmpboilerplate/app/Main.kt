package com.kmpboilerplate.app

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.kmpboilerplate.infrastructure.config.bootstrap

private const val WINDOW_TITLE = "KMP Boilerplate"

fun main() {
    bootstrap()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = WINDOW_TITLE,
        ) {
            App()
        }
    }
}
