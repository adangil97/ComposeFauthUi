package mx.empos.composefauthui.domain

sealed class FauthSignInResult(open val code: Int) {

    data class Success(override val code: Int = 0) : FauthSignInResult(code)

    data class Destroy(override val code: Int = 0) : FauthSignInResult(code)

    data class Error(
        val exception: Throwable,
        val errorCode: Int? = null,
        val errorMessage: String? = null,
        override val code: Int = 0
    ) : FauthSignInResult(code)
}