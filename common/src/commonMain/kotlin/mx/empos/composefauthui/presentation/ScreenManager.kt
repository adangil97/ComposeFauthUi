package mx.empos.composefauthui.presentation

import kotlinx.coroutines.flow.StateFlow

interface ScreenManager {

    val screenState: StateFlow<ScreenEvent>
}