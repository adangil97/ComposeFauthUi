package mx.empos.composefauthui.framework

import cocoapods.FirebaseAuthUI.FIRAuthTokenResult
import cocoapods.FirebaseAuthUI.FUIAuth
import cocoapods.FirebaseAuthUI.FUIAuthDelegateProtocol
import cocoapods.FirebaseEmailAuthUI.FIRActionCodeSettings
import cocoapods.FirebaseEmailAuthUI.FUIEmailAuth
import cocoapods.FirebaseFacebookAuthUI.FUIFacebookAuth
import cocoapods.FirebaseGoogleAuthUI.FUIGoogleAuth
import kotlinx.cinterop.ExperimentalForeignApi
import mx.empos.composefauthui.data.AuthRepository
import mx.empos.composefauthui.domain.FauthConfiguration
import mx.empos.composefauthui.domain.FauthProviders
import platform.Foundation.NSURL
import platform.Foundation.timeIntervalSince1970
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
        val commonConfiguration = fauthConfiguration.commonConfiguration
        val iosConfiguration = fauthConfiguration.iosConfiguration
        authUi?.let { fuiAuth ->
            (callback() as? FUIAuthDelegateProtocol)?.let { delegate ->
                fuiAuth.delegate = delegate
            }
            fuiAuth.shouldHideCancelButton = iosConfiguration.shouldHideCancelButton
            fuiAuth.TOSURL = NSURL(string = commonConfiguration.tosUrl)
            fuiAuth.privacyPolicyURL = NSURL(string = commonConfiguration.privacyPolicyUrl)
            fuiAuth.providers = fauthConfiguration.providers.map {
                when (it) {
                    is FauthProviders.Email -> {
                        FUIEmailAuth(
                            authAuthUI = fuiAuth,
                            signInMethod = it.iosProviderConfiguration.signInMethod,
                            forceSameDevice = it.commonProviderConfiguration.forceSameDevice,
                            allowNewEmailAccounts = it.commonProviderConfiguration.allowNewAccounts,
                            requireDisplayName = it.commonProviderConfiguration.requireName,
                            actionCodeSetting = FIRActionCodeSettings()
                        )
                    }

                    FauthProviders.Facebook -> FUIFacebookAuth(fuiAuth)
                    FauthProviders.Google -> FUIGoogleAuth(fuiAuth)
                }
            }
            uiComponent = fuiAuth
        }
    }

    override suspend fun logout() {
        authUi?.signOutWithError(null)
    }

    override suspend fun getAuthToken(refresh: Boolean): String {
        return getIdToken(refresh)?.token().orEmpty()
    }

    override suspend fun getExpirationTimestamp(refresh: Boolean): Long {
        return getIdToken(refresh)?.expirationDate()?.let { expirationDate ->
            (expirationDate.timeIntervalSince1970 * 1000).toLong()
        } ?: 0
    }

    private suspend fun getIdToken(refresh: Boolean): FIRAuthTokenResult? {
        val token = suspendCoroutine { continuation ->
            FUIAuth.defaultAuthUI()?.auth()?.currentUser()
                ?.getIDTokenResultForcingRefresh(refresh) { authResult, error ->
                    error?.let {
                        continuation.resume(null)
                    } ?: run {
                        continuation.resume(authResult)
                    }
                } ?: continuation.resume(null)
        }
        return token
    }
}