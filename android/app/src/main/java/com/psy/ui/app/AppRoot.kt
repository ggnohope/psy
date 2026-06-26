package com.psy.ui.app

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.lifecycle.Lifecycle.Event.ON_START
import androidx.lifecycle.Lifecycle.Event.ON_STOP
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.psy.ui.auth.LoginScreen
import com.psy.ui.lock.LockScreen
import com.psy.ui.navigation.PsyNavHost
import com.psy.ui.theme.PsyTheme

@Composable
fun AppRoot() {
    val vm: AppViewModel = hiltViewModel()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val locked by vm.isLocked.collectAsStateWithLifecycle()
    val signedIn by vm.isSignedIn.collectAsStateWithLifecycle()
    val preparingData by vm.isPreparingData.collectAsStateWithLifecycle()

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
        val focusManager = LocalFocusManager.current
        // Tap anywhere outside a focused text field to dismiss the keyboard (app-wide).
        // detectTapGestures only fires for taps the children didn't consume, so buttons,
        // list rows, scrolling, etc. are unaffected.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                }
        ) {
            when {
                signedIn == null -> LoadingGate()            // unknown yet → avoid LoginScreen flash
                signedIn == false -> LoginScreen(viewModel = vm)
                preparingData -> LoadingGate()
                locked -> LockScreen(
                    onUnlock = vm::unlock,
                    biometricEnabled = settings.biometricEnabled,
                    verifyPin = { vm.verifyPin(it) },
                )
                else -> PsyNavHost(onLogout = { vm.logout() })
            }
        }
    }
}

@Composable
private fun LoadingGate() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
