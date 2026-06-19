package com.psy.data.auth

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthTokenStore @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val AUTH_TOKEN = stringPreferencesKey("auth_token")
        private val USER_EMAIL = stringPreferencesKey("user_email")
        private val LAST_SYNC_AT = longPreferencesKey("last_sync_at")
    }

    val tokenFlow: Flow<String?> = dataStore.data.map { it[AUTH_TOKEN] }
    val emailFlow: Flow<String?> = dataStore.data.map { it[USER_EMAIL] }
    val lastSyncAtFlow: Flow<Long?> = dataStore.data.map { it[LAST_SYNC_AT] }

    @Volatile
    var cachedToken: String? = null
        private set

    init {
        CoroutineScope(Dispatchers.IO).launch {
            tokenFlow.collect { token ->
                cachedToken = token
            }
        }
    }

    fun currentToken(): String? = cachedToken

    suspend fun setAuth(token: String, email: String) {
        dataStore.edit { prefs ->
            prefs[AUTH_TOKEN] = token
            prefs[USER_EMAIL] = email
        }
    }

    suspend fun clearAuth() {
        dataStore.edit { prefs ->
            prefs.remove(AUTH_TOKEN)
            prefs.remove(USER_EMAIL)
        }
    }

    suspend fun setLastSyncAt(time: Long) {
        dataStore.edit { prefs ->
            prefs[LAST_SYNC_AT] = time
        }
    }
}
