package mx.empos.composefauthui.presentation

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import mx.empos.composefauthui.data.AuthRepository
import mx.empos.composefauthui.domain.FauthConfiguration
import mx.empos.composefauthui.domain.FauthSignInResult
import mx.empos.composefauthui.framework.AndroidAuthRepository

@Composable
actual fun FauthUiContent(
    fauthConfiguration: FauthConfiguration,
    screenManager: ScreenManager,
    fauthResult: (FauthSignInResult) -> Unit
) {
    val context = LocalContext.current
    val authRepository: AuthRepository = remember {
        AndroidAuthRepository(context)
    }

    val signInLauncher =
        rememberLauncherForActivityResult(FirebaseAuthUIActivityResultContract()) { result ->

            when {
                result.resultCode == Activity.RESULT_OK -> {
                    println("DEBUG Auth success")
                    fauthResult(FauthSignInResult.Success)
                }

                result.resultCode == Activity.RESULT_CANCELED && result.idpResponse == null -> {
                    println("DEBUG Auth canceled - isAppInBackground: ${screenManager.isAppInBackground}")

                    if (screenManager.isAppInBackground.not()) {
                        println("DEBUG Technical cancellation detected, calling Destroy")
                        fauthResult(FauthSignInResult.Destroy)
                    }
                }

                else -> {
                    println("DEBUG Auth error")
                    val response = result.idpResponse
                    val exception = response?.error ?: Exception("An unknown error occurred")
                    fauthResult(
                        FauthSignInResult.Error(
                            exception = exception,
                            errorCode = response?.error?.errorCode,
                            errorMessage = response?.error?.message
                        )
                    )
                }
            }
        }

    LaunchedEffect(Unit) {
        if (authRepository.userAlreadyLogin()) {
            println("DEBUG User already logged in")
            fauthResult(FauthSignInResult.Success)
        } else {
            try {
                authRepository.configure(fauthConfiguration)
                when (val uiComponent = authRepository.uiComponent) {
                    is Intent -> {
                        println("DEBUG Launching auth UI")
                        signInLauncher.launch(uiComponent)
                    }

                    else -> {
                        fauthResult(
                            FauthSignInResult.Error(
                                Exception("Invalid UI component type")
                            )
                        )
                    }
                }
            } catch (exception: Exception) {
                fauthResult(FauthSignInResult.Error(exception))
            }
        }
    }
}