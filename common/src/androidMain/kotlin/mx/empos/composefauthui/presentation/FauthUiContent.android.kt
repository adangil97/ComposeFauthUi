package mx.empos.composefauthui.presentation

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

    var isAuthLaunching by remember { mutableStateOf(false) }
    var appWentToBackgroundDuringAuth by remember { mutableStateOf(false) }
    var authLaunchTime by remember { mutableStateOf(0L) }

    val signInLauncher =
        rememberLauncherForActivityResult(FirebaseAuthUIActivityResultContract()) { result ->
            val timeSinceLaunch = System.currentTimeMillis() - authLaunchTime
            isAuthLaunching = false

            println("DEBUG Launcher result: ${result.resultCode}")
            println("DEBUG Time since launch: ${timeSinceLaunch}ms")
            println("DEBUG App went to background during auth: $appWentToBackgroundDuringAuth")

            when {
                result.resultCode == Activity.RESULT_OK -> {
                    println("DEBUG Auth success")
                    appWentToBackgroundDuringAuth = false
                    fauthResult(FauthSignInResult.Success)
                }

                result.resultCode == Activity.RESULT_CANCELED && result.idpResponse == null -> {

                    println("DEBUG User cancellation, calling Destroy")
                    fauthResult(FauthSignInResult.Destroy)

                    appWentToBackgroundDuringAuth = false
                }

                else -> {
                    println("DEBUG Auth error")
                    appWentToBackgroundDuringAuth = false
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

    // Escuchar eventos de pantalla
    LaunchedEffect(screenManager) {
        screenManager
            .screenState
            .collect { event ->
                println("DEBUG Screen event: $event")
                println("DEBUG Screen event: ${event.screenName}")

                when (event) {
                    is ScreenEvent.Stopped -> {
                        if (isAuthLaunching) {
                            println("DEBUG App went to background during auth")
                            appWentToBackgroundDuringAuth = true
                        }
                    }

                    is ScreenEvent.Started -> {
                        // App volvió al foreground
                        println("DEBUG App came back to foreground")
                    }

                    else -> {
                        // Otros eventos no nos interesan por ahora
                    }
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
                        isAuthLaunching = true
                        authLaunchTime = System.currentTimeMillis()
                        appWentToBackgroundDuringAuth = false
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