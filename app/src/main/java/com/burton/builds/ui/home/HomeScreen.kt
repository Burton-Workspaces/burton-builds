package com.burton.builds.ui.home

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
import androidx.compose.material.icons.rounded.Settings
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
import com.burton.builds.ui.components.EmptyStatePanel
import com.burton.builds.ui.components.RowsSkeleton
import com.burton.builds.ui.components.RunRow
import com.burton.builds.ui.settings.SettingsModal
import com.burton.builds.ui.theme.BurtonIvory
import com.burton.builds.ui.theme.BurtonMute

@Composable
fun HomeScreen(
    onOpenRun: (String, Long) -> Unit,
    onChooseApps: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val snapshot by viewModel.state.collectAsStateWithLifecycle()
    var showSettings by remember { mutableStateOf(false) }
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
                text = "Inbox",
                style = MaterialTheme.typography.headlineLarge,
                color = BurtonIvory,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = viewModel::refresh) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Refresh", tint = BurtonIvory)
            }
            IconButton(onClick = { showSettings = true }) {
                Icon(Icons.Rounded.Settings, contentDescription = "Settings", tint = BurtonIvory)
            }
        }
        val account = snapshot.account
        val running = snapshot.inbox.count { it.inProgress }
        Text(
            text = when {
                account == null && snapshot.scanning -> "Connecting to GitHub"
                account == null -> "Sign in with GitHub"
                snapshot.subscribed.isEmpty() -> "Choose which apps to track"
                snapshot.inbox.isEmpty() && !snapshot.scanning -> "Recent Actions runs across subscribed apps"
                running > 0 -> "${account.display} · $running running"
                else -> "${account.display} · ${snapshot.inbox.size} runs"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = BurtonMute,
        )
        Spacer(Modifier.height(16.dp))
        when {
            snapshot.scanning && snapshot.inbox.isEmpty() && snapshot.catalog.isEmpty() -> RowsSkeleton()
            snapshot.subscribed.isEmpty() && !snapshot.scanning -> {
                EmptyStatePanel(
                    title = "Nothing subscribed yet",
                    action = "Choose apps",
                    onAction = onChooseApps,
                )
            }
            snapshot.inbox.isEmpty() && !snapshot.scanning -> {
                EmptyStatePanel(title = "Everything is up to date")
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (snapshot.error != null) {
                        item(key = "error") {
                            Text(snapshot.error ?: "", color = BurtonIvory, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    items(snapshot.inbox, key = { it.key }) { run ->
                        RunRow(
                            run = run,
                            onClick = { onOpenRun(run.repo, run.id) },
                        )
                    }
                }
            }
        }
    }
    if (showSettings) {
        SettingsModal(onDismiss = { showSettings = false })
    }
}
