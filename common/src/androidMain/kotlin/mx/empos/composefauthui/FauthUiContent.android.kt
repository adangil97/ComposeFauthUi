package mx.empos.composefauthui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
actual fun FauthUiContent(
    fauthConfiguration: FauthConfiguration,
    fauthResult: (FauthSignInResult) -> Unit
) {
    val loginService = LoginService(fauthConfiguration)
    val loginState by loginService.state.collectAsState(null)
    loginState?.let(fauthResult) ?: loginService.Launch()
    LaunchedEffect(false) {
        loginService.initialize()
    }
}