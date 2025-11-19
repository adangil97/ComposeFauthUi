package mx.empos.composefauthui.data

import mx.empos.composefauthui.domain.FauthConfiguration
import mx.empos.composefauthui.domain.FauthResult

interface AuthRepository {
    var uiComponent: Any?

    fun userAlreadyLogin(): Boolean

    fun configure(
        fauthConfiguration: FauthConfiguration,
        callback: () -> Any = { Any() }
    )

    suspend fun logout()

    suspend fun getAuthToken(
        refresh: Boolean,
        runException: (Exception) -> Unit
    ): FauthResult?
}