package com.burton.builds.data.parse

import com.burton.builds.data.parse.TinyJson.int
import com.burton.builds.data.parse.TinyJson.long
import com.burton.builds.data.parse.TinyJson.obj
import com.burton.builds.data.parse.TinyJson.str
import com.burton.builds.domain.Account
import com.burton.builds.domain.TrackedApp
import com.burton.builds.domain.Workflow
import com.burton.builds.domain.WorkflowJob
import com.burton.builds.domain.WorkflowRun

object GitHubCodec {
    fun account(map: Map<String, Any?>): Account = Account(
        login = map.str("login"),
        name = map.str("name"),
        email = map.str("email"),
        avatarUrl = map.str("avatar_url"),
        htmlUrl = map.str("html_url"),
    )

    fun repoApp(map: Map<String, Any?>, applicationId: String = ""): TrackedApp {
        val full = map.str("full_name").ifBlank {
            val owner = map.obj("owner").str("login")
            val name = map.str("name")
            if (owner.isBlank()) name else "$owner/$name"
        }
        return TrackedApp(
            repo = full,
            name = displayName(map.str("name"), map.str("description")),
            description = map.str("description"),
            applicationId = applicationId,
            htmlUrl = map.str("html_url"),
            language = map.str("language"),
            defaultBranch = map.str("default_branch").ifBlank { "master" },
        )
    }

    fun workflowRun(map: Map<String, Any?>, repo: String = ""): WorkflowRun {
        val repository = map.obj("repository")
        val repoName = repo.ifBlank { repository.str("full_name") }
        val commit = map.obj("head_commit")
        val actor = map.obj("actor")
        val display = map.str("display_title").ifBlank {
            commit.str("message").substringBefore('\n')
        }
        return WorkflowRun(
            repo = repoName,
            id = map.long("id"),
            name = map.str("name"),
            displayTitle = display,
            runNumber = map.int("run_number"),
            event = map.str("event"),
            status = map.str("status"),
            conclusion = map.str("conclusion"),
            headBranch = map.str("head_branch"),
            headSha = map.str("head_sha"),
            htmlUrl = map.str("html_url"),
            actorLogin = actor.str("login"),
            actorAvatarUrl = actor.str("avatar_url"),
            workflowId = map.long("workflow_id"),
            path = map.str("path"),
            createdAt = map.str("created_at"),
            updatedAt = map.str("updated_at"),
            runStartedAt = map.str("run_started_at"),
        )
    }

    fun job(map: Map<String, Any?>): WorkflowJob = WorkflowJob(
        id = map.long("id"),
        runId = map.long("run_id"),
        name = map.str("name"),
        status = map.str("status"),
        conclusion = map.str("conclusion"),
        htmlUrl = map.str("html_url"),
        startedAt = map.str("started_at"),
        completedAt = map.str("completed_at"),
    )

    fun workflow(map: Map<String, Any?>): Workflow = Workflow(
        id = map.long("id"),
        name = map.str("name"),
        path = map.str("path"),
        state = map.str("state"),
        htmlUrl = map.str("html_url"),
    )

    fun displayName(repoName: String, description: String): String {
        val slug = repoName.removeSuffix("-android")
        val words = slug.split('-').filter { it.isNotBlank() }.joinToString(" ") { part ->
            part.replaceFirstChar { ch -> if (ch.isLowerCase()) ch.titlecase() else ch.toString() }
        }
        return words.ifBlank { repoName.ifBlank { description } }
    }
}
