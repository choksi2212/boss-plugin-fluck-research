# AGENTS.md

This file provides guidance to coding agents working on the `boss-plugin-fluck-research` repository.

## What this plugin is

A citation capture panel that pairs with BOSS Console's Fluck Browser. Captures the URL and title
of the active browser tab through `ActiveTabsProvider`, persists them to a single JSON file under
the user's Downloads directory, and exposes two MCP tools (`fluck_research_cite`,
`fluck_research_list`) so an in-terminal agent can record and query the same store.

## Build, test, and ship

```bash
./gradlew buildPluginJar            # produce build/libs/boss-plugin-fluck-research-0.1.0.jar
./gradlew clean buildPluginJar -x test --no-daemon   # CI-equivalent clean build
```

The jar is the only deliverable. The CI workflow in `.github/workflows/test.yml` mirrors the
release build, and `.github/workflows/build.yml` delegates to the BossConsole release workflow
once a tag is published.

`boss-plugin-api` is `compileOnly`: the host provides it at runtime. Locally the build expects it
at `../boss-plugin-api/build/libs/boss-plugin-api-1.0.93.jar`; CI downloads the matching release
jar into `build/downloaded-deps/` before compiling.

## Layout

```
boss-plugin-fluck-research/
├── build.gradle.kts                                    # build config + version (single source of truth)
├── settings.gradle.kts
├── gradle/ gradlew gradlew.bat                         # Gradle wrapper
├── .github/workflows/build.yml                         # release pipeline (delegates to BossConsole-Releases)
├── .github/workflows/test.yml                          # PR pipeline (downloads api jar, runs build)
└── src/main/
    ├── kotlin/ai/rever/boss/plugin/dynamic/fluckresearch/
    │   ├── FluckResearchDynamicPlugin.kt               # entry point (implements DynamicPlugin)
    │   ├── FluckResearchInfo.kt                        # PanelInfo (id, icon, slot)
    │   ├── FluckResearchComponent.kt                   # PanelComponentWithUI + @Composable Content()
    │   ├── FluckResearchViewModel.kt                   # state (StateFlow)
    │   ├── FluckResearchContent.kt                     # Compose UI
    │   ├── FluckResearchMcpTools.kt                    # MCP tools (cite + list)
    │   ├── Citation.kt                                 # @Serializable data class
    │   ├── CitationStore.kt                            # JSON persistence through FileSystemDataProvider
    │   └── BibTeX.kt                                   # BibTeX formatter
    └── resources/META-INF/boss-plugin/plugin.json      # the manifest
```

The `version` in `plugin.json` is **synced from `build.gradle.kts`** by `processResources` - do
not hand-edit it.

## Single source of truth for the version

`build.gradle.kts` `version = "0.1.0"`. Bumping it bumps the manifest and the jar name
(`boss-plugin-fluck-research-${version}.jar`) in one go.

## Manifest fields worth getting right

- `apiVersion` / `minApiVersion` / `minBossVersion` gate whether the host loads the plugin; see
  `boss-plugins/docs/versioning-and-compatibility.md` for the rules.
- `panel.position` is `"right_bottom"` so the panel can sit beside a Fluck Browser tab in the same
  split.
- `type` is `"panel"`. The plugin does not contribute tab types.

## Provider nullability

Every provider on `PluginContext` is nullable on hosts that do not wire that capability. Handle
each one:

- `fileSystemDataProvider` - if null, persistence is in-memory only and the panel shows a "data
  provider unavailable" message.
- `activeTabsProvider` - if null, the capture-from-active-tab button reports that no browser tab is
  active.
- `clipboardProvider` - if null, the BibTeX export button generates the string but cannot copy it.

There is no crash path. Each provider is captured in `FluckResearchDynamicPlugin.register` and
passed down to `FluckResearchComponent` and the `CitationStore`, and each layer null-checks before
using it.

## Persistence on disk

`CitationStore` writes the whole file on every mutation. The volume is low (a citation every few
minutes at worst) and the simpler path keeps two writers from racing on the same JSON document.
Reads tolerate missing files, blank files and partial / corrupt files - all three return an empty
list so the user can recover by deleting the file.

`FileSystemDataProvider.createFolder` is called before the first write; its failure is ignored
because the directory usually already exists.

## MCP tool design

The two MCP tools share the same `CitationStore` instance as the panel, so any capture made
through the MCP shows up in the panel and vice versa.

- `fluck_research_cite` is mutating (`readOnly = false`).
- `fluck_research_list` is read-only.
- Both follow the host's RBAC gate. There is no `requiredPermissions` set, so they are exposed to
  every signed-in user; the host's own approval flow is what stands between a model call and a
  write.

## BibTeX output

`BibTeX.render` emits one `@misc` entry per citation. The entry's identifier is the citation id
with non-alphanumeric characters collapsed to `_`. `urldate` is rendered as `YYYY-MM-DD` (UTC)
because BibTeX has no portable time format and a date alone is what every reference manager
expects. Empty fields are omitted so a citation the user has only captured renders as the minimum
row.

## Local development loop

1. Build the jar (`./gradlew buildPluginJar`).
2. Copy it to `~/.boss_debug/plugins/` (dev mode) rather than `~/.boss/plugins/` (production).
3. Clear the extracted cache so the new bytecode is picked up:
   ```bash
   rm -rf ~/.boss_debug/plugin-cache/ai.rever.boss.plugin.dynamic.fluckresearch
   ```
4. Restart the dev host.

The user runs and tests the host themselves; do not run `./gradlew run` in a blocking way.

## Style and review notes

- No em-dashes (U+2014) in any user-visible text - the host's release-notes guard flags them in
  Markdown and HTML, and the convention is held by review in code.
- All Kotlin files end with a newline.
- Do not add `Co-Authored-By` lines to commit messages, and do not mention AI assistance in
  commit messages, PR descriptions or source comments.
- The plugin ships with `BossTheme { ... }` wrapped around its content composables so it follows
  the host theme; paint with `BossThemeColors` tokens rather than `Color(...)` literals where one
  exists.
