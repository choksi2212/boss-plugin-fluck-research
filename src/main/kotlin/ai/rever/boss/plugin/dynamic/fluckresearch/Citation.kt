package ai.rever.boss.plugin.dynamic.fluckresearch

import kotlinx.serialization.Serializable

/**
 * One captured reference.
 *
 * Persisted as JSON through [CitationStore]; ids are stable across runs so an
 * edit round-trips without duplicating the row. [capturedAtEpochMs] is the
 * wall-clock millisecond stamp taken at creation, used to sort the panel most
 * recent first.
 */
@Serializable
data class Citation(
    val id: String,
    val url: String,
    val title: String,
    val authorOrSite: String = "",
    val excerpt: String = "",
    val tags: List<String> = emptyList(),
    val note: String = "",
    val capturedAtEpochMs: Long,
) {
    val displayTitle: String get() = title.ifBlank { url }

    val tagText: String get() = tags.joinToString(", ") { it.trim() }.trim()
}
