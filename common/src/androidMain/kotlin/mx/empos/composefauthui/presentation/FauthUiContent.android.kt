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
    fauthResult: (FauthSignInResult) -> Unit
) {
    val context = LocalContext.current
    val authRepository: AuthRepository = remember {
        AndroidAuthRepository(context)
    }

    var isProcessing by remember { mutableStateOf(false) }

    val signInLauncher =
        rememberLauncherForActivityResult(FirebaseAuthUIActivityResultContract()) { result ->
            isProcessing = false
            if (result.resultCode == Activity.RESULT_OK) {
                fauthResult(FauthSignInResult.Success)
            } else {
                val response = result.idpResponse
                fauthResult(
                    FauthSignInResult.Error(
                        response?.error ?: Exception("An unknown error occurred")
                    )
                )
            }
        }

    LaunchedEffect(fauthConfiguration) { // Usar la configuración como key
        if (isProcessing) return@LaunchedEffect

        if (authRepository.userAlreadyLogin()) {
            fauthResult(FauthSignInResult.Success)
        } else {
            try {
                authRepository.configure(fauthConfiguration)
                when (val uiComponent = authRepository.uiComponent) {
                    is Intent -> {
                        isProcessing = true
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
            } catch (e: Exception) {
                fauthResult(FauthSignInResult.Error(e))
            }
        }
    }
}