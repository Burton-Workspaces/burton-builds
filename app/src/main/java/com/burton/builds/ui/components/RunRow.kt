package com.burton.builds.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.burton.builds.domain.RunStatus
import com.burton.builds.domain.TimeText
import com.burton.builds.domain.WorkflowRun
import com.burton.builds.ui.theme.BurtonCharcoal
import com.burton.builds.ui.theme.BurtonDanger
import com.burton.builds.ui.theme.BurtonIvory
import com.burton.builds.ui.theme.BurtonMute
import com.burton.builds.ui.theme.BurtonSand

@Composable
fun RunRow(
    run: WorkflowRun,
    onClick: () -> Unit,
    showRepo: Boolean = true,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BurtonCharcoal, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = run.title,
                style = MaterialTheme.typography.titleMedium,
                color = BurtonIvory,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            val meta = buildList {
                if (showRepo) add(run.repo.substringAfterLast('/'))
                add(run.name.ifBlank { "workflow" })
                if (run.headBranch.isNotBlank()) add(run.headBranch)
                add("#${run.runNumber}")
            }.joinToString(" · ")
            Text(
                text = meta,
                style = MaterialTheme.typography.bodyMedium,
                color = BurtonMute,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = RunStatus.label(run),
                style = MaterialTheme.typography.labelLarge,
                color = statusColor(run),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = TimeText.short(run.updatedAt.ifBlank { run.createdAt }),
                style = MaterialTheme.typography.bodyMedium,
                color = BurtonMute,
            )
        }
    }
}

fun statusColor(run: WorkflowRun): Color = when {
    run.inProgress -> BurtonIvory
    run.failed -> BurtonDanger
    run.cancelled -> BurtonMute
    else -> BurtonSand
}
