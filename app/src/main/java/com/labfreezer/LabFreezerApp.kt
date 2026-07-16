package com.labfreezer

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class LabFreezerApp : Application() {

    @Inject
    lateinit var fairMemoryReceiver: FairMemoryReceiver

    override fun onCreate() {
        super.onCreate()
        fairMemoryReceiver.initialize()
    }
}
