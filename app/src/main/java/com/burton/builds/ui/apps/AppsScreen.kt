package com.burton.builds.ui.apps

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
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burton.builds.domain.TrackedApp
import com.burton.builds.ui.components.EmptyStatePanel
import com.burton.builds.ui.components.RowsSkeleton
import com.burton.builds.ui.theme.BurtonCharcoal
import com.burton.builds.ui.theme.BurtonIvory
import com.burton.builds.ui.theme.BurtonMute
import com.burton.builds.ui.theme.BurtonSand
import com.burton.builds.ui.theme.BurtonVoid

@Composable
fun AppsScreen(
    onOpenApp: (String) -> Unit,
    viewModel: AppsViewModel = hiltViewModel(),
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
                text = "Apps",
                style = MaterialTheme.typography.headlineLarge,
                color = BurtonIvory,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = viewModel::refresh) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Refresh catalog", tint = BurtonIvory)
            }
        }
        Text(
            text = "Public Android apps in Burton-Workspaces. Select which to track.",
            style = MaterialTheme.typography.bodyMedium,
            color = BurtonMute,
        )
        Spacer(Modifier.height(16.dp))
        when {
            snapshot.scanning && snapshot.catalog.isEmpty() -> RowsSkeleton()
            snapshot.catalog.isEmpty() -> {
                EmptyStatePanel(
                    title = "No public Android apps found",
                    action = "Refresh catalog",
                    onAction = viewModel::refresh,
                )
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(snapshot.catalog, key = { it.repo }) { app ->
                        AppRow(
                            app = app,
                            onToggle = { viewModel.setSubscribed(app.repo, !app.subscribed) },
                            onOpen = { onOpenApp(app.repo) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AppRow(
    app: TrackedApp,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BurtonCharcoal, RoundedCornerShape(18.dp))
            .clickable(onClick = onOpen)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = app.subscribed,
            onCheckedChange = { onToggle() },
            colors = CheckboxDefaults.colors(
                checkedColor = BurtonSand,
                uncheckedColor = BurtonMute,
                checkmarkColor = BurtonVoid,
            ),
        )
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                app.name,
                style = MaterialTheme.typography.titleMedium,
                color = BurtonIvory,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            val bits = buildList {
                add(app.repo)
                if (app.installed) add("Installed")
                if (app.applicationId.isNotBlank()) add(app.applicationId)
            }
            Text(
                bits.joinToString(" · "),
                style = MaterialTheme.typography.bodyMedium,
                color = BurtonMute,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
