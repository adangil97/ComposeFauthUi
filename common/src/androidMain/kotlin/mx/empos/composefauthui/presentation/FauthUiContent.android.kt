package mx.empos.composefauthui.presentation

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
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
    onEvent: (String) -> Unit,
    fauthResult: (FauthSignInResult) -> Unit
) {
    val context = LocalContext.current
    val authRepository: AuthRepository = remember {
        AndroidAuthRepository(context)
    }

    val screenState: ScreenEvent? by screenManager.screenState.collectAsState()
    onEvent("screenState: $screenState")
    val isAppInBackground: Boolean by remember(screenState) {
        derivedStateOf {
            screenState?.screenName != screenManager.screenNameOfFirebaseAuthUiLauncher
                    && screenState is ScreenEvent.Stopped
        }
    }
    onEvent("is app in background: $isAppInBackground")

    val signInLauncher =
        rememberLauncherForActivityResult(FirebaseAuthUIActivityResultContract()) { result ->

            when {
                result.resultCode == Activity.RESULT_OK -> {
                    onEvent("Auth success")
                    fauthResult(FauthSignInResult.Success)
                }

                result.resultCode == Activity.RESULT_CANCELED && result.idpResponse == null -> {

                    onEvent("User cancellation, calling Destroy")
                    if (isAppInBackground.not()) {
                        fauthResult(FauthSignInResult.Destroy)
                    }
                }

                else -> {
                    onEvent("Auth error")
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

    LaunchedEffect(isAppInBackground) {
        if (authRepository.userAlreadyLogin()) {
            onEvent("User already logged in")
            fauthResult(FauthSignInResult.Success)
        } else {
            try {
                authRepository.configure(fauthConfiguration)
                when (val uiComponent = authRepository.uiComponent) {
                    is Intent -> {
                        onEvent("Launching auth UI $isAppInBackground")
                        if (isAppInBackground.not()) {
                            onEvent("Launching auth UI")
                            signInLauncher.launch(uiComponent)
                        }
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