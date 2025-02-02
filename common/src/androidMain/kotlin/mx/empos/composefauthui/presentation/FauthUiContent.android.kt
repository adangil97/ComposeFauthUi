package mx.empos.composefauthui.presentation

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
    val authRepository: AuthRepository = AndroidAuthRepository(LocalContext.current)
    val signInLauncher =
        rememberLauncherForActivityResult(FirebaseAuthUIActivityResultContract()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
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
    LaunchedEffect(true) {
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