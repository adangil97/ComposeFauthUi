package io.empos.composefauthui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView

@Composable
actual fun FauthUiContent(
    fauthConfiguration: FauthConfiguration,
    fauthResult: (FauthSignInResult) -> Unit
) {
    val loginService = remember {
        LoginService(fauthConfiguration)
    }

    val loginState by loginService.state.collectAsState(null)
    loginState?.let(fauthResult) ?: run {
        val loginView = remember { loginService.getAuthPreview() }
        UIKitView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                loginView
            }
        )
    }
    LaunchedEffect(false) {
        loginService.initialize()
    }
}