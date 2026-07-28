package com.labfreezer.ui.screens.settings

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

data class PersonalizationState(
    val inputMode: String = "CAMERA",
    val tempModeAllowed: Boolean = false,
    val autoSaveEnabled: Boolean = false,
    val zoomSliderEnabled: Boolean = true,
    val searchHistoryEnabled: Boolean = true
)

@HiltViewModel
class PersonalizationViewModel @Inject constructor(
    private val preferences: PersonalizationPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(
        PersonalizationState(
            inputMode = preferences.getInputMode(),
            tempModeAllowed = preferences.isTempModeAllowed(),
            autoSaveEnabled = preferences.isAutoSaveEnabled(),
            zoomSliderEnabled = preferences.isZoomSliderEnabled(),
            searchHistoryEnabled = preferences.isSearchHistoryEnabled()
        )
    )
    val state: StateFlow<PersonalizationState> = _state

    fun setInputMode(mode: String) {
        preferences.setInputMode(mode)
        _state.value = _state.value.copy(inputMode = mode)
    }

    fun setTempModeAllowed(allowed: Boolean) {
        preferences.setTempModeAllowed(allowed)
        _state.value = _state.value.copy(tempModeAllowed = allowed)
    }

    fun setAutoSaveEnabled(enabled: Boolean) {
        preferences.setAutoSaveEnabled(enabled)
        _state.value = _state.value.copy(autoSaveEnabled = enabled)
    }

    fun setZoomSliderEnabled(enabled: Boolean) {
        preferences.setZoomSliderEnabled(enabled)
        _state.value = _state.value.copy(zoomSliderEnabled = enabled)
    }

    fun setSearchHistoryEnabled(enabled: Boolean) {
        preferences.setSearchHistoryEnabled(enabled)
        _state.value = _state.value.copy(searchHistoryEnabled = enabled)
    }
}