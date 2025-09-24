package mx.empos.composefauthui.domain

sealed class FauthSignInResult {

    data object Success : FauthSignInResult()

    data object UserCancellation : FauthSignInResult()

    data class Error(
        val exception: Throwable,
        val errorCode: Int? = null,
        val errorMessage: String? = null,
    ) : FauthSignInResult()
}