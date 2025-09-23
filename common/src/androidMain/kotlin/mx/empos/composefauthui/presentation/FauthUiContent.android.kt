package mx.empos.composefauthui.presentation

import android.app.Activity
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import kotlinx.coroutines.delay
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
    val activity = context as? ComponentActivity

    val authRepository: AuthRepository = remember {
        AndroidAuthRepository(context)
    }

    var authState by remember { mutableStateOf(AuthState.IDLE) }
    var appWentToBackground by remember { mutableStateOf(false) }
    var authLaunchTime by remember { mutableLongStateOf(0L) }
    var lastPauseTime by remember { mutableLongStateOf(0L) }

    println("DEBUG authState: $authState")
    println("DEBUG appWentToBackground: $appWentToBackground")

    val signInLauncher =
        rememberLauncherForActivityResult(FirebaseAuthUIActivityResultContract()) { result ->
            val response = result.idpResponse
            val currentTime = System.currentTimeMillis()
            val timeSinceLaunch = currentTime - authLaunchTime
            val timeSincePause = currentTime - lastPauseTime

            println("DEBUG Launcher result: ${result.resultCode}")
            println("DEBUG Time since launch: ${timeSinceLaunch}ms")
            println("DEBUG Time since last pause: ${timeSincePause}ms")
            println("DEBUG appWentToBackground: $appWentToBackground")

            when {
                result.resultCode == Activity.RESULT_OK -> {
                    println("DEBUG Success result")
                    authState = AuthState.COMPLETED
                    appWentToBackground = false
                    fauthResult(FauthSignInResult.Success)
                }

                result.resultCode == Activity.RESULT_CANCELED && response == null -> {
                    // Criterios para detectar cancelación técnica:
                    // 1. La app fue a background Y el resultado llegó muy rápido después de volver
                    // 2. O el resultado llegó muy rápido en general (menos de 1 segundo)
                    val isQuickCancel = timeSinceLaunch < 1000
                    val isBackgroundCancel = appWentToBackground && timeSincePause < 2000
                    val isProbablyTechnical = isQuickCancel || isBackgroundCancel

                    println("DEBUG Cancel analysis:")
                    println("  - isQuickCancel: $isQuickCancel")
                    println("  - isBackgroundCancel: $isBackgroundCancel")
                    println("  - isProbablyTechnical: $isProbablyTechnical")

                    if (isProbablyTechnical) {
                        println("DEBUG Probable technical cancellation, retrying...")
                        authState = AuthState.LAUNCHING_AUTH
                        appWentToBackground = false
                    } else {
                        println("DEBUG User cancellation detected")
                        authState = AuthState.COMPLETED
                        appWentToBackground = false
                        fauthResult(FauthSignInResult.Destroy)
                    }
                }

                else -> {
                    println("DEBUG Error result")
                    authState = AuthState.COMPLETED
                    appWentToBackground = false
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
                println("DEBUG LaunchedEffect: IDLE -> CHECKING_LOGIN")
                authState = AuthState.CHECKING_LOGIN
            }

            AuthState.CHECKING_LOGIN -> {
                println("DEBUG LaunchedEffect: Checking login...")
                if (authRepository.userAlreadyLogin()) {
                    authState = AuthState.COMPLETED
                    fauthResult(FauthSignInResult.Success)
                } else {
                    authState = AuthState.LAUNCHING_AUTH
                }
            }

            AuthState.LAUNCHING_AUTH -> {
                println("DEBUG LaunchedEffect: Launching auth...")
                try {
                    // Pequeño delay si es un reintento
                    if (appWentToBackground) {
                        println("DEBUG Adding retry delay...")
                        delay(300)
                    }

                    authRepository.configure(fauthConfiguration)
                    when (val uiComponent = authRepository.uiComponent) {
                        is Intent -> {
                            authState = AuthState.WAITING_RESULT
                            authLaunchTime = System.currentTimeMillis()
                            println("DEBUG Launching Intent at: $authLaunchTime")
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
            }

            AuthState.WAITING_RESULT,
            AuthState.COMPLETED -> Unit
        }
    }

    DisposableEffect(lifecycleOwner) {
        println("DEBUG Setting up lifecycle observer")

        val observer = LifecycleEventObserver { source, event ->
            println("DEBUG Lifecycle event: $event from ${source::class.java.simpleName}")
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    println("DEBUG ON_PAUSE - authState: $authState")
                    if (authState == AuthState.WAITING_RESULT) {
                        lastPauseTime = System.currentTimeMillis()
                        appWentToBackground = true
                        println("DEBUG Marking app as went to background at: $lastPauseTime")
                    }
                }

                Lifecycle.Event.ON_STOP -> {
                    println("DEBUG ON_STOP")
                    // Confirmamos que definitivamente fuimos a background
                    if (authState == AuthState.WAITING_RESULT) {
                        appWentToBackground = true
                        println("DEBUG Confirmed app went to background (ON_STOP)")
                    }
                }

                Lifecycle.Event.ON_START -> {
                    println("DEBUG ON_START")
                    // NO reseteamos appWentToBackground aquí
                    // Lo dejamos para que se detecte en el launcher result
                }

                Lifecycle.Event.ON_RESUME -> {
                    println("DEBUG ON_RESUME")
                    // Tampoco reseteamos aquí, dejamos que el launcher lo maneje
                }

                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        activity?.lifecycle?.addObserver(observer)

        onDispose {
            println("DEBUG Removing lifecycle observer")
            lifecycleOwner.lifecycle.removeObserver(observer)
            activity?.lifecycle?.removeObserver(observer)
        }
    }
}