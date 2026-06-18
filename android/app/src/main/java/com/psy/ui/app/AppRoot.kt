package com.psy.ui.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle.Event.ON_START
import androidx.lifecycle.Lifecycle.Event.ON_STOP
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.psy.ui.lock.LockScreen
import com.psy.ui.navigation.PsyNavHost
import com.psy.ui.theme.PsyTheme

@Composable
fun AppRoot() {
    val vm: AppViewModel = hiltViewModel()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val locked by vm.isLocked.collectAsStateWithLifecycle()

    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, event ->
            when (event) {
                ON_START -> vm.onStart(System.currentTimeMillis())
                ON_STOP  -> vm.onStop(System.currentTimeMillis())
                else     -> Unit
            }
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    PsyTheme(themeMode = settings.themeMode, accent = settings.accent) {
        if (locked) {
            LockScreen(
                onUnlock = vm::unlock,
                biometricEnabled = settings.biometricEnabled,
                verifyPin = { vm.verifyPin(it) },
            )
        } else {
            PsyNavHost()
        }
    }
}
