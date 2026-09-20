package ai.rever.boss.plugin.dynamic.fluckresearch

import ai.rever.boss.plugin.api.McpToolArgs
import ai.rever.boss.plugin.api.McpToolDefinition
import ai.rever.boss.plugin.api.McpToolHandler
import ai.rever.boss.plugin.api.McpToolProvider
import ai.rever.boss.plugin.api.McpToolResult
import java.util.UUID

/**
 * MCP tools contributed by the Fluck Research plugin.
 *
 * Registered in [FluckResearchDynamicPlugin.register] via
 * `context.registerMcpToolProvider(...)`. The two tools share a single
 * [CitationStore] with the panel so any capture made through the MCP tools
 * appears in the panel and vice versa.
 *
 * `fluck_research_cite` is mutating (`readOnly = false`) because it writes
 * to disk. `fluck_research_list` is read-only.
 */
internal class FluckResearchMcpToolProvider(
    override val providerId: String,
    private val store: CitationStore,
) : McpToolProvider {

    override fun tools(): List<McpToolDefinition> = listOf(
        McpToolDefinition(
            name = "fluck_research_cite",
            description = "Capture a citation. Required: url. Optional: title, " +
                "authorOrSite, excerpt, tags (comma-separated), note. Returns the new " +
                "citation's id.",
            inputSchema = CITE_SCHEMA,
            readOnly = false,
            handler = McpToolHandler { args -> cite(args) },
        ),
        McpToolDefinition(
            name = "fluck_research_list",
            description = "List captured citations, most recent first. Optional " +
                "argument: query (substring matched against title, url, author, tags " +
                "and note). Returns one citation per line in a compact form.",
            inputSchema = LIST_SCHEMA,
            readOnly = true,
            handler = McpToolHandler { args -> list(args) },
        ),
    )

    private suspend fun cite(args: McpToolArgs): McpToolResult {
        val url = args.string("url")?.trim()
        if (url.isNullOrBlank()) {
            return McpToolResult("Missing required argument: url", isError = true)
        }
        val title = args.string("title").orEmpty().trim()
        val author = args.string("authorOrSite").orEmpty().trim()
        val excerpt = args.string("excerpt").orEmpty().trim()
        val note = args.string("note").orEmpty().trim()
        val tags = args.string("tags")
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()
        val existing = store.load()
        val citation = Citation(
            id = UUID.randomUUID().toString(),
            url = url,
            title = title,
            authorOrSite = author,
            excerpt = excerpt,
            tags = tags,
            note = note,
            capturedAtEpochMs = System.currentTimeMillis(),
        )
        val updated = (existing + citation).sortedByDescending { it.capturedAtEpochMs }
        val saved = store.save(updated)
        return if (saved) {
            McpToolResult("Captured: ${citation.displayTitle} (id: ${citation.id})")
        } else {
            McpToolResult(
                "Stored in memory but could not write to disk (file system " +
                    "provider unavailable).",
                isError = true,
            )
        }
    }

    private suspend fun list(args: McpToolArgs): McpToolResult {
        val query = args.string("query")?.trim()?.lowercase().orEmpty()
        val all = store.load().sortedByDescending { it.capturedAtEpochMs }
        val filtered = if (query.isBlank()) {
            all
        } else {
            all.filter { c ->
                c.title.lowercase().contains(query) ||
                    c.url.lowercase().contains(query) ||
                    c.authorOrSite.lowercase().contains(query) ||
                    c.tags.any { it.lowercase().contains(query) } ||
                    c.note.lowercase().contains(query)
            }
        }
        if (filtered.isEmpty()) {
            return McpToolResult(
                if (query.isBlank()) "No citations captured." else "No citations match \"$query\".",
            )
        }
        val body = filtered.joinToString("\n") { formatLine(it) }
        return McpToolResult("Captured: ${filtered.size}\n$body")
    }

    private fun formatLine(c: Citation): String {
        val tags = if (c.tags.isEmpty()) "" else " [${c.tags.joinToString(",")}]"
        val author = if (c.authorOrSite.isBlank()) "" else " - ${c.authorOrSite}"
        return "- ${c.displayTitle}$author${tags}\n  ${c.url}"
    }

    private companion object {
        const val CITE_SCHEMA = """
            {"type":"object","properties":{
              "url":{"type":"string","description":"The captured URL (required)."},
              "title":{"type":"string","description":"Page title."},
              "authorOrSite":{"type":"string","description":"Author or hosting site."},
              "excerpt":{"type":"string","description":"User-selected excerpt."},
              "tags":{"type":"string","description":"Comma-separated tags."},
              "note":{"type":"string","description":"Free-form note."}
            },"required":["url"]}
        """

        const val LIST_SCHEMA = """
            {"type":"object","properties":{
              "query":{"type":"string","description":"Optional substring filter."}
            }}
        """
    }
}
