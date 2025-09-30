package mx.empos.composefauthui.framework

import android.content.Context
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GetTokenResult
import mx.empos.composefauthui.data.AuthRepository
import mx.empos.composefauthui.domain.FauthConfiguration
import mx.empos.composefauthui.domain.FauthProviders
import mx.empos.composefauthui.domain.FauthResult

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
        val commonConfiguration = fauthConfiguration.commonConfiguration
        val androidConfiguration = fauthConfiguration.androidConfiguration

        val providers = fauthConfiguration.providers.map {
            when (it) {
                is FauthProviders.Email -> {
                    AuthUI.IdpConfig.EmailBuilder()
                        .apply {
                            with(it.commonProviderConfiguration) {
                                if (forceSameDevice) {
                                    setForceSameDevice()
                                }
                                setAllowNewAccounts(allowNewAccounts)
                                if (allowNewAccounts.not()) {
                                    hideEmailSignUp()
                                }
                                setRequireName(requireName)
                            }
                        }
                        .build()
                }

                is FauthProviders.Facebook -> AuthUI
                    .IdpConfig
                    .FacebookBuilder()
                    .apply {
                        this.setPermissions(it.extraPermissions)
                    }
                    .build()

                FauthProviders.Google -> AuthUI.IdpConfig.GoogleBuilder().build()
                FauthProviders.Phone -> AuthUI.IdpConfig.PhoneBuilder().build()
            }
        }

        uiComponent = authUi
            .createSignInIntentBuilder()
            .setCredentialManagerEnabled(androidConfiguration.credentialManagerEnabled)
            .setAvailableProviders(providers)
            .setLogo(androidConfiguration.logo)
            .setTosAndPrivacyPolicyUrls(
                commonConfiguration.tosUrl,
                commonConfiguration.privacyPolicyUrl
            )
            .setTheme(androidConfiguration.theme)
            .build()
    }

    override suspend fun logout() {
        authUi.signOut(context).await()
    }

    override suspend fun getAuthToken(refresh: Boolean): FauthResult? {
        return getIdToken(refresh)?.let {
            FauthResult(
                token = it.token.orEmpty(),
                timestamp = (it.expirationTimestamp * 1000)
            )
        }
    }

    private suspend fun getIdToken(refresh: Boolean): GetTokenResult? {
        return auth.currentUser?.getIdToken(refresh)?.await()
    }
}