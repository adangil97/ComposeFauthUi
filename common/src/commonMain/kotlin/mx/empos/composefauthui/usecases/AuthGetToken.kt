package mx.empos.composefauthui.usecases

import mx.empos.composefauthui.data.AuthRepository

class AuthGetToken(private val authRepository: AuthRepository) {

    suspend operator fun invoke(
        refresh: Boolean,
        runException: (Exception) -> Unit = {}
    ) = authRepository.getAuthToken(refresh, runException)
}