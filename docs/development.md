# Development

## Tooling

- JDK **17**
- Android SDK compile/target **35**, min **26**
- Android Gradle Plugin 8.7.3, Kotlin 2.0.21, Compose BOM 2024.12.01
- Hilt 2.53.1 (KSP)
- OkHttp 4.12.0, Coil 2.7.0, Android Browser (Custom Tabs)

Point Gradle at the SDK with `local.properties` (`sdk.dir=…`). That file is gitignored.

## Commands

```bash
./gradlew :app:assembleDebug
./gradlew :app:installDebug
./gradlew testDebugUnitTest
```

Release assemble is blocked unless `keystore.properties` exists and `storeFile` points at a real keystore. Copy [`keystore.properties.example`](../keystore.properties.example) and keep `keystore.properties`, `*.jks`, and `*.keystore` out of git (see `.gitignore`). GitHub Actions signing is [build automation](build-automation.md).

Debug application id is `com.burton.builds.debug` so it can sit next to a signed install.

The emulator can run the UI. Connect with GitHub against a real account is the real test. Paste-token still works with a PAT.

## GitHub OAuth application

The Android client is not a GitHub OAuth app by itself. Register one under Burton-Workspaces with:

`https://burton-workspaces.github.io/burton-builds/oauth/`

[`web/oauth/index.html`](../web/oauth/index.html) is that page. It redirects into the app (`burtonbuilds://oauth`). GitHub Pages publishes `web/` from [`.github/workflows/pages.yml`](../.github/workflows/pages.yml). In the GitHub repo: **Settings → Pages → Source: GitHub Actions**.

Device flow is the primary sign-in and does not need that callback. PKCE still uses it as a fallback.

Put the public Client ID in [`github/client-id.txt`](../github/client-id.txt). Never commit a client secret.

Without a Client ID, **Use a token** still signs in.

## Layout

```
app/src/main/java/com/burton/builds/
  MainActivity.kt              sign-in gate, OAuth callback, tabs, nav
  data/github/                 GitHub REST, OAuth, token holder, GitHubTracker
  data/parse/                  TinyJson, GitHubCodec, Android catalog
  data/repository/             BuildsRepository, DataStore, installed packages
  domain/                      models
  ui/home, apps, failed, runs, run, settings, signin, components, theme
app/src/test/java/…            TinyJson, GitHubCodec, OAuth, catalog
github/                        public Client ID
web/oauth/                     HTTPS callback hop for GitHub Pages
```

Parser tests cover GitHub JSON. Run those before changing `GitHubCodec`.

Launcher PNGs (mipmaps + F-Droid `fdroid/metadata/com.burton.builds/en-US/icon.png`) come from `brand/ic_launcher.svg`:

```bash
python3 scripts/render-icons.py
```

## Network while debugging

HTTPS only. Avatars on `avatars.githubusercontent.com` load through Coil.

## Versioning while developing

Do not hand-edit `CHANGELOG.md` or `version.txt` on feature branches. Those are owned by [release-please](releases.md) from Conventional Commits on `master`.

Commit subjects must follow Conventional Commits. Install the hook once:

```bash
./scripts/install-git-hooks.sh
```

See [CONTRIBUTING.md](../CONTRIBUTING.md).

After a tagged SemVer release, publish the APK into the shared Burton Workspaces catalog:

```bash
./scripts/setup-fdroid-and-secrets.sh   # once
./scripts/publish-fdroid-pages.sh
```

That reads `version.txt`. Setup: [fdroid.md](fdroid.md).
