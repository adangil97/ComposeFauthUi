package mx.empos.composefauthui

sealed class FauthSignInResult {

    data object Success : FauthSignInResult()

    data class Error(val exception: Throwable) : FauthSignInResult()
}