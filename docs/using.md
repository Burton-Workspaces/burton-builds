# Using Burton Builds

Burton Builds is an account client. Sign in with **Connect with GitHub**. The token is stored on the phone. Workflow runs go through GitHub’s HTTPS Actions API.

## Connect

1. Tap **Connect with GitHub** and allow the Burton Builds application in the browser.
2. GitHub shows a short device code. The app displays the same code. Confirm it, then the token stays on the phone (DataStore).

The GitHub OAuth app is registered for org **Burton-Workspaces**. GitHub’s browser flow also accepts an **HTTPS** callback, so register:

`https://burton-workspaces.github.io/burton-builds/oauth/`

That Pages hop (`web/oauth/index.html`) opens `burtonbuilds://oauth` with the authorization code (PKCE). Someone has to create the OAuth app once, then put the public Client ID in `github/client-id.txt`. After that, every phone uses the same application. Never put a client secret in the APK.

Scopes: `public_repo workflow read:user read:org`. Sign out from Settings. That deletes the token from the phone.

### Use a token (optional)

On Connect, **Use a token** pastes a classic PAT (`public_repo` and `workflow`) or a fine-grained token that can read Actions on Burton-Workspaces. Prefer Connect with GitHub when the Client ID is set.

## Screens

### Connect

Shown when no token is stored. **Connect with GitHub** starts GitHub device login (and falls back to the HTTPS callback). Failed login stays on this screen with an error and retry. **Use a token** is a debug fallback.

### Inbox

Recent Actions runs across the apps you subscribe to. In-progress and failed runs sort first. Tap a row to open jobs. Refresh (circle) reloads. Settings (gear) holds the account, backend, sign out, and the app version.

If nothing is subscribed, the empty-state card is **Nothing subscribed yet** with **Choose apps**. If subscribed apps have no runs, the card is **Everything is up to date**.

### Apps

Public Android repositories under [Burton-Workspaces](https://github.com/Burton-Workspaces). The list is refreshed from GitHub (Kotlin/Java Android apps). Check the apps you want in Inbox. Installed packages on the phone are marked. Tap a row for that app’s run list.

### App runs

All / Failed / Running. Play starts a `workflow_dispatch` on the default branch for an active workflow.

### Run

Jobs, conclusion, branch, event. **Rerun** / **Rerun failed jobs** / **Cancel run**. Open on GitHub from the top-right.

### Failed

Failed and timed-out runs across subscribed apps. If none, the card is **Everything is up to date**.

## Permissions

| Android | Permission | Why |
| --- | --- |
| All | Internet | GitHub API |

No location or nearby-devices permission. Debug builds use application id `com.burton.builds.debug` and can sit next to a signed install.
