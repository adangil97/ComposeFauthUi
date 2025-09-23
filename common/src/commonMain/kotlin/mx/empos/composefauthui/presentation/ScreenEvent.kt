package mx.empos.composefauthui.presentation

sealed class ScreenEvent(open val screenName: String) {

    data class Created(override val screenName: String) : ScreenEvent(screenName)

    data class Started(override val screenName: String) : ScreenEvent(screenName)

    data class Resumed(override val screenName: String) : ScreenEvent(screenName)

    data class Paused(override val screenName: String) : ScreenEvent(screenName)

    data class Stopped(override val screenName: String) : ScreenEvent(screenName)

    data class Destroyed(override val screenName: String) : ScreenEvent(screenName)
}