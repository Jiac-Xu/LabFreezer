package com.labfreezer.ui.screens.settings

import androidx.lifecycle.ViewModel
import com.labfreezer.ui.screens.boxgrid.InputMode
import com.labfreezer.ui.screens.boxgrid.InputModePreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

data class PersonalizationState(
    val inputMode: String = "CAMERA",
    val tempModeAllowed: Boolean = false,
    val autoSaveEnabled: Boolean = false,
    val zoomSliderEnabled: Boolean = true,
    val searchHistoryEnabled: Boolean = true,
    val autoKeyboardFromBottomBar: Boolean = false
)

@HiltViewModel
class PersonalizationViewModel @Inject constructor(
    private val preferences: PersonalizationPreferences,
    private val inputModePreferences: InputModePreferences
) : ViewModel() {

    private val _state = MutableStateFlow(
        PersonalizationState(
            inputMode = inputModePreferences.getInputMode().name,
            tempModeAllowed = preferences.isTempModeAllowed(),
            autoSaveEnabled = preferences.isAutoSaveEnabled(),
            zoomSliderEnabled = preferences.isZoomSliderEnabled(),
            searchHistoryEnabled = preferences.isSearchHistoryEnabled(),
            autoKeyboardFromBottomBar = preferences.isAutoKeyboardFromBottomBar()
        )
    )
    val state: StateFlow<PersonalizationState> = _state

    fun setInputMode(mode: String) {
        runCatching {
            inputModePreferences.setInputMode(InputMode.valueOf(mode))
        }
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

    fun setAutoKeyboardFromBottomBar(enabled: Boolean) {
        preferences.setAutoKeyboardFromBottomBar(enabled)
        _state.value = _state.value.copy(autoKeyboardFromBottomBar = enabled)
    }
}