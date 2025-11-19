package mx.empos.composefauthui.framework

import cocoapods.FirebaseAuthUI.FIRAuthTokenResult
import cocoapods.FirebaseAuthUI.FUIAuth
import cocoapods.FirebaseAuthUI.FUIAuthDelegateProtocol
import cocoapods.FirebaseEmailAuthUI.FIRActionCodeSettings
import cocoapods.FirebaseEmailAuthUI.FUIEmailAuth
import cocoapods.FirebaseFacebookAuthUI.FUIFacebookAuth
import cocoapods.FirebaseGoogleAuthUI.FUIGoogleAuth
import cocoapods.FirebasePhoneAuthUI.FUIPhoneAuth
import kotlinx.cinterop.ExperimentalForeignApi
import mx.empos.composefauthui.data.AuthRepository
import mx.empos.composefauthui.domain.FauthConfiguration
import mx.empos.composefauthui.domain.FauthProviders
import mx.empos.composefauthui.domain.FauthResult
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

                    is FauthProviders.Facebook -> FUIFacebookAuth(
                        authUI = fuiAuth,
                        permissions = it.extraPermissions
                    )

                    FauthProviders.Google -> FUIGoogleAuth(fuiAuth)
                    FauthProviders.Phone -> FUIPhoneAuth(fuiAuth)
                }
            }
            uiComponent = fuiAuth
        }
    }

    override suspend fun logout() {
        authUi?.signOutWithError(null)
    }

    override suspend fun getAuthToken(
        refresh: Boolean,
        runException: (Exception) -> Unit
    ): FauthResult? {
        return getIdToken(refresh, runException)?.let {
            FauthResult(
                token = it.token(),
                timestamp = (it.authDate().timeIntervalSince1970 * 1000).toLong()
            )
        }
    }

    private suspend fun getIdToken(
        refresh: Boolean,
        runException: (Exception) -> Unit
    ): FIRAuthTokenResult? {
        val token = suspendCoroutine { continuation ->
            FUIAuth.defaultAuthUI()?.auth()?.currentUser()
                ?.getIDTokenResultForcingRefresh(refresh) { authResult, error ->
                    error?.let {
                        runException(Exception(it.domain))
                        continuation.resume(null)
                    } ?: run {
                        continuation.resume(authResult)
                    }
                } ?: run {
                runException(Exception("Get id token failed"))
                continuation.resume(null)
            }
        }
        return token
    }
}