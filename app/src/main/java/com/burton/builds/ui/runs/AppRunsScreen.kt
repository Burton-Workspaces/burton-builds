package com.burton.builds.ui.runs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burton.builds.domain.RunFilter
import com.burton.builds.domain.Workflow
import com.burton.builds.ui.components.BurtonModalSheet
import com.burton.builds.ui.components.EmptyStatePanel
import com.burton.builds.ui.components.RowsSkeleton
import com.burton.builds.ui.components.RunRow
import com.burton.builds.ui.theme.BurtonCharcoal
import com.burton.builds.ui.theme.BurtonElevated
import com.burton.builds.ui.theme.BurtonIvory
import com.burton.builds.ui.theme.BurtonMute

@Composable
fun AppRunsScreen(
    onBack: () -> Unit,
    onOpenRun: (Long) -> Unit,
    viewModel: AppRunsViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    var showDispatch by remember { mutableStateOf(false) }
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
                ui.appName,
                style = MaterialTheme.typography.headlineLarge,
                color = BurtonIvory,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { showDispatch = true }, enabled = !ui.dispatching) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = "Run workflow", tint = BurtonIvory)
            }
        }
        Text(ui.repo, style = MaterialTheme.typography.bodyMedium, color = BurtonMute)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip("All", ui.filter == RunFilter.ALL) { viewModel.setFilter(RunFilter.ALL) }
            FilterChip("Failed", ui.filter == RunFilter.FAILED) { viewModel.setFilter(RunFilter.FAILED) }
            FilterChip("Running", ui.filter == RunFilter.ACTIVE) { viewModel.setFilter(RunFilter.ACTIVE) }
        }
        Spacer(Modifier.height(16.dp))
        when {
            ui.scanning && ui.runs.isEmpty() -> RowsSkeleton()
            ui.visible.isEmpty() && ui.error == null -> {
                val title = when (ui.filter) {
                    RunFilter.FAILED -> "Everything is up to date"
                    RunFilter.ACTIVE -> "Nothing running"
                    RunFilter.ALL -> "No workflow runs yet"
                }
                EmptyStatePanel(title = title)
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (ui.error != null) {
                        item(key = "error") {
                            Text(ui.error ?: "", color = BurtonIvory, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    items(ui.visible, key = { it.key }) { run ->
                        RunRow(
                            run = run,
                            showRepo = false,
                            onClick = { onOpenRun(run.id) },
                        )
                    }
                }
            }
        }
    }
    if (showDispatch) {
        BurtonModalSheet(onDismiss = { showDispatch = false }) {
            Text("Run workflow", style = MaterialTheme.typography.headlineMedium, color = BurtonIvory)
            Spacer(Modifier.height(6.dp))
            Text(
                "Dispatches on ${ui.defaultBranch}. The workflow file needs workflow_dispatch.",
                style = MaterialTheme.typography.bodyMedium,
                color = BurtonMute,
            )
            Spacer(Modifier.height(16.dp))
            if (ui.workflows.isEmpty()) {
                EmptyStatePanel(title = "No active workflows")
            } else {
                ui.workflows.forEach { workflow ->
                    WorkflowRow(workflow = workflow) {
                        viewModel.dispatch(workflow)
                        showDispatch = false
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun WorkflowRow(workflow: Workflow, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BurtonCharcoal, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(workflow.name, style = MaterialTheme.typography.titleMedium, color = BurtonIvory)
        Spacer(Modifier.height(4.dp))
        Text(workflow.path, style = MaterialTheme.typography.bodyMedium, color = BurtonMute)
    }
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) BurtonIvory else BurtonMute,
        modifier = Modifier
            .background(if (selected) BurtonElevated else BurtonCharcoal, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}
