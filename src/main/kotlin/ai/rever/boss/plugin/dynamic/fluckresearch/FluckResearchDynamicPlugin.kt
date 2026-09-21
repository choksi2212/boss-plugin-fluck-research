package ai.rever.boss.plugin.dynamic.fluckresearch

import ai.rever.boss.plugin.api.DynamicPlugin
import ai.rever.boss.plugin.api.PluginContext

/**
 * Fluck Research dynamic plugin - Loaded from external JAR.
 *
 * A citation capture companion to Fluck Browser. The panel reads URLs and
 * titles from the active tab through [ai.rever.boss.plugin.api.ActiveTabsProvider]
 * and persists citations to a single JSON file under the user's Downloads
 * directory. Two MCP tools (`fluck_research_cite`, `fluck_research_list`) let
 * an in-terminal agent capture and query the same store.
 */
class FluckResearchDynamicPlugin : DynamicPlugin {
    override val pluginId: String = "ai.rever.boss.plugin.dynamic.fluckresearch"
    override val displayName: String = "Fluck Research"
    override val version: String = "0.1.0"
    override val description: String = "Citation capture panel that complements Fluck Browser"
    override val author: String = "choksi2212"
    override val url: String = "https://github.com/choksi2212/boss-plugin-fluck-research"

    override fun register(context: PluginContext) {
        val fileSystem = context.fileSystemDataProvider
        val activeTabs = context.activeTabsProvider
        val clipboard = context.clipboardProvider

        context.panelRegistry.registerPanel(FluckResearchInfo) { ctx, panelInfo ->
            FluckResearchComponent(
                ctx = ctx,
                panelInfo = panelInfo,
                fileSystem = fileSystem,
                activeTabsProvider = activeTabs,
                clipboardProvider = clipboard,
            )
        }

        context.registerMcpToolProvider(
            FluckResearchMcpToolProvider(
                providerId = pluginId,
                store = CitationStore(fileSystem),
            ),
        )
    }

    override fun dispose() {
        // No long-lived resources; providers are released by the host on unload.
    }
}
