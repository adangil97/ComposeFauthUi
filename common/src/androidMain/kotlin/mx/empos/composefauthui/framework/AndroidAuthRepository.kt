package mx.empos.composefauthui.framework

import android.content.Context
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import mx.empos.composefauthui.data.AuthRepository
import mx.empos.composefauthui.domain.FauthConfiguration
import mx.empos.composefauthui.domain.FauthProviders

class AndroidAuthRepository(
    private val context: Context
) : AuthRepository {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val authUi: AuthUI = AuthUI.getInstance()

    override var uiComponent: Any? = null

    override fun userAlreadyLogin(): Boolean {
        return auth.currentUser != null
    }

    override fun configure(
        fauthConfiguration: FauthConfiguration,
        callback: () -> Any
    ) {
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

        uiComponent = authUi
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

    override suspend fun logout() {
        authUi.signOut(context).await()
    }

    override suspend fun getAuthToken(refresh: Boolean): String {
        return auth
            .currentUser
            ?.getIdToken(refresh)
            ?.await()?.token.orEmpty()
    }
}