package mx.empos.composefauthui.presentation

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mx.empos.composefauthui.data.AuthRepository
import mx.empos.composefauthui.domain.FauthConfiguration
import mx.empos.composefauthui.domain.FauthSignInResult
import mx.empos.composefauthui.framework.AndroidAuthRepository

enum class AuthState {
    IDLE,
    CHECKING_LOGIN,
    LAUNCHING_AUTH,
    WAITING_RESULT,
    COMPLETED
}

@Composable
actual fun FauthUiContent(
    fauthConfiguration: FauthConfiguration,
    fauthResult: (FauthSignInResult) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val authRepository: AuthRepository = remember {
        AndroidAuthRepository(context)
    }

    var authState by remember { mutableStateOf(AuthState.IDLE) }
    var wasInBackground by remember { mutableStateOf(false) }
    var hasUserCanceled by remember { mutableStateOf(false) }

    val signInLauncher =
        rememberLauncherForActivityResult(FirebaseAuthUIActivityResultContract()) { result ->
            val response = result.idpResponse

            when {
                result.resultCode == Activity.RESULT_OK -> {
                    authState = AuthState.COMPLETED
                    fauthResult(FauthSignInResult.Success)
                }

                result.resultCode == Activity.RESULT_CANCELED && response == null -> {
                    if (wasInBackground) {
                        // Cancelación técnica → ignorar, no reportar
                        authState = AuthState.IDLE
                    } else {
                        // Cancelación real del usuario
                        authState = AuthState.COMPLETED
                        hasUserCanceled = true
                        fauthResult(FauthSignInResult.Destroy)
                    }
                }

                else -> {
                    authState = AuthState.COMPLETED
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

    LaunchedEffect(authState) {
        when (authState) {
            AuthState.IDLE -> {
                authState = AuthState.CHECKING_LOGIN
            }

            AuthState.CHECKING_LOGIN -> {
                if (authRepository.userAlreadyLogin()) {
                    authState = AuthState.COMPLETED
                    fauthResult(FauthSignInResult.Success)
                } else {
                    authState = AuthState.LAUNCHING_AUTH
                }
            }

            AuthState.LAUNCHING_AUTH -> {
                if (!hasUserCanceled) {
                    try {
                        authRepository.configure(fauthConfiguration)
                        when (val uiComponent = authRepository.uiComponent) {
                            is Intent -> {
                                authState = AuthState.WAITING_RESULT
                                signInLauncher.launch(uiComponent)
                            }

                            else -> {
                                authState = AuthState.COMPLETED
                                fauthResult(
                                    FauthSignInResult.Error(
                                        Exception("Invalid UI component type")
                                    )
                                )
                            }
                        }
                    } catch (exception: Exception) {
                        authState = AuthState.COMPLETED
                        fauthResult(FauthSignInResult.Error(exception))
                    }
                } else {
                    hasUserCanceled = false
                }
            }

            AuthState.WAITING_RESULT,
            AuthState.COMPLETED -> Unit
        }
    }

    val scope = rememberCoroutineScope()
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if (authState == AuthState.WAITING_RESULT) {
                        wasInBackground = true
                    }
                }

                Lifecycle.Event.ON_RESUME -> {
                    scope.launch {
                        delay(100)
                        wasInBackground = false
                    }
                }

                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
}