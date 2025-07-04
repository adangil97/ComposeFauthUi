package mx.empos.composefauthui.usecases

import mx.empos.composefauthui.data.AuthRepository

class AuthGetTimestamp(private val authRepository: AuthRepository) {

    suspend operator fun invoke(refresh: Boolean) = authRepository.getExpirationTimestamp(refresh)
}