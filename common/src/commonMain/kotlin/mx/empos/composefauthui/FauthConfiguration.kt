package mx.empos.composefauthui

import mx.empos.composefauthui.PlatformConfiguration.AndroidConfiguration
import mx.empos.composefauthui.PlatformConfiguration.IosConfiguration

data class FauthConfiguration(
    val androidConfiguration: AndroidConfiguration = AndroidConfiguration(),
    val iosConfiguration: IosConfiguration = IosConfiguration(),
    val providers: List<FauthProviders> = listOf()
)

sealed class PlatformConfiguration(
    open val tosUrl: String,
    open val privacyPolicyUrl: String
) {

    data class AndroidConfiguration(
        override val tosUrl: String = "",
        override val privacyPolicyUrl: String = "",
        val isSmartLockEnabled: Boolean = false,
        val logo: Int = -1,
        val theme: Int = -1
    ) : PlatformConfiguration(tosUrl, privacyPolicyUrl)

    data class IosConfiguration(
        override val tosUrl: String = "",
        override val privacyPolicyUrl: String = "",
        val shouldHideCancelButton: Boolean = false,
        val signInMethod: String = ""
    ) : PlatformConfiguration(tosUrl, privacyPolicyUrl)
}