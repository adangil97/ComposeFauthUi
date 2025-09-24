package mx.empos.composefauthui.presentation

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import mx.empos.composefauthui.data.AuthRepository
import mx.empos.composefauthui.domain.FauthConfiguration
import mx.empos.composefauthui.domain.FauthSignInResult
import mx.empos.composefauthui.framework.AndroidAuthRepository

@Composable
actual fun FauthUiContent(
    fauthConfiguration: FauthConfiguration,
    fauthUiLauncher: FauthUiLauncher,
    fauthResult: (FauthSignInResult) -> Unit
) {
    val context = LocalContext.current
    val authRepository: AuthRepository = remember {
        AndroidAuthRepository(context)
    }

    LaunchedEffect(Unit) {
        if (!authRepository.userAlreadyLogin()) {
            try {
                authRepository.configure(fauthConfiguration)
                when (val uiComponent = authRepository.uiComponent) {
                    is Intent -> {
                        (fauthUiLauncher as? AndroidFauthUiLauncher)?.let {
                            it.intent = uiComponent
                            it.launch()
                        }
                    }

                    else -> fauthResult(
                        FauthSignInResult.Error(Exception("Invalid UI component type"))
                    )
                }
            } catch (e: Exception) {
                fauthResult(FauthSignInResult.Error(e))
            }
        } else {
            fauthResult(FauthSignInResult.Success)
        }
    }
}