package com.labfreezer.ui.screens.settings

import androidx.lifecycle.ViewModel
import com.labfreezer.data.repository.SamplePositionRepository
import com.labfreezer.data.repository.StorageBoxRepository
import com.labfreezer.data.repository.StorageDeviceRepository
import com.labfreezer.data.repository.StorageLayerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class StartPagePickerViewModel @Inject constructor(
    val deviceRepo: StorageDeviceRepository,
    val layerRepo: StorageLayerRepository,
    val boxRepo: StorageBoxRepository,
    val sampleRepo: SamplePositionRepository
) : ViewModel()
