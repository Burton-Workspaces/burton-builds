package com.burton.builds.data.github

import android.util.Base64
import com.burton.builds.data.parse.AndroidCatalog
import com.burton.builds.data.parse.GitHubCodec
import com.burton.builds.data.parse.GradleIds
import com.burton.builds.data.parse.TinyJson.objList
import com.burton.builds.data.parse.TinyJson.str
import com.burton.builds.domain.Account
import com.burton.builds.domain.RunDetail
import com.burton.builds.domain.TrackedApp
import com.burton.builds.domain.Workflow
import com.burton.builds.domain.WorkflowRun
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GitHubTracker @Inject constructor(
    private val api: GitHubApi,
) {
    val id: String = "github"
    val displayName: String = "GitHub"
    val org: String = ORG

    suspend fun currentUser(): Account = GitHubCodec.account(api.getObject("user"))

    suspend fun discoverAndroidApps(): List<TrackedApp> {
        val repos = mutableListOf<Map<String, Any?>>()
        var page = 1
        while (page <= 5) {
            val batch = api.getList(
                "orgs/$ORG/repos",
                mapOf("type" to "public", "per_page" to "100", "page" to page.toString(), "sort" to "full_name"),
            )
            if (batch.isEmpty()) break
            repos += batch
            if (batch.size < 100) break
            page++
        }
        return repos.mapNotNull { row ->
            val name = row.str("name")
            val language = row.str("language")
            val description = row.str("description")
            if (!AndroidCatalog.looksLikeAndroidRepo(name, language, description)) return@mapNotNull null
            val full = row.str("full_name")
            val applicationId = runCatching { readApplicationId(full) }.getOrDefault("")
            if (applicationId.isBlank() && language.lowercase() !in setOf("kotlin", "java") && !name.contains("android", true)) {
                return@mapNotNull null
            }
            GitHubCodec.repoApp(row, applicationId)
        }.sortedBy { it.name.lowercase() }
    }

    suspend fun listRuns(repo: String, perPage: Int = 20): List<WorkflowRun> {
        val result = api.getObject(
            "repos/$repo/actions/runs",
            mapOf("per_page" to perPage.toString()),
        )
        return result.objList("workflow_runs").map { GitHubCodec.workflowRun(it, repo) }
    }

    suspend fun getRun(repo: String, runId: Long): RunDetail {
        val run = GitHubCodec.workflowRun(api.getObject("repos/$repo/actions/runs/$runId"), repo)
        val jobs = api.getObject("repos/$repo/actions/runs/$runId/jobs", mapOf("per_page" to "100"))
            .objList("jobs")
            .map(GitHubCodec::job)
        return RunDetail(run = run, jobs = jobs)
    }

    suspend fun listWorkflows(repo: String): List<Workflow> {
        val result = api.getObject("repos/$repo/actions/workflows", mapOf("per_page" to "100"))
        return result.objList("workflows").map(GitHubCodec::workflow).filter { it.active }
    }

    suspend fun rerun(repo: String, runId: Long, failedOnly: Boolean = false) {
        val path = if (failedOnly) {
            "repos/$repo/actions/runs/$runId/rerun-failed-jobs"
        } else {
            "repos/$repo/actions/runs/$runId/rerun"
        }
        api.post(path)
    }

    suspend fun cancel(repo: String, runId: Long) {
        api.post("repos/$repo/actions/runs/$runId/cancel")
    }

    suspend fun dispatch(repo: String, workflowId: Long, ref: String) {
        api.post(
            "repos/$repo/actions/workflows/$workflowId/dispatches",
            mapOf("ref" to ref),
        )
    }

    private suspend fun readApplicationId(repo: String): String {
        val gradle = readFile(repo, "app/build.gradle.kts").ifBlank { readFile(repo, "app/build.gradle") }
        val fromGradle = GradleIds.applicationId(gradle)
        if (fromGradle.isNotBlank()) return fromGradle
        val manifest = readFile(repo, "app/src/main/AndroidManifest.xml")
        return GradleIds.manifestPackage(manifest)
    }

    private suspend fun readFile(repo: String, path: String): String {
        val row = runCatching { api.getObject("repos/$repo/contents/$path") }.getOrNull() ?: return ""
        if (row.str("encoding") != "base64") return row.str("content")
        val raw = row.str("content").replace("\n", "")
        return String(Base64.decode(raw, Base64.DEFAULT), Charsets.UTF_8)
    }

    companion object {
        const val ORG = "Burton-Workspaces"
    }
}
