package mx.empos.composefauthui.presentation

import androidx.compose.runtime.Composable
import mx.empos.composefauthui.domain.FauthConfiguration
import mx.empos.composefauthui.domain.FauthSignInResult

@Composable
expect fun FauthUiContent(
    fauthConfiguration: FauthConfiguration,
    screenManager: ScreenManager,
    onEvent: (String) -> Unit,
    fauthResult: (FauthSignInResult) -> Unit
)