package ai.rever.boss.plugin.dynamic.fluckresearch

import ai.rever.boss.plugin.api.ActiveTabsProvider
import ai.rever.boss.plugin.api.ClipboardProvider
import ai.rever.boss.plugin.api.FileSystemDataProvider
import ai.rever.boss.plugin.api.PanelComponentWithUI
import ai.rever.boss.plugin.api.PanelInfo
import androidx.compose.runtime.Composable
import com.arkivanov.decompose.ComponentContext

/**
 * The live Fluck Research panel. [Content] draws the UI; wrap it in [BossTheme]
 * so it follows the active host theme, and paint with [BossThemeColors] tokens.
 *
 * Hosts pull providers off `PluginContext` and forward them - every provider
 * is nullable, so this class accepts each as nullable and surfaces a fallback
 * state in the content rather than crashing.
 */
class FluckResearchComponent(
    ctx: ComponentContext,
    override val panelInfo: PanelInfo,
    fileSystem: FileSystemDataProvider?,
    activeTabsProvider: ActiveTabsProvider?,
    clipboardProvider: ClipboardProvider?,
) : PanelComponentWithUI, ComponentContext by ctx {

    private val viewModel = FluckResearchViewModel(
        store = CitationStore(fileSystem),
        activeTabsProvider = activeTabsProvider,
        clipboardProvider = clipboardProvider,
    )

    @Composable
    override fun Content() {
        FluckResearchContent(viewModel)
    }
}
