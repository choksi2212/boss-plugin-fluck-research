package ai.rever.boss.plugin.dynamic.fluckresearch

import ai.rever.boss.plugin.api.Panel.Companion.bottom
import ai.rever.boss.plugin.api.Panel.Companion.right
import ai.rever.boss.plugin.api.PanelId
import ai.rever.boss.plugin.api.PanelInfo
import compose.icons.FeatherIcons
import compose.icons.feathericons.Bookmark

/**
 * Describes the Fluck Research panel: its id, sidebar icon, and default slot.
 *
 * Lives on the right sidebar (bottom slot) so it can sit beside a Fluck Browser
 * tab in the same split.
 */
object FluckResearchInfo : PanelInfo {
    override val id = PanelId("fluck-research", 16)
    override val displayName = "Research"
    override val icon = FeatherIcons.Bookmark
    override val defaultSlotPosition = right.bottom
}
