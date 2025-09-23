package mx.empos.composefauthui.presentation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import cocoapods.FirebaseAuthUI.FIRUser
import cocoapods.FirebaseAuthUI.FUIAuth
import cocoapods.FirebaseAuthUI.FUIAuthDelegateProtocol
import cocoapods.FirebaseAuthUI.FUIAuthErrorCodeUserCancelledSignIn
import kotlinx.cinterop.ExperimentalForeignApi
import mx.empos.composefauthui.data.AuthRepository
import mx.empos.composefauthui.domain.FauthConfiguration
import mx.empos.composefauthui.domain.FauthSignInResult
import mx.empos.composefauthui.framework.IosAuthRepository
import platform.Foundation.NSError
import platform.Foundation.NSUnderlyingErrorKey
import platform.UIKit.UIView
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun FauthUiContent(
    fauthConfiguration: FauthConfiguration,
    fauthResult: (FauthSignInResult) -> Unit
) {
    val authDelegate = remember {
        object : NSObject(), FUIAuthDelegateProtocol {

            override fun authUI(authUI: FUIAuth, didSignInWithUser: FIRUser?, error: NSError?) {
                if (error == null) {
                    fauthResult(FauthSignInResult.Success)
                } else {
                    if (error.code.toInt() == FUIAuthErrorCodeUserCancelledSignIn.toInt()) {
                        fauthResult(FauthSignInResult.Error(Exception("The user has cancelled the operation")))
                        return
                    }
                    val customError = error.userInfo[NSUnderlyingErrorKey]
                    if (customError != null) {
                        fauthResult(FauthSignInResult.Error(Exception("An unknown error occurred")))
                        return
                    }
                    fauthResult(FauthSignInResult.Error(Exception("An unknown error occurred")))
                }
            }
        }
    }
    val authRepository: AuthRepository = IosAuthRepository()
    if (authRepository.userAlreadyLogin()) {
        fauthResult(FauthSignInResult.Success)
    } else {
        authRepository.configure(fauthConfiguration) {
            authDelegate
        }
        (authRepository.uiComponent as? FUIAuth)?.let { fuiAuth ->
            val loginView: UIView = remember { fuiAuth.authViewController().view }
            UIKitView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    loginView
                }
            )
        }
    }
}