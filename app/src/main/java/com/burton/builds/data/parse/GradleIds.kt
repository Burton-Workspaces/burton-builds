package com.burton.builds.data.parse

object GradleIds {
    private val APPLICATION_ID = Regex("""applicationId\s*=\s*"([^"]+)"""")
    private val NAMESPACE = Regex("""namespace\s*=\s*"([^"]+)"""")
    private val MANIFEST_PACKAGE = Regex("""package\s*=\s*"([^"]+)"""")

    fun applicationId(gradleText: String): String =
        APPLICATION_ID.find(gradleText)?.groupValues?.get(1)
            ?: NAMESPACE.find(gradleText)?.groupValues?.get(1)
            ?: ""

    fun manifestPackage(xml: String): String =
        MANIFEST_PACKAGE.find(xml)?.groupValues?.get(1).orEmpty()
}

object AndroidCatalog {
    private val SKIP_NAME = listOf(
        "-site", "-fdroid", "fdroid", "docs", "templates", "website",
    )
    private val SKIP_PREFIX = listOf("rabun", "rgit")

    fun looksLikeAndroidRepo(name: String, language: String, description: String = ""): Boolean {
        val n = name.lowercase()
        if (SKIP_PREFIX.any { n.startsWith(it) }) return false
        if (SKIP_NAME.any { n.contains(it) }) return false
        val lang = language.lowercase()
        if (lang == "kotlin" || lang == "java") return true
        if (n.contains("android")) return true
        val blob = "$n ${description.lowercase()}"
        return blob.contains("android") && (lang.isBlank() || lang == "kotlin" || lang == "java")
    }
}
