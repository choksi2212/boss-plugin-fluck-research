# Fluck Research (BOSS plugin)

A citation capture panel for the [BOSS Console](https://github.com/risa-labs-inc/BossConsole) desktop
app. Captures the URL and title of the active browser tab with one click, lets you add tags and a
note, persists everything to a single JSON file, and exports the result as a BibTeX snippet you can
paste into a paper's bibliography.

## What it does, and how it pairs with Fluck Browser

BOSS already ships **Fluck Browser**, an embedded browser that lets you chat with a page or ask
follow-up questions about its content. Fluck Research does the other half of the reading workflow:

- a sidebar panel of references to pages you have actually opened,
- every capture stored with URL, title, optional author/site, excerpt, tags and a free-form note,
- a search box that filters by any of those fields,
- a copy-to-clipboard button that produces a `.bib`-compatible string,
- two MCP tools (`fluck_research_cite`, `fluck_research_list`) so an in-terminal agent can record
  the page it just read without leaving the terminal.

The panel lives in the right sidebar by default (`right.bottom`), so it can sit next to a Fluck
Browser tab in the same split.

## Install

This plugin is hosted on the public BOSS Plugin Store. In a BOSS Console window:

1. Open the Toolbox (`Ctrl+Shift+P` or the side panel).
2. Search for **Fluck Research** and install it.
3. The plugin enables itself and registers the **Research** panel; open it from the right sidebar.

To install from a local jar:

```bash
cp build/libs/boss-plugin-fluck-research-0.1.0.jar ~/.boss/plugins/
```

Then restart BOSS Console (plugins load at startup). To reload a development build, also remove the
extracted cache:

```bash
rm -rf ~/.boss/plugin-cache/ai.rever.boss.plugin.dynamic.fluckresearch
```

## Use

1. Open a Fluck Browser tab on the page you want to cite.
2. Click the **bookmark** icon in the panel's toolbar to capture its URL and title (the URL is
   derived from the active tab through `ActiveTabsProvider`).
3. Click a citation row to expand it; click **Edit** to add an excerpt, tags or a note.
4. Select multiple rows (click them), then press **BibTeX** in the lower-right of the panel to copy a
   `.bib` snippet to the clipboard.
5. Use the search box to narrow the list by title, tag, URL, author or note.

If the active tab is not a browser tab (no URL), the capture button reports "No browser tab with a
URL is active" - use **Manual** in the lower-right to paste a URL by hand.

## Where the data lives

Captured citations are stored in a single JSON file:

```
<Downloads>/fluck-research/citations.json
```

The path is resolved through `FileSystemDataProvider.getDownloadsDirectory()`, so it follows the
host's idea of the user's downloads directory (which is host-aware per OS). The full path is shown
at the bottom of the panel.

The file is plain JSON and safe to edit by hand; the next load reads whatever is there.

```json
[
  {
    "id": "f7c8…",
    "url": "https://example.com/article",
    "title": "An example article",
    "authorOrSite": "example.com",
    "excerpt": "The bit you want to come back to.",
    "tags": ["hci", "evaluation"],
    "note": "Re-read section 4.",
    "capturedAtEpochMs": 1765000000000
  }
]
```

## MCP tools

Two tools are exposed on the `boss` MCP server while the plugin is active. They share the same JSON
file as the panel, so an agent can `cite` and the result shows up in the sidebar next time the
panel composes.

### `fluck_research_cite`

Capture a citation. Required: `url`. Optional: `title`, `authorOrSite`, `excerpt`, `tags`
(comma-separated), `note`. Returns the new citation's id.

This tool is mutating (`readOnly = false`), so the host prompts the user to confirm unless they
have set an "always allow" policy for it.

### `fluck_research_list`

List captured citations, most recent first. Optional `query` argument; substring matched against
title, URL, author, tags and note. Read-only.

## BibTeX export format

Each citation becomes one `@misc` entry:

```bibtex
@misc{citation-id,
  url  = {https://example.com/article},
  title  = {An example article},
  howpublished  = {example.com},
  note  = {Re-read section 4.},
  keywords  = {hci, evaluation},
  urldate  = {2025-12-05}
}
```

`urldate` is rendered as `YYYY-MM-DD` (UTC). `howpublished` is filled from the
`authorOrSite` field so a citation with no author still has a recognizable source line. `url` is
always present; the rest are omitted when empty.

The `citation-id` is the same id as in the JSON file, with non-alphanumeric characters collapsed
to `_`, so it is safe to paste straight into LaTeX.

## Compatibility

- BOSS Console 9.4.2 or later (`minBossVersion` in the manifest).
- Plugin API 1.0.93 or later.
- The MCP tools require a host whose `McpToolProvider` is wired (the MCP bridge lives in the
  `terminal-tab` plugin).
- The capture-from-active-tab feature requires a host that supplies `ActiveTabsProvider`; the panel
  degrades gracefully when the provider is missing - the toolbar capture button simply reports that
  no browser tab is active.

## License

MIT.
