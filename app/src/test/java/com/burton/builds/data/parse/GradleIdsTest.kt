package com.burton.builds.data.parse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GradleIdsTest {
    @Test
    fun readsApplicationIdFromKotlinDsl() {
        val text = """
            android {
                namespace = "com.burton.pod"
                defaultConfig {
                    applicationId = "com.burton.pod"
                }
            }
        """.trimIndent()
        assertEquals("com.burton.pod", GradleIds.applicationId(text))
    }

    @Test
    fun fallsBackToNamespace() {
        assertEquals("com.burton.builds", GradleIds.applicationId("""namespace = "com.burton.builds""""))
    }

    @Test
    fun catalogSkipsSitesAndKeepsKotlinApps() {
        assertTrue(AndroidCatalog.looksLikeAndroidRepo("burton-pod", "Kotlin"))
        assertTrue(AndroidCatalog.looksLikeAndroidRepo("burton-photos-android", "Kotlin"))
        assertFalse(AndroidCatalog.looksLikeAndroidRepo("burton-apps-site", "Kotlin"))
        assertFalse(AndroidCatalog.looksLikeAndroidRepo("burton-sonos-fdroid", ""))
        assertFalse(AndroidCatalog.looksLikeAndroidRepo("rabun-git", "Rust"))
    }
}
