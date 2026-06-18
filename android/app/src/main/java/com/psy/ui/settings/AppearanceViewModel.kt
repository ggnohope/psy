package com.psy.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.psy.data.settings.AccentPalette
import com.psy.data.settings.SettingsRepository
import com.psy.data.settings.SettingsState
import com.psy.data.settings.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppearanceViewModel @Inject constructor(
    private val settingsRepo: SettingsRepository,
) : ViewModel() {

    val settings = settingsRepo.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsState(),
    )

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch {
        settingsRepo.setThemeMode(mode)
    }

    fun setAccent(accent: AccentPalette) = viewModelScope.launch {
        settingsRepo.setAccent(accent)
    }
}
