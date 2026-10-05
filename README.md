# Burton Builds

A GitHub Actions client for Burton Workspaces Android apps, with the same look as the other Burton apps. Sign in with **Connect with GitHub**, pick public Android apps in [Burton-Workspaces](https://github.com/Burton-Workspaces), and watch, rerun, cancel, or dispatch workflow runs.

Signed APKs are published on [GitHub Releases](https://github.com/Burton-Workspaces/burton-builds/releases). Droidify / F-Droid: [burton-app-dist](https://github.com/Burton-Workspaces/burton-app-dist) (`https://burton-workspaces.github.io/burton-app-dist/fdroid/repo`).

## What it does

- **Inbox** — recent Actions runs across the apps you subscribe to
- **Apps** — public Android repos in Burton-Workspaces; select which to track
- **Failed** — failed and timed-out runs
- **Run** — jobs, rerun, rerun failed, cancel, open on GitHub
- **Dispatch** — start a `workflow_dispatch` on an app’s default branch
- **Settings** — account, GitHub backend, sign out, app version

The token stays on the phone (DataStore). The app talks to GitHub over HTTPS; there is no Burton cloud account.

## Requirements

- Android 8.0+ (API 26)
- Internet
- A GitHub account you can authorize (see [Using the app](docs/using.md))

## Docs

| Doc | Contents |
| --- | --- |
| [Using the app](docs/using.md) | Connect with GitHub, screens |
| [Architecture](docs/architecture.md) | Packages, GitHub Actions API |
| [Development](docs/development.md) | Build, run, test, project layout |
| [Build automation](docs/build-automation.md) | GitHub Actions, workflow permissions, signing secrets |
| [Releases](docs/releases.md) | SemVer 2.0, local build + publish walkthrough, GitHub Releases |
| [F-Droid / Droidify](docs/fdroid.md) | Same catalog as Burton Sonos, Fingerprint, setup + Pages publish |
| [Contributing](CONTRIBUTING.md) | Conventional Commits (required) |

## Quick start (debug)

```bash
./gradlew :app:installDebug
```

Debug builds use application id `com.burton.builds.debug`. Release builds need a keystore; see [docs/releases.md](docs/releases.md).

```bash
./gradlew testDebugUnitTest
```

One-time F-Droid and GitHub signing setup:

```bash
./scripts/setup-fdroid-and-secrets.sh
```

## License and scope

This is a household build board. It does not replace github.com for org administration, environments, or required reviewers.
