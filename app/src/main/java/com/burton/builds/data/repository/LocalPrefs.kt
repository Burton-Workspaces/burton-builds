package com.burton.builds.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.burton.builds.data.github.GitHubAuth
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.burtonStore: DataStore<Preferences> by preferencesDataStore("burton_builds")

data class PendingOauth(
    val state: String,
    val verifier: String = "",
)

@Singleton
class LocalPrefs @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val store = context.burtonStore

    val auth: Flow<GitHubAuth> = store.data.map { prefs ->
        GitHubAuth(accessToken = prefs[TOKEN].orEmpty())
    }

    val pendingOauth: Flow<PendingOauth?> = store.data.map { prefs ->
        val state = prefs[OAUTH_STATE].orEmpty()
        if (state.isBlank()) null else PendingOauth(state = state, verifier = prefs[OAUTH_VERIFIER].orEmpty())
    }

    val subscribedRepos: Flow<Set<String>> = store.data.map { prefs ->
        prefs[SUBSCRIBED].orEmpty().split('\n').map { it.trim() }.filter { it.isNotBlank() }.toSet()
    }

    suspend fun setAuth(value: GitHubAuth) {
        store.edit { prefs ->
            val access = value.accessToken.trim()
            if (access.isBlank()) prefs.remove(TOKEN) else prefs[TOKEN] = access
            prefs.remove(OAUTH_STATE)
            prefs.remove(OAUTH_VERIFIER)
        }
    }

    suspend fun clearToken() = setAuth(GitHubAuth())

    suspend fun setPendingOauth(state: String, verifier: String) {
        store.edit { prefs ->
            prefs[OAUTH_STATE] = state
            prefs[OAUTH_VERIFIER] = verifier
        }
    }

    suspend fun clearPendingOauth() {
        store.edit { prefs ->
            prefs.remove(OAUTH_STATE)
            prefs.remove(OAUTH_VERIFIER)
        }
    }

    suspend fun setSubscribed(repos: Set<String>) {
        store.edit { prefs ->
            prefs[SUBSCRIBED] = repos.sorted().joinToString("\n")
        }
    }

    private companion object {
        val TOKEN = stringPreferencesKey("user_token")
        val OAUTH_STATE = stringPreferencesKey("oauth_state")
        val OAUTH_VERIFIER = stringPreferencesKey("oauth_verifier")
        val SUBSCRIBED = stringPreferencesKey("subscribed_repos")
    }
}
