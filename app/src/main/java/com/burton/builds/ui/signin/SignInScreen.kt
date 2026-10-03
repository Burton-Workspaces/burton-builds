package com.burton.builds.ui.signin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.burton.builds.ui.theme.BurtonBlack
import com.burton.builds.ui.theme.BurtonCharcoal
import com.burton.builds.ui.theme.BurtonIvory
import com.burton.builds.ui.theme.BurtonMute
import com.burton.builds.ui.theme.BurtonSand
import com.burton.builds.ui.theme.BurtonVoid

@Composable
fun SignInScreen(
    viewModel: SignInViewModel = hiltViewModel(),
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BurtonBlack)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Burton Builds", style = MaterialTheme.typography.headlineLarge, color = BurtonIvory)
        Spacer(Modifier.height(12.dp))
        Text(
            "Sign in with GitHub to watch Actions runs on Burton Workspaces Android apps. The token stays on this phone.",
            style = MaterialTheme.typography.bodyLarge,
            color = BurtonMute,
        )
        Spacer(Modifier.height(20.dp))
        Button(
            onClick = { viewModel.connectWithGitHub { url -> openAuthorizeUrl(context, url) } },
            enabled = !ui.busy,
            colors = ButtonDefaults.buttonColors(containerColor = BurtonIvory, contentColor = BurtonVoid),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (ui.busy) {
                CircularProgressIndicator(color = BurtonSand, modifier = Modifier.height(18.dp))
            } else {
                Text("Connect with GitHub")
            }
        }
        if (ui.deviceUserCode.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BurtonCharcoal, RoundedCornerShape(18.dp))
                    .padding(16.dp),
            ) {
                Text("Enter this code on GitHub", style = MaterialTheme.typography.labelSmall, color = BurtonMute)
                Spacer(Modifier.height(8.dp))
                Text(ui.deviceUserCode, style = MaterialTheme.typography.headlineMedium, color = BurtonIvory)
                Spacer(Modifier.height(8.dp))
                Text(
                    ui.deviceVerificationUri.ifBlank { "https://github.com/login/device" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = BurtonSand,
                )
            }
        }
        if (!ui.oauthConfigured) {
            Spacer(Modifier.height(12.dp))
            Text(
                "This build has no GitHub Client ID yet. After creating an OAuth app on GitHub with callback https://burton-workspaces.github.io/burton-builds/oauth/, put the public Client ID in github/client-id.txt — or paste a token below.",
                style = MaterialTheme.typography.bodyMedium,
                color = BurtonMute,
            )
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = viewModel::togglePaste, enabled = !ui.busy) {
            Text(
                if (ui.pasteOpen) "Hide token field" else "Use a token",
                color = BurtonSand,
            )
        }
        if (ui.pasteOpen) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BurtonCharcoal, RoundedCornerShape(18.dp))
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            ) {
                Text("Access token", style = MaterialTheme.typography.labelSmall, color = BurtonMute)
                Spacer(Modifier.height(8.dp))
                BasicTextField(
                    value = ui.token,
                    onValueChange = viewModel::onTokenChange,
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    textStyle = MaterialTheme.typography.titleLarge.copy(color = BurtonIvory),
                    cursorBrush = SolidColor(BurtonIvory),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { inner ->
                        if (ui.token.isBlank()) {
                            Text("ghp_… or github_pat_…", color = BurtonMute, style = MaterialTheme.typography.titleLarge)
                        }
                        inner()
                    },
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "Paste a classic token with public_repo and workflow, or a fine-grained token that can read Actions on Burton-Workspaces.",
                style = MaterialTheme.typography.bodyMedium,
                color = BurtonMute,
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = viewModel::connect,
                enabled = !ui.busy,
                colors = ButtonDefaults.buttonColors(containerColor = BurtonCharcoal, contentColor = BurtonIvory),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Connect with token")
            }
        }
        if (ui.error != null) {
            Spacer(Modifier.height(12.dp))
            Text(ui.error ?: "", color = BurtonIvory, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
