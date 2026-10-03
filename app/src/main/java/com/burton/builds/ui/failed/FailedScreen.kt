package com.burton.builds.ui.failed

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burton.builds.ui.components.EmptyStatePanel
import com.burton.builds.ui.components.RowsSkeleton
import com.burton.builds.ui.components.RunRow
import com.burton.builds.ui.theme.BurtonIvory
import com.burton.builds.ui.theme.BurtonMute

@Composable
fun FailedScreen(
    onOpenRun: (String, Long) -> Unit,
    onChooseApps: () -> Unit,
    viewModel: FailedViewModel = hiltViewModel(),
) {
    val snapshot by viewModel.state.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Failed",
                style = MaterialTheme.typography.headlineLarge,
                color = BurtonIvory,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = viewModel::refresh) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Refresh", tint = BurtonIvory)
            }
        }
        Text(
            text = "Failed and timed-out runs across subscribed apps.",
            style = MaterialTheme.typography.bodyMedium,
            color = BurtonMute,
        )
        Spacer(Modifier.height(16.dp))
        when {
            snapshot.scanning && snapshot.inbox.isEmpty() -> RowsSkeleton()
            snapshot.subscribed.isEmpty() && !snapshot.scanning -> {
                EmptyStatePanel(
                    title = "Nothing subscribed yet",
                    action = "Choose apps",
                    onAction = onChooseApps,
                )
            }
            snapshot.failed.isEmpty() && !snapshot.scanning -> {
                EmptyStatePanel(title = "Everything is up to date")
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(snapshot.failed, key = { it.key }) { run ->
                        RunRow(
                            run = run,
                            onClick = { onOpenRun(run.repo, run.id) },
                        )
                    }
                }
            }
        }
    }
}
