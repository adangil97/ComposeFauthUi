package mx.empos.composefauthui.domain

sealed class FauthProviders {

    data object Google : FauthProviders()

    data object Facebook : FauthProviders()

    data class Email(
        val commonProviderConfiguration: CommonEmailProviderConfiguration = CommonEmailProviderConfiguration(),
        val androidProviderConfiguration: AndroidEmailProviderConfiguration = AndroidEmailProviderConfiguration,
        val iosProviderConfiguration: IosEmailProviderConfiguration = IosEmailProviderConfiguration()
    ) : FauthProviders()
}

data class CommonEmailProviderConfiguration(
    val allowNewAccounts: Boolean = false,
    val requireName: Boolean = false,
    val forceSameDevice: Boolean = false
)

data object AndroidEmailProviderConfiguration

data class IosEmailProviderConfiguration(val signInMethod: String = "")