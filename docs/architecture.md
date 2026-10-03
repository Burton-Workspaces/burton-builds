# Architecture

The app is a single Gradle module (`:app`), Kotlin, Jetpack Compose, Hilt, OkHttp, Coil, and DataStore. UI collects `BuildsRepository` state.

```
ui/          Compose screens and ViewModels (Hilt)
domain/      Account, TrackedApp, WorkflowRun, WorkflowJob, Workflow
data/
  github     GitHubApi, GitHubTracker, OAuth (device + PKCE), access token
  parse      TinyJson, GitHubCodec, GradleIds / Android catalog
  repository BuildsRepository, LocalPrefs (DataStore), InstalledApps
di/          OkHttp, Coil ImageLoader
```

## Auth

**Connect with GitHub** starts the [device flow](https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/authorizing-oauth-apps#device-flow) (`login/device/code` + poll `login/oauth/access_token`). The user confirms a short code in a Custom Tab. If device login cannot start, the app falls back to authorization-code + PKCE. GitHub requires an HTTPS callback for that path, so it returns to `https://burton-workspaces.github.io/burton-builds/oauth/?code=…`. That static page (`web/oauth/index.html`) hops to `burtonbuilds://oauth`. There is no client secret in the APK. Scopes: `public_repo workflow read:user read:org`. Tokens live in DataStore (`burton_builds`) as `user_token`. Paste-token sign-in stores the same field.

`users` (`GET /user`) fills the account snapshot. Screens never see the token string after sign-in; the repository holds it in memory and DataStore. OkHttp adds `Authorization: Bearer` for `api.github.com`.

## Catalog

`GET /orgs/Burton-Workspaces/repos?type=public` lists public repos. `AndroidCatalog` keeps Kotlin/Java Android apps and drops sites, F-Droid trees, and `rabun` / `rgit` prefixes. `applicationId` is read from `app/build.gradle.kts` (contents API). Installed `com.burton.*` packages are matched through PackageManager (launcher queries). Subscriptions are a newline-separated repo list in DataStore; first launch subscribes to the full catalog.

## Builds

`GitHubApi` GETs and POSTs JSON to `https://api.github.com/{path}` with `Accept: application/vnd.github+json`. Responses are TinyJson maps. GitHubCodec maps those onto domain models.

| Path | Use |
| --- | --- |
| `user` | Signed-in account |
| `orgs/Burton-Workspaces/repos` | Public catalog |
| `repos/{repo}/actions/runs` | Inbox and app run lists |
| `repos/{repo}/actions/runs/{id}` | Run detail |
| `repos/{repo}/actions/runs/{id}/jobs` | Jobs |
| `repos/{repo}/actions/runs/{id}/rerun` | Rerun |
| `repos/{repo}/actions/runs/{id}/rerun-failed-jobs` | Rerun failed |
| `repos/{repo}/actions/runs/{id}/cancel` | Cancel |
| `repos/{repo}/actions/workflows` | Dispatch picker |
| `repos/{repo}/actions/workflows/{id}/dispatches` | `workflow_dispatch` |

Inbox loads recent runs per subscribed repo in parallel, then sorts in-progress, failed, then the rest by `updated_at`.

## UI shell

`MainActivity` hosts a `NavHost`. **Inbox**, **Apps**, and **Failed** are bottom tabs. App run lists and run detail are stacked routes and hide the tab bar. Settings is a full-screen modal. Sign-in is a gate when no token is stored.

Theme tokens match Burton Sonos: black surfaces, ivory text, sand accent, danger `#C45C4A`. Empty lists use the Burton empty-state card (`EmptyStatePanel`).
