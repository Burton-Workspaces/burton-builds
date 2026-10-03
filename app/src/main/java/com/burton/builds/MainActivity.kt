package com.burton.builds

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.burton.builds.data.github.GitHubOAuth
import com.burton.builds.data.repository.BuildsRepository
import com.burton.builds.ui.apps.AppsScreen
import com.burton.builds.ui.failed.FailedScreen
import com.burton.builds.ui.home.HomeScreen
import com.burton.builds.ui.navigation.Routes
import com.burton.builds.ui.run.RunScreen
import com.burton.builds.ui.runs.AppRunsScreen
import com.burton.builds.ui.signin.SignInScreen
import com.burton.builds.ui.theme.BurtonBlack
import com.burton.builds.ui.theme.BurtonBuildsTheme
import com.burton.builds.ui.theme.BurtonIvory
import com.burton.builds.ui.theme.BurtonMute
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var repository: BuildsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleIncoming(intent)
        enableEdgeToEdge()
        setContent {
            BurtonBuildsTheme {
                val appViewModel: AppViewModel = hiltViewModel()
                val snapshot by appViewModel.state.collectAsStateWithLifecycle()
                if (snapshot.tokenPresent) {
                    BurtonApp()
                } else {
                    SignInScreen()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncoming(intent)
    }

    private fun handleIncoming(intent: Intent?) {
        val uri = intent?.data
        if (!GitHubOAuth.isCallback(uri?.scheme, uri?.host, uri?.path)) return
        val error = uri?.getQueryParameter("error")
        val description = uri?.getQueryParameter("error_description").orEmpty()
        val code = uri?.getQueryParameter("code").orEmpty()
        val state = uri?.getQueryParameter("state").orEmpty()
        lifecycleScope.launch {
            if (!error.isNullOrBlank()) {
                repository.failOauth(
                    description.ifBlank {
                        if (error == "access_denied") "GitHub login was cancelled." else error
                    },
                )
            } else {
                runCatching { repository.completeOAuth(code, state) }
            }
        }
    }
}

@Composable
private fun BurtonApp() {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val tabs = listOf(Routes.INBOX, Routes.APPS, Routes.FAILED)
    val selectedTab = if (route in tabs) route else Routes.INBOX
    val hideTabs = route?.startsWith("run") == true
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(BurtonBlack),
        containerColor = BurtonBlack,
        bottomBar = {
            if (!hideTabs) {
                NavigationBar(
                    containerColor = BurtonBlack,
                    contentColor = BurtonIvory,
                    modifier = Modifier.navigationBarsPadding(),
                ) {
                    NavigationBarItem(
                        selected = selectedTab == Routes.INBOX,
                        onClick = { navController.goTab(Routes.INBOX) },
                        icon = { Icon(Icons.Rounded.Home, contentDescription = "Inbox") },
                        label = { Text("Inbox") },
                        colors = navColors(selectedTab == Routes.INBOX),
                    )
                    NavigationBarItem(
                        selected = selectedTab == Routes.APPS,
                        onClick = { navController.goTab(Routes.APPS) },
                        icon = { Icon(Icons.Rounded.Inventory2, contentDescription = "Apps") },
                        label = { Text("Apps") },
                        colors = navColors(selectedTab == Routes.APPS),
                    )
                    NavigationBarItem(
                        selected = selectedTab == Routes.FAILED,
                        onClick = { navController.goTab(Routes.FAILED) },
                        icon = { Icon(Icons.Rounded.ErrorOutline, contentDescription = "Failed") },
                        label = { Text("Failed") },
                        colors = navColors(selectedTab == Routes.FAILED),
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.INBOX,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.INBOX) {
                HomeScreen(
                    onOpenRun = { repo, id -> navController.navigate(Routes.run(repo, id)) },
                    onChooseApps = { navController.goTab(Routes.APPS) },
                )
            }
            composable(Routes.APPS) {
                AppsScreen(onOpenApp = { navController.navigate(Routes.runs(it)) })
            }
            composable(Routes.FAILED) {
                FailedScreen(
                    onOpenRun = { repo, id -> navController.navigate(Routes.run(repo, id)) },
                    onChooseApps = { navController.goTab(Routes.APPS) },
                )
            }
            composable(
                Routes.RUNS,
                arguments = listOf(navArgument("repo") { type = NavType.StringType }),
            ) {
                val repo = Routes.decode(it.arguments?.getString("repo"))
                AppRunsScreen(
                    onBack = { navController.popBackStack() },
                    onOpenRun = { id -> navController.navigate(Routes.run(repo, id)) },
                )
            }
            composable(
                Routes.RUN,
                arguments = listOf(
                    navArgument("repo") { type = NavType.StringType },
                    navArgument("runId") { type = NavType.LongType },
                ),
            ) {
                RunScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

private fun NavHostController.goTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun navColors(selected: Boolean) = NavigationBarItemDefaults.colors(
    selectedIconColor = BurtonIvory,
    selectedTextColor = BurtonIvory,
    unselectedIconColor = BurtonMute,
    unselectedTextColor = BurtonMute,
    indicatorColor = Color(0xFF222222),
)
