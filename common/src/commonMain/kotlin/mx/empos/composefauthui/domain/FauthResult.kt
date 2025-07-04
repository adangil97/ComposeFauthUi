package mx.empos.composefauthui.domain

data class FauthResult(
    val token: String,
    val timestamp: Long
)