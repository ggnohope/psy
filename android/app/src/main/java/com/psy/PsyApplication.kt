package com.psy

import android.app.Application
import com.psy.data.seed.DefaultDataSeeder
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class PsyApplication : Application() {

    @Inject lateinit var seeder: DefaultDataSeeder

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch {
            seeder.seedIfEmpty(System.currentTimeMillis())
        }
    }
}
