package mx.empos.composefauthui.presentation

import androidx.compose.runtime.Composable
import mx.empos.composefauthui.domain.FauthSignInResult

@Composable
expect fun rememberFauthUiLauncher(fauthResult: (FauthSignInResult) -> Unit): FauthUiLauncher