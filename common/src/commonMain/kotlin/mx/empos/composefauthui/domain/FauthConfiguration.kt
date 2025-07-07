package mx.empos.composefauthui.domain

data class FauthConfiguration(
    val providers: List<FauthProviders> = listOf(),
    val commonConfiguration: CommonConfiguration = CommonConfiguration(),
    val androidConfiguration: AndroidConfiguration = AndroidConfiguration(),
    val iosConfiguration: IosConfiguration = IosConfiguration(),
)

data class CommonConfiguration(
    val tosUrl: String = "",
    val privacyPolicyUrl: String = "",
)

data class AndroidConfiguration(
    val alwaysShowSignInMethodScreen: Boolean = false,
    val logo: Int = -1,
    val theme: Int = -1
)

data class IosConfiguration(val shouldHideCancelButton: Boolean = false)