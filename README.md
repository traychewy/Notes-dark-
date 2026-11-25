# Samsung Notes patches for Morphe

One patch: **Force dark mode** — Samsung Notes always opens in dark mode, whatever your system theme is.

## Add to Morphe
In Morphe Manager: **Sources → + → Remote**, then paste `https://github.com/traychewy/notes-dark-`.
Or download the `.mpp` from [Releases](https://github.com/traychewy/notes-dark-/releases) and add it under **Local**.

## Patches list

<!-- PATCHES_START -->
<!-- PATCHES_END -->

## How it works
- Reads the Application class from `AndroidManifest.xml` and injects a call at the start of its `onCreate()` (or adds an `onCreate()` if there isn't one).
- That call (`extensions/.../samsungnotes/ForceDarkModePatch.java`) uses `UiModeManager.setApplicationNightMode(MODE_NIGHT_YES)` (Android 12+), a per-app night mode. Only Samsung Notes is affected; the system theme is untouched.
- Fallback: `AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_YES)` via reflection, if that class isn't obfuscated.

## Build
Same as the Morphe patches template: set `gpr.user` / `gpr.key` (a GitHub token with `read:packages`) in `~/.gradle/gradle.properties`, then
```
./gradlew buildAndroid
```
The `.mpp` file lands in `patches/build/libs/`. Load it in Morphe Manager as a local patch source, or use Morphe CLI/Desktop.

Package names still use the template's `app.template.*` — rename if you publish it.
