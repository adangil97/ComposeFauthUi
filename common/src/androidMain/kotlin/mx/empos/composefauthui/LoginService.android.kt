package mx.empos.composefauthui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.FirebaseAuthUIActivityResultContract
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class LoginService(private val fauthConfiguration: FauthConfiguration) {
    private val signInState: MutableSharedFlow<FauthSignInResult> = MutableSharedFlow()
    val state: SharedFlow<FauthSignInResult?> = signInState.asSharedFlow()

    suspend fun initialize() {
        if (FirebaseAuth.getInstance().currentUser != null) {
            signInState.emit(FauthSignInResult.Success)
        }
    }

    private fun createSignInIntent(): Intent {
        val androidConfiguration = fauthConfiguration.androidConfiguration

        val providers = fauthConfiguration.providers.map {
            when (it) {
                is FauthProviders.Email -> AuthUI.IdpConfig.EmailBuilder()
                    .apply {
                        this.setAllowNewAccounts(it.androidProviderConfiguration.allowNewAccounts)
                        this.setRequireName(it.androidProviderConfiguration.requireName)
                    }
                    .build()

                FauthProviders.Facebook -> AuthUI.IdpConfig.FacebookBuilder().build()
                FauthProviders.Google -> AuthUI.IdpConfig.GoogleBuilder().build()
            }
        }

        return AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setIsSmartLockEnabled(androidConfiguration.isSmartLockEnabled)
            .setAvailableProviders(providers)
            .setLogo(androidConfiguration.logo)
            .setTosAndPrivacyPolicyUrls(
                androidConfiguration.tosUrl,
                androidConfiguration.privacyPolicyUrl
            )
            .setTheme(androidConfiguration.theme)
            .build()
    }

    @Composable
    fun Launch() {
        val signInLauncher =
            rememberLauncherForActivityResult(FirebaseAuthUIActivityResultContract()) { result ->
                if (result.resultCode == AppCompatActivity.RESULT_OK) {
                    signInState.tryEmit(FauthSignInResult.Success)
                } else {
                    val response = result.idpResponse
                    signInState.tryEmit(
                        FauthSignInResult.Error(
                            response?.error ?: Exception("An unknown error occurred")
                        )
                    )
                }
            }
        LaunchedEffect(true) {
            signInLauncher.launch(createSignInIntent())
        }
    }
}