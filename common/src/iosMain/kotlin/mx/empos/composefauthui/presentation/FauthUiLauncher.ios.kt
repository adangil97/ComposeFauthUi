package mx.empos.composefauthui.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import mx.empos.composefauthui.domain.FauthSignInResult

class IosFauthUiLauncher internal constructor() : FauthUiLauncher {
    override fun launch() = Unit
}

@Composable
actual fun rememberFauthUiLauncher(
    fauthResult: (FauthSignInResult) -> Unit
): FauthUiLauncher {
    return remember {
        IosFauthUiLauncher()
    }
}