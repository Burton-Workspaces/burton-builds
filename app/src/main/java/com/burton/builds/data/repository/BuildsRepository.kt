package com.burton.builds.data.repository

import com.burton.builds.BuildConfig
import com.burton.builds.data.github.GitHubApi
import com.burton.builds.data.github.GitHubApiException
import com.burton.builds.data.github.GitHubAuth
import com.burton.builds.data.github.GitHubOAuth
import com.burton.builds.data.github.GitHubTracker
import com.burton.builds.data.github.TokenHolder
import com.burton.builds.data.parse.TinyJson.int
import com.burton.builds.data.parse.TinyJson.str
import com.burton.builds.domain.BuildsSnapshot
import com.burton.builds.domain.RunDetail
import com.burton.builds.domain.Workflow
import com.burton.builds.domain.WorkflowRun
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BuildsRepository @Inject constructor(
    private val tracker: GitHubTracker,
    private val api: GitHubApi,
    private val prefs: LocalPrefs,
    private val tokenHolder: TokenHolder,
    private val installedApps: InstalledApps,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _state = MutableStateFlow(BuildsSnapshot())
    val state = _state.asStateFlow()
    private val lock = Mutex()
    private var started = false
    private var devicePoll: Job? = null

    fun start() {
        if (started) return
        started = true
        scope.launch {
            val token = prefs.auth.first().accessToken
            if (token.isNotBlank()) {
                signIn(token, refreshCatalog = true)
            } else {
                _state.update { it.copy(tokenPresent = false, scanning = false) }
            }
        }
    }

    suspend fun signIn(token: String, refreshCatalog: Boolean = true) {
        val trimmed = token.trim()
        if (trimmed.isBlank()) error("empty_token")
        tokenHolder.token = trimmed
        _state.update { it.copy(scanning = true, error = null, tokenPresent = true, deviceUserCode = "", deviceVerificationUri = "") }
        try {
            val account = tracker.currentUser()
            prefs.setAuth(GitHubAuth(trimmed))
            _state.update { it.copy(account = account, tokenPresent = true) }
            if (refreshCatalog) refreshCatalogAndInbox()
        } catch (error: Throwable) {
            tokenHolder.token = ""
            _state.update {
                it.copy(
                    tokenPresent = false,
                    scanning = false,
                    account = null,
                    error = publicError(error),
                )
            }
            throw error
        }
    }

    fun beginOAuth(): String {
        val clientId = BuildConfig.GITHUB_CLIENT_ID
        if (clientId.isBlank()) error("missing_client_id")
        val pkce = GitHubOAuth.pkce()
        scope.launch { prefs.setPendingOauth(pkce.state, pkce.verifier) }
        return GitHubOAuth.authorizeUrl(clientId, pkce.state, pkce.challenge, BuildConfig.GITHUB_REDIRECT_URI)
    }

    suspend fun completeOAuth(code: String, state: String) {
        val clientId = BuildConfig.GITHUB_CLIENT_ID
        if (clientId.isBlank()) error("missing_client_id")
        val pending = prefs.pendingOauth.first()
        if (pending == null || pending.state.isBlank() || pending.state != state) {
            error("oauth_state")
        }
        val body = api.formPost(
            GitHubOAuth.TOKEN_URL,
            mapOf(
                "client_id" to clientId,
                "code" to code,
                "redirect_uri" to BuildConfig.GITHUB_REDIRECT_URI,
                "code_verifier" to pending.verifier,
            ),
        )
        val token = body.str("access_token")
        if (token.isBlank()) {
            throw GitHubApiException("oauth", body.str("error").ifBlank { "no_token" }, body.str("error_description"))
        }
        prefs.clearPendingOauth()
        signIn(token)
    }

    suspend fun beginDeviceLogin(): GitHubOAuth.DeviceStart {
        val clientId = BuildConfig.GITHUB_CLIENT_ID
        if (clientId.isBlank()) error("missing_client_id")
        val body = api.formPost(
            GitHubOAuth.DEVICE_CODE_URL,
            mapOf("client_id" to clientId, "scope" to GitHubOAuth.SCOPE),
        )
        val start = GitHubOAuth.DeviceStart(
            deviceCode = body.str("device_code"),
            userCode = body.str("user_code"),
            verificationUri = body.str("verification_uri").ifBlank { "https://github.com/login/device" },
            intervalSeconds = body.int("interval", 5).coerceAtLeast(5),
            expiresInSeconds = body.int("expires_in", 900),
        )
        if (start.deviceCode.isBlank() || start.userCode.isBlank()) {
            throw GitHubApiException("device", body.str("error").ifBlank { "device_failed" }, body.str("error_description"))
        }
        _state.update {
            it.copy(
                deviceUserCode = start.userCode,
                deviceVerificationUri = start.verificationUri,
                error = null,
            )
        }
        devicePoll?.cancel()
        devicePoll = scope.launch { pollDevice(start) }
        return start
    }

    private suspend fun pollDevice(start: GitHubOAuth.DeviceStart) {
        val clientId = BuildConfig.GITHUB_CLIENT_ID
        var wait = start.intervalSeconds * 1000L
        val deadline = System.currentTimeMillis() + start.expiresInSeconds * 1000L
        while (System.currentTimeMillis() < deadline) {
            delay(wait)
            val result = runCatching {
                api.formPost(
                    GitHubOAuth.TOKEN_URL,
                    mapOf(
                        "client_id" to clientId,
                        "device_code" to start.deviceCode,
                        "grant_type" to "urn:ietf:params:oauth:grant-type:device_code",
                    ),
                )
            }
            val body = result.getOrNull()
            val errorCode = (result.exceptionOrNull() as? GitHubApiException)?.code
                ?: body?.str("error").orEmpty()
            val token = body?.str("access_token").orEmpty()
            if (token.isNotBlank()) {
                signIn(token)
                return
            }
            when (errorCode) {
                "authorization_pending", "" -> Unit
                "slow_down" -> wait += 5000
                "expired_token", "access_denied" -> {
                    _state.update {
                        it.copy(
                            deviceUserCode = "",
                            deviceVerificationUri = "",
                            error = if (errorCode == "access_denied") "GitHub login was cancelled." else "Device login expired. Try again.",
                        )
                    }
                    return
                }
                else -> {
                    val message = result.exceptionOrNull()?.let(::publicError)
                        ?: body?.str("error_description")?.ifBlank { body.str("error") }
                    _state.update { it.copy(error = message, deviceUserCode = "", deviceVerificationUri = "") }
                    return
                }
            }
        }
        _state.update { it.copy(deviceUserCode = "", deviceVerificationUri = "", error = "Device login expired. Try again.") }
    }

    fun failOauth(message: String) {
        _state.update { it.copy(error = message, scanning = false) }
        scope.launch { prefs.clearPendingOauth() }
    }

    suspend fun signOut() {
        devicePoll?.cancel()
        tokenHolder.token = ""
        prefs.clearToken()
        _state.value = BuildsSnapshot()
    }

    suspend fun refreshCatalogAndInbox() = lock.withLock {
        if (tokenHolder.token.isBlank()) return
        _state.update { it.copy(scanning = true, error = null) }
        try {
            val catalog = tracker.discoverAndroidApps()
            val installed = installedApps.applicationIds()
            var subscribed = prefs.subscribedRepos.first()
            if (subscribed.isEmpty()) {
                subscribed = catalog.map { it.repo }.toSet()
                prefs.setSubscribed(subscribed)
            }
            val marked = catalog.map { app ->
                app.copy(
                    installed = app.applicationId.isNotBlank() && app.applicationId in installed,
                    subscribed = app.repo in subscribed,
                )
            }
            _state.update { it.copy(catalog = marked) }
            refreshInboxLocked(marked.filter { it.subscribed }.map { it.repo })
        } catch (error: Throwable) {
            _state.update { it.copy(scanning = false, error = publicError(error)) }
        }
    }

    suspend fun setSubscribed(repo: String, on: Boolean) {
        val current = prefs.subscribedRepos.first().toMutableSet()
        if (on) current += repo else current -= repo
        prefs.setSubscribed(current)
        _state.update { snap ->
            snap.copy(catalog = snap.catalog.map { if (it.repo == repo) it.copy(subscribed = on) else it })
        }
        refreshInbox()
    }

    suspend fun refreshInbox() = lock.withLock {
        refreshInboxLocked(_state.value.catalog.filter { it.subscribed }.map { it.repo })
    }

    private suspend fun refreshInboxLocked(repos: List<String>) {
        if (tokenHolder.token.isBlank()) {
            _state.update { it.copy(scanning = false) }
            return
        }
        _state.update { it.copy(scanning = true) }
        try {
            val runs = if (repos.isEmpty()) {
                emptyList()
            } else {
                coroutineScope {
                    repos.map { repo ->
                        async {
                            runCatching { tracker.listRuns(repo) }.getOrDefault(emptyList())
                        }
                    }.awaitAll().flatten()
                }
            }
            _state.update {
                it.copy(
                    inbox = sortRuns(runs),
                    scanning = false,
                    error = null,
                )
            }
        } catch (error: Throwable) {
            _state.update { it.copy(scanning = false, error = publicError(error)) }
        }
    }

    suspend fun listRuns(repo: String): List<WorkflowRun> = sortRuns(tracker.listRuns(repo, perPage = 40))

    suspend fun getRun(repo: String, runId: Long): RunDetail = tracker.getRun(repo, runId)

    suspend fun listWorkflows(repo: String): List<Workflow> = tracker.listWorkflows(repo)

    suspend fun rerun(repo: String, runId: Long, failedOnly: Boolean = false) {
        tracker.rerun(repo, runId, failedOnly)
        refreshInbox()
    }

    suspend fun cancel(repo: String, runId: Long) {
        tracker.cancel(repo, runId)
        refreshInbox()
    }

    suspend fun dispatch(repo: String, workflowId: Long, ref: String) {
        tracker.dispatch(repo, workflowId, ref)
        delay(1500)
        refreshInbox()
    }

    fun defaultBranch(repo: String): String =
        _state.value.catalog.firstOrNull { it.repo == repo }?.defaultBranch?.ifBlank { "master" } ?: "master"

    private fun sortRuns(runs: List<WorkflowRun>): List<WorkflowRun> =
        runs.sortedWith(
            compareBy<WorkflowRun> { run ->
                when {
                    run.inProgress -> 0
                    run.failed -> 1
                    else -> 2
                }
            }.thenByDescending { it.updatedAt.ifBlank { it.createdAt } },
        )

    fun publicError(error: Throwable): String {
        val code = (error as? GitHubApiException)?.code.orEmpty()
        val message = error.message.orEmpty()
        return when {
            code.contains("401") || code.contains("unauthorized", true) || message.contains("Bad credentials", true) ->
                "GitHub rejected that token."
            code.contains("403") || message.contains("Resource not accessible", true) ->
                "GitHub refused that action. Reconnect with workflow scope, or paste a token that can rerun Actions."
            code.contains("422") ->
                "That workflow cannot be dispatched from here. It needs a workflow_dispatch trigger."
            code == "missing_client_id" ->
                "This build has no GitHub Client ID. Fill github/client-id.txt after creating the OAuth app."
            code == "oauth_state" -> "GitHub login did not match this phone. Try Connect with GitHub again."
            message.isNotBlank() -> message
            else -> "Could not reach GitHub."
        }
    }
}
