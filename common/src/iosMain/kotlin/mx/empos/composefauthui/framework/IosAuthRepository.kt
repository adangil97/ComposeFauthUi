package mx.empos.composefauthui.framework

import cocoapods.FirebaseAuthUI.FUIAuth
import cocoapods.FirebaseAuthUI.FUIAuthDelegateProtocol
import cocoapods.FirebaseEmailAuthUI.FIRActionCodeSettings
import cocoapods.FirebaseEmailAuthUI.FUIEmailAuth
import cocoapods.FirebaseEmailAuthUI.FUIPasswordSignInViewController
import cocoapods.FirebaseFacebookAuthUI.FUIFacebookAuth
import cocoapods.FirebaseGoogleAuthUI.FUIGoogleAuth
import kotlinx.cinterop.ExperimentalForeignApi
import mx.empos.composefauthui.data.AuthRepository
import mx.empos.composefauthui.domain.FauthConfiguration
import mx.empos.composefauthui.domain.FauthProviders
import platform.Foundation.NSURL
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@OptIn(ExperimentalForeignApi::class)
class IosAuthRepository : AuthRepository {
    private val authUi: FUIAuth? = FUIAuth.defaultAuthUI()

    override var uiComponent: Any? = null

    override fun userAlreadyLogin(): Boolean {
        return authUi?.auth?.currentUser() != null
    }

    override fun configure(
        fauthConfiguration: FauthConfiguration,
        callback: () -> Any
    ) {
        val iosConfiguration = fauthConfiguration.iosConfiguration
        authUi?.let { fuiAuth ->
            (callback() as? FUIAuthDelegateProtocol)?.let { delegate ->
                fuiAuth.delegate = delegate
            }
            fuiAuth.shouldHideCancelButton = true
            fuiAuth.TOSURL = NSURL(string = iosConfiguration.tosUrl)
            fuiAuth.privacyPolicyURL = NSURL(string = iosConfiguration.privacyPolicyUrl)
            fuiAuth.providers = fauthConfiguration.providers.map {
                when (it) {
                    is FauthProviders.Email -> {
                        FUIEmailAuth(
                            authAuthUI = fuiAuth,
                            signInMethod = it.iosProviderConfiguration.signInMethod,
                            forceSameDevice = it.iosProviderConfiguration.forceSameDevice,
                            allowNewEmailAccounts = it.iosProviderConfiguration.allowNewAccounts,
                            requireDisplayName = it.iosProviderConfiguration.requireName,
                            actionCodeSetting = FIRActionCodeSettings()
                        )
                    }

                    FauthProviders.Facebook -> FUIFacebookAuth(fuiAuth)
                    FauthProviders.Google -> FUIGoogleAuth(fuiAuth)
                }
            }
            fuiAuth.providers = listOf(
                FUIGoogleAuth(fuiAuth),
                FUIFacebookAuth(fuiAuth),
                FUIEmailAuth(
                    authAuthUI = fuiAuth,
                    signInMethod = "FIREmailPasswordAuthSignInMethod",
                    forceSameDevice = false,
                    allowNewEmailAccounts = false,
                    requireDisplayName = false,
                    actionCodeSetting = FIRActionCodeSettings()
                ).apply {
                    this.signInWithPresentingViewController(FUIPasswordSignInViewController())
                }
            )
            uiComponent = fuiAuth
        }
    }

    override suspend fun logout() {
        authUi?.signOutWithError(null)
    }

    override suspend fun getAuthToken(refresh: Boolean): String {
        val token = suspendCoroutine { continuation ->
            FUIAuth.defaultAuthUI()?.auth()?.currentUser()
                ?.getIDTokenResultForcingRefresh(refresh) { authResult, error ->
                    error?.let {
                        continuation.resume(null)
                    } ?: run {
                        continuation.resume(authResult?.token())
                    }
                } ?: continuation.resume(null)
        }
        return token.orEmpty()
    }
}