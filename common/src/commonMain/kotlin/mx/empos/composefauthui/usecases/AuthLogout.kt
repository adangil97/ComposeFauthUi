package mx.empos.composefauthui.usecases

import mx.empos.composefauthui.data.AuthRepository

class AuthLogout(private val authRepository: AuthRepository) {

    suspend operator fun invoke() = authRepository.logout()
}