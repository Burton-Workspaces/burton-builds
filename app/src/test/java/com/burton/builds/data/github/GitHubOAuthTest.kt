package com.burton.builds.data.github

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubOAuthTest {
    @Test
    fun callbackMatchesCustomSchemeAndPagesHop() {
        assertTrue(GitHubOAuth.isCallback("burtonbuilds", "oauth", null))
        assertTrue(GitHubOAuth.isCallback("https", "burton-workspaces.github.io", "/burton-builds/oauth/"))
        assertTrue(GitHubOAuth.isCallback("https", "burton-workspaces.github.io", "/burton-builds/oauth"))
        assertFalse(GitHubOAuth.isCallback("https", "burton-workspaces.github.io", "/burton-builds/new"))
        assertFalse(GitHubOAuth.isCallback("burtonbuilds", "new", null))
    }

    @Test
    fun authorizeUrlIncludesPkce() {
        val pkce = GitHubOAuth.pkce()
        val url = GitHubOAuth.authorizeUrl("abc", pkce.state, pkce.challenge)
        assertTrue(url.startsWith("https://github.com/login/oauth/authorize?"))
        assertTrue(url.contains("client_id=abc"))
        assertTrue(url.contains("code_challenge_method=S256"))
        assertTrue(url.contains("code_challenge=${pkce.challenge}"))
        assertTrue(url.contains("scope="))
        assertTrue(url.contains("workflow"))
        assertEquals(43, pkce.challenge.length)
    }
}
