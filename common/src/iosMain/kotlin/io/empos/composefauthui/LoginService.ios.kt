package io.empos.composefauthui

import cocoapods.FirebaseAuthUI.FIRUser
import cocoapods.FirebaseAuthUI.FUIAuth
import cocoapods.FirebaseAuthUI.FUIAuthDelegateProtocol
import cocoapods.FirebaseAuthUI.FUIAuthErrorCodeUserCancelledSignIn
import cocoapods.FirebaseEmailAuthUI.FIRActionCodeSettings
import cocoapods.FirebaseEmailAuthUI.FUIEmailAuth
import cocoapods.FirebaseEmailAuthUI.FUIPasswordSignInViewController
import cocoapods.FirebaseFacebookAuthUI.FUIFacebookAuth
import cocoapods.FirebaseGoogleAuthUI.FUIGoogleAuth
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import platform.Foundation.NSError
import platform.Foundation.NSURL
import platform.Foundation.NSUnderlyingErrorKey
import platform.UIKit.UIView
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
class LoginService(private val fauthConfiguration: FauthConfiguration) : NSObject(),
    FUIAuthDelegateProtocol {
    private var authUi: FUIAuth? = null
    private val signInState: MutableSharedFlow<FauthSignInResult> = MutableSharedFlow()
    val state: SharedFlow<FauthSignInResult> = signInState.asSharedFlow()

    suspend fun initialize() {
        authUi = FUIAuth.defaultAuthUI()
        if (authUi?.auth?.currentUser() != null) {
            signInState.emit(FauthSignInResult.Success)
        } else {
            val iosConfiguration = fauthConfiguration.iosConfiguration
            authUi?.let { fuiAuth ->
                fuiAuth.delegate = this
                fuiAuth.shouldHideCancelButton = true
                fuiAuth.TOSURL = NSURL(string = iosConfiguration.tosUrl)
                fuiAuth.privacyPolicyURL = NSURL(string = iosConfiguration.privacyPolicyUrl)
                fuiAuth.providers = fauthConfiguration.providers.map {
                    when(it) {
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
            }
        }
    }

    override fun authUI(authUI: FUIAuth, didSignInWithUser: FIRUser?, error: NSError?) {
        if (error == null) {
            signInState.tryEmit(FauthSignInResult.Success)
        } else {
            if (error.code.toInt() == FUIAuthErrorCodeUserCancelledSignIn.toInt()) {
                signInState.tryEmit(FauthSignInResult.Error(Exception("The user has cancelled the operation")))
                return
            }
            val customError = error.userInfo[NSUnderlyingErrorKey]
            if (customError != null) {
                signInState.tryEmit(FauthSignInResult.Error(Exception("An unknown error occurred")))
                return
            }
            signInState.tryEmit(FauthSignInResult.Error(Exception("An unknown error occurred")))
        }
    }

    fun getAuthPreview(): UIView {
        return requireNotNull(authUi?.authViewController()?.view())
    }
}