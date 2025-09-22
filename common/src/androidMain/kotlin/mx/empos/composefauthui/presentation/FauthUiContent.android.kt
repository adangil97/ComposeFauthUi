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
    fauthResult: (FauthSignInResult) -> Unit
) {
    val context = LocalContext.current
    val authRepository: AuthRepository = remember {
        AndroidAuthRepository(context)
    }

    val signInLauncher =
        rememberLauncherForActivityResult(FirebaseAuthUIActivityResultContract()) { result ->
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

    LaunchedEffect(Unit) {
        if (authRepository.userAlreadyLogin()) {
            fauthResult(FauthSignInResult.Success)
        } else {
            authRepository.configure(fauthConfiguration)
            (authRepository.uiComponent as? Intent)?.let { intent ->
                signInLauncher.launch(intent)
            }
        }
    }
}