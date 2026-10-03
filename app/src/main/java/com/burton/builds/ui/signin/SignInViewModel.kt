package com.burton.builds.ui.signin

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.burton.builds.BuildConfig
import com.burton.builds.data.github.GitHubApiException
import com.burton.builds.data.repository.BuildsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SignInUi(
    val token: String = "",
    val busy: Boolean = false,
    val error: String? = null,
    val pasteOpen: Boolean = false,
    val oauthConfigured: Boolean = BuildConfig.GITHUB_CLIENT_ID.isNotBlank(),
    val deviceUserCode: String = "",
    val deviceVerificationUri: String = "",
)

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val repository: BuildsRepository,
) : ViewModel() {
    private val _ui = MutableStateFlow(SignInUi())
    val ui = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            repository.state.collect { snap ->
                if (snap.tokenPresent) return@collect
                _ui.update {
                    it.copy(
                        busy = false,
                        error = snap.error ?: it.error,
                        deviceUserCode = snap.deviceUserCode,
                        deviceVerificationUri = snap.deviceVerificationUri,
                    )
                }
            }
        }
    }

    fun onTokenChange(value: String) {
        _ui.update { it.copy(token = value, error = null) }
    }

    fun togglePaste() {
        _ui.update { it.copy(pasteOpen = !it.pasteOpen, error = null) }
    }

    fun connectWithGitHub(openUrl: (String) -> Unit) {
        if (BuildConfig.GITHUB_CLIENT_ID.isBlank()) {
            _ui.update {
                it.copy(
                    error = "This build has no GitHub Client ID. Fill github/client-id.txt after creating the OAuth app, or paste a token.",
                    pasteOpen = true,
                )
            }
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(busy = true, error = null) }
            runCatching { repository.beginDeviceLogin() }
                .onSuccess { start ->
                    _ui.update {
                        it.copy(
                            busy = false,
                            deviceUserCode = start.userCode,
                            deviceVerificationUri = start.verificationUri,
                        )
                    }
                    openUrl(start.verificationUri)
                }
                .onFailure { error ->
                    val url = runCatching { repository.beginOAuth() }.getOrNull()
                    if (url != null) {
                        _ui.update { it.copy(busy = false) }
                        openUrl(url)
                    } else {
                        _ui.update { it.copy(busy = false, error = connectError(error)) }
                    }
                }
        }
    }

    fun connect() {
        val token = _ui.value.token.trim()
        if (token.isBlank()) {
            _ui.update { it.copy(error = "Paste a GitHub token to connect.", pasteOpen = true) }
            return
        }
        viewModelScope.launch {
            _ui.update { it.copy(busy = true, error = null) }
            runCatching { repository.signIn(token) }
                .onFailure { error ->
                    _ui.update { it.copy(busy = false, error = connectError(error)) }
                }
        }
    }

    private fun connectError(error: Throwable): String {
        val code = (error as? GitHubApiException)?.code ?: error.message.orEmpty()
        return when {
            code.contains("unauthorized", ignoreCase = true) ||
                code.contains("invalid", ignoreCase = true) ||
                code.contains("401") -> "That token was rejected."
            code == "missing_client_id" ->
                "This build has no GitHub Client ID. Fill github/client-id.txt after creating the OAuth app."
            else -> error.message ?: "Could not connect."
        }
    }
}

fun openAuthorizeUrl(context: Context, url: String) {
    val uri = Uri.parse(url)
    try {
        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setColorScheme(CustomTabsIntent.COLOR_SCHEME_DARK)
            .build()
            .launchUrl(context, uri)
    } catch (_: Exception) {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }
}
