package mx.empos.composefauthui.presentation

import android.app.Activity
import android.content.Intent
import androidx.activity.ComponentActivity
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
    val activity = context as? ComponentActivity

    val authRepository: AuthRepository = remember {
        AndroidAuthRepository(context)
    }

    var authState by remember { mutableStateOf(AuthState.IDLE) }
    var wasInBackground by remember { mutableStateOf(false) }
    var hasUserCanceled by remember { mutableStateOf(false) }
    var authLaunchTime by remember { mutableStateOf(0L) }

    println("DEBUG authState: $authState")
    println("DEBUG wasInBackground: $wasInBackground")
    println("DEBUG hasUserCanceled: $hasUserCanceled")
    println("DEBUG LifecycleOwner: ${lifecycleOwner::class.java.simpleName}")
    println("DEBUG Activity: ${activity?.javaClass?.simpleName}")

    val signInLauncher =
        rememberLauncherForActivityResult(FirebaseAuthUIActivityResultContract()) { result ->
            val response = result.idpResponse
            val currentTime = System.currentTimeMillis()
            val timeSinceLaunch = currentTime - authLaunchTime

            println("DEBUG Launcher result: ${result.resultCode}")
            println("DEBUG Response: $response")
            println("DEBUG Time since launch: ${timeSinceLaunch}ms")
            println("DEBUG wasInBackground at result: $wasInBackground")

            when {
                result.resultCode == Activity.RESULT_OK -> {
                    println("DEBUG Success result")
                    authState = AuthState.COMPLETED
                    fauthResult(FauthSignInResult.Success)
                }

                result.resultCode == Activity.RESULT_CANCELED && response == null -> {
                    println("DEBUG Cancel result - wasInBackground: $wasInBackground, timeSinceLaunch: $timeSinceLaunch")

                    // Si la cancelación ocurrió muy rápido (menos de 2 segundos) es probablemente técnica
                    val isProbablyTechnicalCancel = (timeSinceLaunch < 2000) || wasInBackground

                    if (isProbablyTechnicalCancel) {
                        println("DEBUG Probable technical cancellation, retrying...")
                        // Cancelación técnica → reintentar
                        authState = AuthState.LAUNCHING_AUTH
                        wasInBackground = false // Reset para próximo intento
                    } else {
                        println("DEBUG User cancellation detected")
                        // Cancelación real del usuario
                        authState = AuthState.COMPLETED
                        hasUserCanceled = true
                        fauthResult(FauthSignInResult.Destroy)
                    }
                }

                else -> {
                    println("DEBUG Error result")
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
                if (!hasUserCanceled) {
                    try {
                        // Pequeño delay si es un reintento
                        if (wasInBackground) {
                            println("DEBUG Adding retry delay...")
                            delay(500)
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
        println("DEBUG Setting up lifecycle observer")

        val observer = LifecycleEventObserver { source, event ->
            println("DEBUG Lifecycle event: $event from $source")
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    println("DEBUG ON_PAUSE - authState: $authState")
                    if (authState == AuthState.WAITING_RESULT) {
                        println("DEBUG Setting wasInBackground = true")
                        wasInBackground = true
                    }
                }

                Lifecycle.Event.ON_RESUME -> {
                    println("DEBUG ON_RESUME")
                    scope.launch {
                        delay(300) // Reducido el delay
                        println("DEBUG Setting wasInBackground = false after delay")
                        wasInBackground = false
                    }
                }

                Lifecycle.Event.ON_START -> println("DEBUG ON_START")
                Lifecycle.Event.ON_STOP -> println("DEBUG ON_STOP")
                Lifecycle.Event.ON_CREATE -> println("DEBUG ON_CREATE")
                Lifecycle.Event.ON_DESTROY -> println("DEBUG ON_DESTROY")

                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        println("DEBUG Lifecycle observer added to: ${lifecycleOwner.lifecycle}")

        // También agregar el observer directamente a la Activity si está disponible
        activity?.lifecycle?.addObserver(observer)
        if (activity != null) {
            println("DEBUG Lifecycle observer also added to activity: ${activity.lifecycle}")
        }

        onDispose {
            println("DEBUG Removing lifecycle observer")
            lifecycleOwner.lifecycle.removeObserver(observer)
            activity?.lifecycle?.removeObserver(observer)
        }
    }
}