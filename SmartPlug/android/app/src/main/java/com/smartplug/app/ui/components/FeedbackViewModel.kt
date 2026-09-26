package com.smartplug.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.smartplug.app.data.local.AppPreferences
import com.smartplug.app.data.local.AppSettings
import com.smartplug.app.util.SoundManager
import com.smartplug.app.util.rememberHapticController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** Shared "did the user just tap something" feedback: optional sound + haptic, both toggleable
 * from Settings (spec: "suara tap opsional... pengguna dapat mematikannya" / "haptic feedback
 * ringan bila tersedia"). Every screen grabs the same Hilt-scoped instance via [hiltViewModel]. */
@HiltViewModel
class FeedbackViewModel @Inject constructor(
    private val soundManager: SoundManager,
    appPreferences: AppPreferences,
) : ViewModel() {
    val settings = appPreferences.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun playTap() {
        soundManager.playTap(settings.value.soundEnabled)
    }
}

class TapFeedback(val onTap: () -> Unit, val onConfirm: () -> Unit)

@Composable
fun rememberTapFeedback(): TapFeedback {
    val feedbackViewModel: FeedbackViewModel = hiltViewModel()
    val settings by feedbackViewModel.settings.collectAsStateWithLifecycle()
    val haptic = rememberHapticController()
    return remember(feedbackViewModel, settings.hapticEnabled) {
        TapFeedback(
            onTap = {
                feedbackViewModel.playTap()
                haptic.tick(settings.hapticEnabled)
            },
            onConfirm = {
                feedbackViewModel.playTap()
                haptic.confirm(settings.hapticEnabled)
            },
        )
    }
}
