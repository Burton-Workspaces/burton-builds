package com.burton.builds.domain

data class Account(
    val login: String = "",
    val name: String = "",
    val email: String = "",
    val avatarUrl: String = "",
    val htmlUrl: String = "",
) {
    val display: String get() = name.ifBlank { login }
}

data class TrackedApp(
    val repo: String,
    val name: String,
    val description: String = "",
    val applicationId: String = "",
    val htmlUrl: String = "",
    val language: String = "",
    val defaultBranch: String = "master",
    val installed: Boolean = false,
    val subscribed: Boolean = false,
) {
    val shortName: String
        get() = name.removePrefix("Burton ").ifBlank { repo.substringAfterLast('/') }
}

data class WorkflowRun(
    val repo: String,
    val id: Long,
    val name: String,
    val displayTitle: String,
    val runNumber: Int,
    val event: String,
    val status: String,
    val conclusion: String = "",
    val headBranch: String = "",
    val headSha: String = "",
    val htmlUrl: String = "",
    val actorLogin: String = "",
    val actorAvatarUrl: String = "",
    val workflowId: Long = 0,
    val path: String = "",
    val createdAt: String = "",
    val updatedAt: String = "",
    val runStartedAt: String = "",
) {
    val completed: Boolean get() = status.equals("completed", ignoreCase = true)
    val inProgress: Boolean get() = !completed
    val failed: Boolean
        get() = conclusion.equals("failure", true) ||
            conclusion.equals("timed_out", true) ||
            conclusion.equals("startup_failure", true)
    val cancelled: Boolean get() = conclusion.equals("cancelled", true) || conclusion.equals("skipped", true)
    val succeeded: Boolean get() = conclusion.equals("success", true)
    val key: String get() = "$repo/$id"
    val title: String get() = displayTitle.ifBlank { name }.ifBlank { "Run #$runNumber" }
}

data class WorkflowJob(
    val id: Long,
    val runId: Long,
    val name: String,
    val status: String,
    val conclusion: String = "",
    val htmlUrl: String = "",
    val startedAt: String = "",
    val completedAt: String = "",
) {
    val completed: Boolean get() = status.equals("completed", ignoreCase = true)
}

data class Workflow(
    val id: Long,
    val name: String,
    val path: String,
    val state: String = "active",
    val htmlUrl: String = "",
) {
    val active: Boolean get() = state.equals("active", ignoreCase = true)
}

data class RunDetail(
    val run: WorkflowRun,
    val jobs: List<WorkflowJob> = emptyList(),
)

enum class RunFilter {
    ALL, FAILED, ACTIVE
}

data class BuildsSnapshot(
    val tokenPresent: Boolean = false,
    val scanning: Boolean = false,
    val error: String? = null,
    val account: Account? = null,
    val catalog: List<TrackedApp> = emptyList(),
    val inbox: List<WorkflowRun> = emptyList(),
    val deviceUserCode: String = "",
    val deviceVerificationUri: String = "",
) {
    val subscribed: List<TrackedApp> get() = catalog.filter { it.subscribed }
    val failed: List<WorkflowRun> get() = inbox.filter { it.failed }
}

object TimeText {
    fun short(iso: String): String {
        if (iso.isBlank()) return ""
        val date = iso.take(10)
        if (date.length < 10) return iso
        val year = date.substring(0, 4)
        val month = date.substring(5, 7).toIntOrNull() ?: return date
        val day = date.substring(8, 10).trimStart('0').ifBlank { date.substring(8, 10) }
        val months = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val label = months.getOrNull(month - 1) ?: date.substring(5, 7)
        return "$label $day, $year"
    }
}

object RunStatus {
    fun label(run: WorkflowRun): String = when {
        run.inProgress -> when (run.status.lowercase()) {
            "queued" -> "Queued"
            "waiting" -> "Waiting"
            "in_progress" -> "Running"
            else -> run.status.replace('_', ' ').replaceFirstChar { it.uppercase() }
        }
        run.failed -> "Failed"
        run.cancelled -> if (run.conclusion.equals("skipped", true)) "Skipped" else "Cancelled"
        run.succeeded -> "Passed"
        run.conclusion.isNotBlank() -> run.conclusion.replace('_', ' ').replaceFirstChar { it.uppercase() }
        else -> run.status.replaceFirstChar { it.uppercase() }
    }

    fun label(job: WorkflowJob): String {
        val asRun = WorkflowRun(
            repo = "",
            id = job.id,
            name = job.name,
            displayTitle = job.name,
            runNumber = 0,
            event = "",
            status = job.status,
            conclusion = job.conclusion,
        )
        return label(asRun)
    }
}
