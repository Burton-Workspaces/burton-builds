package com.burton.builds.data.parse

import com.burton.builds.data.parse.TinyJson.parseObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GitHubCodecTest {
    @Test
    fun mapsWorkflowRun() {
        val json = """
            {
              "id": 30433642,
              "name": "CI",
              "head_branch": "master",
              "head_sha": "acb48f9c",
              "display_title": "fix: parser",
              "run_number": 12,
              "event": "push",
              "status": "completed",
              "conclusion": "failure",
              "workflow_id": 159038,
              "html_url": "https://github.com/Burton-Workspaces/burton-pod/actions/runs/30433642",
              "created_at": "2026-01-22T19:33:08Z",
              "updated_at": "2026-01-22T19:40:08Z",
              "actor": {"login": "ryco", "avatar_url": "https://example/a"},
              "repository": {"full_name": "Burton-Workspaces/burton-pod"}
            }
        """.trimIndent()
        val run = GitHubCodec.workflowRun(parseObject(json))
        assertEquals("Burton-Workspaces/burton-pod", run.repo)
        assertEquals(30433642L, run.id)
        assertEquals("CI", run.name)
        assertEquals("fix: parser", run.displayTitle)
        assertEquals(12, run.runNumber)
        assertTrue(run.failed)
        assertEquals("ryco", run.actorLogin)
    }

    @Test
    fun mapsRepoApp() {
        val json = """
            {
              "full_name": "Burton-Workspaces/burton-pod",
              "name": "burton-pod",
              "description": "Podcast client",
              "html_url": "https://github.com/Burton-Workspaces/burton-pod",
              "language": "Kotlin",
              "default_branch": "master"
            }
        """.trimIndent()
        val app = GitHubCodec.repoApp(parseObject(json), "com.burton.pod")
        assertEquals("Burton-Workspaces/burton-pod", app.repo)
        assertEquals("Burton Pod", app.name)
        assertEquals("com.burton.pod", app.applicationId)
        assertEquals("master", app.defaultBranch)
    }
}
