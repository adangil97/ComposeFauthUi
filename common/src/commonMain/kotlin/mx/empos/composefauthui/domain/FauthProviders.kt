package mx.empos.composefauthui.domain

import mx.empos.composefauthui.domain.FauthEmailProviderConfiguration.AndroidEmailProviderConfiguration
import mx.empos.composefauthui.domain.FauthEmailProviderConfiguration.IosEmailProviderConfiguration

sealed class FauthProviders {

    data object Google : FauthProviders()

    data object Facebook : FauthProviders()

    data class Email(
        val androidProviderConfiguration: AndroidEmailProviderConfiguration,
        val iosProviderConfiguration: IosEmailProviderConfiguration
    ) : FauthProviders()
}

sealed class FauthEmailProviderConfiguration(
    open val allowNewAccounts: Boolean,
    open val requireName: Boolean
) {

    data class AndroidEmailProviderConfiguration(
        override val allowNewAccounts: Boolean = false,
        override val requireName: Boolean = false
    ) : FauthEmailProviderConfiguration(allowNewAccounts, requireName)

    data class IosEmailProviderConfiguration(
        override val allowNewAccounts: Boolean = false,
        override val requireName: Boolean = false,
        val signInMethod: String = "",
        val forceSameDevice: Boolean = false
    ) : FauthEmailProviderConfiguration(allowNewAccounts, requireName)
}