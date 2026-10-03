package com.burton.builds.ui.run

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burton.builds.domain.RunStatus
import com.burton.builds.domain.TimeText
import com.burton.builds.domain.WorkflowJob
import com.burton.builds.domain.WorkflowRun
import com.burton.builds.ui.components.EmptyStatePanel
import com.burton.builds.ui.components.RowsSkeleton
import com.burton.builds.ui.components.statusColor
import com.burton.builds.ui.theme.BurtonCharcoal
import com.burton.builds.ui.theme.BurtonDanger
import com.burton.builds.ui.theme.BurtonIvory
import com.burton.builds.ui.theme.BurtonMute
import com.burton.builds.ui.theme.BurtonVoid

@Composable
fun RunScreen(
    onBack: () -> Unit,
    viewModel: RunViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val run = ui.detail?.run
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Back", tint = BurtonIvory)
            }
            Text(
                run?.name?.ifBlank { "Run" } ?: "Run",
                style = MaterialTheme.typography.headlineLarge,
                color = BurtonIvory,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!run?.htmlUrl.isNullOrBlank()) {
                IconButton(
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(run?.htmlUrl)))
                    },
                ) {
                    Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = "Open on GitHub", tint = BurtonIvory)
                }
            }
        }
        when {
            ui.scanning && ui.detail == null -> {
                Spacer(Modifier.height(16.dp))
                RowsSkeleton(count = 4)
            }
            run == null -> {
                Spacer(Modifier.height(16.dp))
                EmptyStatePanel(title = ui.error ?: "Run not found")
            }
            else -> {
                Text(run.title, style = MaterialTheme.typography.bodyLarge, color = BurtonIvory)
                Spacer(Modifier.height(4.dp))
                Text(
                    buildList {
                        add(run.repo)
                        add("#${run.runNumber}")
                        if (run.headBranch.isNotBlank()) add(run.headBranch)
                        add(run.event)
                    }.joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = BurtonMute,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    RunStatus.label(run),
                    style = MaterialTheme.typography.labelLarge,
                    color = statusColor(run),
                )
                if (ui.error != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(ui.error ?: "", color = BurtonIvory, style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.height(16.dp))
                val jobs = ui.detail?.jobs.orEmpty()
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (jobs.isEmpty()) {
                        item {
                            EmptyStatePanel(title = "No jobs yet")
                        }
                    } else {
                        items(jobs, key = { it.id }) { job ->
                            JobRow(job)
                        }
                    }
                }
                RunActions(
                    run = run,
                    busy = ui.busy,
                    onRerun = { viewModel.rerun(failedOnly = false) },
                    onRerunFailed = { viewModel.rerun(failedOnly = true) },
                    onCancel = viewModel::cancel,
                )
            }
        }
    }
}

@Composable
private fun JobRow(job: WorkflowJob) {
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BurtonCharcoal, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                job.name,
                style = MaterialTheme.typography.titleMedium,
                color = BurtonIvory,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                RunStatus.label(job),
                style = MaterialTheme.typography.labelLarge,
                color = statusColor(asRun),
            )
        }
        val whenText = TimeText.short(job.completedAt.ifBlank { job.startedAt })
        if (whenText.isNotBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(whenText, style = MaterialTheme.typography.bodyMedium, color = BurtonMute)
        }
    }
}

@Composable
private fun RunActions(
    run: WorkflowRun,
    busy: Boolean,
    onRerun: () -> Unit,
    onRerunFailed: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (run.inProgress) {
            Button(
                onClick = onCancel,
                enabled = !busy,
                colors = ButtonDefaults.buttonColors(containerColor = BurtonDanger, contentColor = BurtonIvory),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Cancel run")
            }
        } else {
            Button(
                onClick = onRerun,
                enabled = !busy,
                colors = ButtonDefaults.buttonColors(containerColor = BurtonIvory, contentColor = BurtonVoid),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Rerun")
            }
            if (run.failed) {
                TextButton(
                    onClick = onRerunFailed,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Rerun failed jobs", color = BurtonIvory)
                }
            }
        }
    }
}
