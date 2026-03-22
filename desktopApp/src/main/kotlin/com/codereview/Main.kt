package com.codereview

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.codereview.ui.App

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Code Review APP (AI)"
    ) {
        App()
    }
}
