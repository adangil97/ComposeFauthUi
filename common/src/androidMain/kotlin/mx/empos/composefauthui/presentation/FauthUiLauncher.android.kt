package mx.empos.composefauthui.presentation

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.firebase.ui.auth.data.model.FirebaseAuthUIAuthenticationResult
import mx.empos.composefauthui.domain.FauthSignInResult

class AndroidFauthUiLauncher internal constructor(
    private val signInLauncher: ManagedActivityResultLauncher<Intent, FirebaseAuthUIAuthenticationResult>
) : FauthUiLauncher {
    var intent: Intent? = null

    override fun launch() {
        intent?.let { signInLauncher.launch(it) }
    }
}

@Composable
actual fun rememberFauthUiLauncher(fauthResult: (FauthSignInResult) -> Unit): FauthUiLauncher {
    val signInLauncher =
        rememberLauncherForActivityResult(FirebaseAuthUIActivityResultContract()) { result ->
            when {
                result.resultCode == Activity.RESULT_OK -> {
                    fauthResult(FauthSignInResult.Success)
                }

                result.resultCode == Activity.RESULT_CANCELED && result.idpResponse == null -> {
                    fauthResult(FauthSignInResult.UserCancellation)
                }

                else -> {
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
    return remember {
        AndroidFauthUiLauncher(signInLauncher)
    }
}