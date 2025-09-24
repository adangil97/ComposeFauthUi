package mx.empos.composefauthui.presentation

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import mx.empos.composefauthui.data.AuthRepository
import mx.empos.composefauthui.domain.FauthConfiguration
import mx.empos.composefauthui.domain.FauthSignInResult
import mx.empos.composefauthui.framework.AndroidAuthRepository
import kotlin.time.Duration.Companion.seconds

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
    var loginLaunched by remember { mutableStateOf(false) }
    var launchTime by remember { mutableLongStateOf(0L) }
    onEvent("is app in background: $isAppInBackground")

    val signInLauncher =
        rememberLauncherForActivityResult(FirebaseAuthUIActivityResultContract()) { result ->
            loginLaunched = false // siempre limpiar
            when {
                result.resultCode == Activity.RESULT_OK -> {
                    onEvent("Auth success")
                    fauthResult(FauthSignInResult.Success)
                }

                result.resultCode == Activity.RESULT_CANCELED && result.idpResponse == null -> {
                    val currentTime = System.currentTimeMillis()
                    val timeDifference = currentTime - launchTime
                    onEvent("User cancellation, time difference: $timeDifference")
                    if (isAppInBackground.not() && loginLaunched && timeDifference >= 5L.seconds.inWholeMilliseconds) {
                        onEvent("User real cancellation, calling Destroy")
                        fauthResult(FauthSignInResult.Destroy)
                    } else {
                        onEvent("Cancellation ignored (background or lifecycle)")
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
        if (!authRepository.userAlreadyLogin()) {
            try {
                authRepository.configure(fauthConfiguration)
                when (val uiComponent = authRepository.uiComponent) {
                    is Intent -> {
                        if (!isAppInBackground) {
                            loginLaunched = true
                            launchTime = System.currentTimeMillis()
                            signInLauncher.launch(uiComponent)
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