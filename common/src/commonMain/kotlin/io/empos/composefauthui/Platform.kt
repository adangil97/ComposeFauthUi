package io.empos.composefauthui

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform