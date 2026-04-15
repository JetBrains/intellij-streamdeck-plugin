# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a dual-plugin system enabling Elgato Stream Deck hardware to control JetBrains IDEs:

1. **`streamdeck-plugin/`** — TypeScript plugin running inside the Stream Deck desktop app
2. **`idea-plugin/`** — Kotlin/Java plugin running inside JetBrains IDEs

The Stream Deck plugin communicates with the IDE plugin via HTTP (`http://{host}:{port}/api/action/{actionId}`), passing an Authorization header (password) and optional `?name=` query param for run configurations.

## Stream Deck Plugin Commands

```bash
cd streamdeck-plugin

npm install          # Install dependencies
npm run build        # Full build: TypeScript compile + Browserify bundle
npm run tsc          # TypeScript compilation only (output: build/)
npm run browserify   # Bundle for browser (output: com.jetbrains.ide.sdPlugin/public/dist/)
npm run lint         # ESLint
npm run test         # Jest tests with coverage
npm run distribution # Package release zip (output: releases/)
```

**Live development:** `refreshPlugin.sh` auto-packages and deploys to the Stream Deck app.
**Debug URL:** `http://localhost:23654/`

## IDEA Plugin Commands

```bash
cd idea-plugin

./gradlew runIde         # Launch IDE sandbox with plugin loaded
./gradlew build          # Build plugin JAR
./gradlew publishPlugin  # Publish to JetBrains Marketplace
```

## Architecture

### Stream Deck Plugin

- **Entry point:** `src/idea-plugin.ts` — extends `StreamDeckPluginHandler`, registers all action instances
- **Base action:** `src/actions/default-action.ts` — abstract class; override `actionId()` to map a button to an IDE action ID. Handles `onKeyUp()` (HTTP call to IDE), `onContextAppear()`, `didReceiveSettings()`
- **Actions:** `src/actions/` — one file per button type. Most are thin subclasses that just return a hardcoded `actionId()`. `EmptyAction` is the generic "customized action" where the user provides the ID via property inspector
- **Utils:** `src/utils/` — HTTP fetch helpers for IDE communication
- **Property Inspector:** `src/idea-property-inspector.ts` + `com.jetbrains.ide.sdPlugin/public/property-inspector.html` — per-button settings UI (action ID, port, run config name) and global settings (host, default port, password)
- **Plugin manifest:** `com.jetbrains.ide.sdPlugin/manifest.json` — declares all action UUIDs and their icons

### IDEA Plugin

- **HTTP endpoint:** `service/StreamDeckHttpService.kt` — `RestService` that handles `/api/action/{actionId}`; validates password, delegates to `ActionExecutor`
- **Action execution:** `util/ActionExecutor.java` — resolves action ID from URI, handles run/debug config targeting, dispatches via IntelliJ's action system
- **Settings:** `settings/ActionServerSettings.kt` + `StreamDeckPreferenceComponent.java` — stores password, enabled state, focus-only mode, remote port (default 21420)
- **Action Browser:** `keymap/KeymapPanel.java` + `ActionsTree.java` — UI for browsing all 300+ IDE action IDs (Help > Open Action Browser)
- **Startup:** `BackendServiceLoader.kt` — `ApplicationInitializedListener` that starts the HTTP server

### Adding a New Action

1. Add a new `*Action.ts` in `streamdeck-plugin/src/actions/` extending `DefaultAction`, returning the IDE action ID from `actionId()`
2. Register it in `streamdeck-plugin/src/idea-plugin.ts`
3. Add the action entry to `com.jetbrains.ide.sdPlugin/manifest.json` with a matching UUID
4. Add icon assets under `com.jetbrains.ide.sdPlugin/`

### Port Discovery

The Stream Deck plugin tries ports 63342–63352 sequentially when no custom port is configured, then falls back to the explicit remote port (21420).
