package com.burton.builds.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.burton.builds.ui.theme.BurtonElevated
import com.burton.builds.ui.theme.BurtonIvory
import com.burton.builds.ui.theme.BurtonSand

@Composable
fun EmptyStatePanel(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(BurtonElevated, RoundedCornerShape(20.dp))
            .then(
                if (onAction != null) {
                    Modifier.clickable(role = Role.Button, onClick = onAction)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 20.dp, vertical = 22.dp),
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = BurtonIvory,
        )
        if (action != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                action,
                style = MaterialTheme.typography.bodyLarge,
                color = BurtonSand,
            )
        }
    }
}
