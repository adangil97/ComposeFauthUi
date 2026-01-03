package mx.empos.composefauthui.domain

sealed class FauthProviders(open val signButtonText: String?) {

    data class Google(
        override val signButtonText: String? = null
    ) : FauthProviders(signButtonText)

    data class Facebook(
        override val signButtonText: String? = null,
        val extraPermissions: List<String> = emptyList()
    ) : FauthProviders(signButtonText)

    data class Phone(
        override val signButtonText: String? = null
    ) : FauthProviders(signButtonText)

    data class Email(
        override val signButtonText: String? = null,
        val commonProviderConfiguration: CommonEmailProviderConfiguration = CommonEmailProviderConfiguration(),
        val androidProviderConfiguration: AndroidEmailProviderConfiguration = AndroidEmailProviderConfiguration,
        val iosProviderConfiguration: IosEmailProviderConfiguration = IosEmailProviderConfiguration()
    ) : FauthProviders(signButtonText)
}

data class CommonEmailProviderConfiguration(
    val allowNewAccounts: Boolean = false,
    val requireName: Boolean = false,
    val forceSameDevice: Boolean = false
)

data object AndroidEmailProviderConfiguration

data class IosEmailProviderConfiguration(val signInMethod: String = "")