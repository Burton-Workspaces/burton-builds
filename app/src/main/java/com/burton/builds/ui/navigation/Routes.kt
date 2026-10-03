package com.burton.builds.ui.navigation

object Routes {
    const val INBOX = "inbox"
    const val APPS = "apps"
    const val FAILED = "failed"
    const val RUNS = "runs/{repo}"
    const val RUN = "run/{repo}/{runId}"

    fun runs(repo: String) = "runs/${encode(repo)}"
    fun run(repo: String, runId: Long) = "run/${encode(repo)}/$runId"

    fun encode(value: String): String = android.net.Uri.encode(value)
    fun decode(value: String?): String = android.net.Uri.decode(value.orEmpty())
}
