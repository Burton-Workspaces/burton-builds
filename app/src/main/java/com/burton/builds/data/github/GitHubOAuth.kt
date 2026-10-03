package com.burton.builds.data.github

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

object GitHubOAuth {
    const val REDIRECT_SCHEME = "burtonbuilds"
    const val REDIRECT_HOST = "oauth"
    const val REDIRECT_URI = "$REDIRECT_SCHEME://$REDIRECT_HOST"

    const val CALLBACK_URL = "https://burton-workspaces.github.io/burton-builds/oauth/"
    const val CALLBACK_HOST = "burton-workspaces.github.io"
    const val CALLBACK_PATH_PREFIX = "/burton-builds/oauth"

    const val AUTHORIZE_URL = "https://github.com/login/oauth/authorize"
    const val TOKEN_URL = "https://github.com/login/oauth/access_token"
    const val DEVICE_CODE_URL = "https://github.com/login/device/code"
    const val SCOPE = "public_repo workflow read:user read:org"

    data class Pkce(
        val state: String,
        val verifier: String,
        val challenge: String,
    )

    data class DeviceStart(
        val deviceCode: String,
        val userCode: String,
        val verificationUri: String,
        val intervalSeconds: Int,
        val expiresInSeconds: Int,
    )

    fun authorizeUrl(clientId: String, state: String, challenge: String, redirectUri: String = CALLBACK_URL): String {
        val query = listOf(
            "client_id" to clientId,
            "redirect_uri" to redirectUri,
            "scope" to SCOPE,
            "state" to state,
            "code_challenge" to challenge,
            "code_challenge_method" to "S256",
        ).joinToString("&") { (key, value) -> "${encode(key)}=${encode(value)}" }
        return "$AUTHORIZE_URL?$query"
    }

    fun isCallback(scheme: String?, host: String?, path: String?): Boolean {
        if (scheme == REDIRECT_SCHEME && host == REDIRECT_HOST) return true
        if (scheme == "https" && host == CALLBACK_HOST) {
            val p = path.orEmpty()
            return p == CALLBACK_PATH_PREFIX || p.startsWith("$CALLBACK_PATH_PREFIX/")
        }
        return false
    }

    fun pkce(random: SecureRandom = SecureRandom()): Pkce {
        val state = randomUrl(random, 24)
        val verifier = randomUrl(random, 32)
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.UTF_8))
        val challenge = Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
        return Pkce(state = state, verifier = verifier, challenge = challenge)
    }

    fun encode(value: String): String =
        java.net.URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")

    private fun randomUrl(random: SecureRandom, bytes: Int): String {
        val buffer = ByteArray(bytes)
        random.nextBytes(buffer)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer)
    }
}
