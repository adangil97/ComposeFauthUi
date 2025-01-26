package mx.empos.composefauthui

import androidx.compose.runtime.Composable

@Composable
expect fun FauthUiContent(fauthConfiguration: FauthConfiguration, fauthResult: (FauthSignInResult) -> Unit)