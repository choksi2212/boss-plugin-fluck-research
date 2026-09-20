package ai.rever.boss.plugin.dynamic.fluckresearch

import ai.rever.boss.plugin.api.ActiveTabData
import ai.rever.boss.plugin.api.ActiveTabsProvider
import ai.rever.boss.plugin.api.ClipboardProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * State and side effects for the Fluck Research panel.
 *
 * Holds the in-memory citation list (the source of truth while the panel is
 * alive), a free-text filter, and a transient status / error line. Persists
 * through [CitationStore] on every mutation so a crash does not lose work.
 *
 * [activeTabsProvider] is nullable on hosts that do not advertise active tabs;
 * "capture from active tab" is a no-op there rather than an error. The same
 * applies to [clipboardProvider] for the BibTeX copy.
 */
class FluckResearchViewModel(
    private val store: CitationStore,
    private val activeTabsProvider: ActiveTabsProvider?,
    private val clipboardProvider: ClipboardProvider?,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _citations = MutableStateFlow<List<Citation>>(emptyList())
    val citations: StateFlow<List<Citation>> = _citations.asStateFlow()

    private val _filter = MutableStateFlow("")
    val filter: StateFlow<String> = _filter.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _dataPath = MutableStateFlow<String?>(null)
    val dataPath: StateFlow<String?> = _dataPath.asStateFlow()

    private val _selectedIds = MutableStateFlow<Set<String>>(emptySet())
    val selectedIds: StateFlow<Set<String>> = _selectedIds.asStateFlow()

    init {
        _dataPath.value = store.resolvePath()
        refresh()
    }

    fun setFilter(text: String) {
        _filter.value = text
    }

    fun refresh() {
        scope.launch {
            try {
                val loaded = store.load()
                _citations.value = loaded.sortedByDescending { it.capturedAtEpochMs }
            } catch (e: Exception) {
                _errorMessage.value = "Failed to load citations: ${e.message ?: "unknown error"}"
            }
        }
    }

    fun add(citation: Citation) {
        scope.launch {
            val updated = (_citations.value + citation)
                .sortedByDescending { it.capturedAtEpochMs }
            _citations.value = updated
            if (store.save(updated)) {
                _statusMessage.value = "Captured: ${citation.displayTitle}"
            } else {
                _errorMessage.value = "Saved in memory but could not write to disk"
            }
        }
    }

    fun update(citation: Citation) {
        scope.launch {
            val updated = _citations.value
                .map { if (it.id == citation.id) citation else it }
                .sortedByDescending { it.capturedAtEpochMs }
            _citations.value = updated
            store.save(updated)
        }
    }

    fun remove(id: String) {
        scope.launch {
            val updated = _citations.value.filterNot { it.id == id }
            _citations.value = updated
            _selectedIds.value = _selectedIds.value - id
            store.save(updated)
            _statusMessage.value = "Removed citation"
        }
    }

    fun toggleSelected(id: String) {
        _selectedIds.value = if (id in _selectedIds.value) {
            _selectedIds.value - id
        } else {
            _selectedIds.value + id
        }
    }

    fun clearSelection() {
        _selectedIds.value = emptySet()
    }

    fun clearMessages() {
        _statusMessage.value = null
        _errorMessage.value = null
    }

    fun captureCurrentTab() {
        scope.launch {
            val seed = currentTabSeed() ?: run {
                _errorMessage.value = "No browser tab with a URL is active"
                return@launch
            }
            add(seed)
        }
    }

    fun addManual(url: String, title: String) {
        val trimmedUrl = url.trim()
        val trimmedTitle = title.trim()
        if (trimmedUrl.isBlank()) {
            _errorMessage.value = "URL is required"
            return
        }
        val citation = Citation(
            id = UUID.randomUUID().toString(),
            url = trimmedUrl,
            title = trimmedTitle,
            capturedAtEpochMs = System.currentTimeMillis(),
        )
        add(citation)
    }

    /**
     * Build a BibTeX document for the citations whose ids are in [ids]. If
     * [ids] is empty, exports all citations. Returns the rendered text and
     * copies it to the clipboard when a provider is wired up.
     */
    /**
     * Convenience: build BibTeX for every citation, copy to clipboard, and
     * surface a status message. Wired to the toolbar's main copy button.
     */
    fun copyBibTeXAll() {
        exportBibTeX(emptySet())
    }

    fun exportBibTeX(ids: Set<String> = emptySet()): String {
        val target = if (ids.isEmpty()) {
            _citations.value
        } else {
            _citations.value.filter { it.id in ids }
        }
        val bib = BibTeX.render(target)
        if (bib.isNotEmpty()) {
            clipboardProvider?.let { cp ->
                if (cp.setText(bib)) {
                    _statusMessage.value = if (ids.isEmpty()) {
                        "Copied BibTeX for ${target.size} citations"
                    } else {
                        "Copied BibTeX for ${target.size} selected citations"
                    }
                } else {
                    _errorMessage.value = "Clipboard unavailable"
                }
            }
        }
        return bib
    }

    fun filtered(): List<Citation> {
        val query = _filter.value.trim().lowercase()
        if (query.isBlank()) return _citations.value
        return _citations.value.filter { c ->
            c.title.lowercase().contains(query) ||
                c.url.lowercase().contains(query) ||
                c.authorOrSite.lowercase().contains(query) ||
                c.tags.any { it.lowercase().contains(query) } ||
                c.note.lowercase().contains(query)
        }
    }

    private suspend fun currentTabSeed(): Citation? {
        val provider = activeTabsProvider ?: return null
        runCatching { provider.refreshTabs() }
        val tabs: List<ActiveTabData> = provider.activeTabs.value
        if (tabs.isEmpty()) return null
        val activePanel = provider.activePanelId
        val candidates = if (activePanel != null) {
            tabs.filter { it.panelId == activePanel }
        } else {
            tabs
        }
        val withUrl = candidates.firstOrNull { !it.url.isNullOrBlank() }
            ?: tabs.firstOrNull { !it.url.isNullOrBlank() }
            ?: return null
        val url = withUrl.url ?: return null
        return Citation(
            id = UUID.randomUUID().toString(),
            url = url,
            title = withUrl.title,
            authorOrSite = deriveAuthor(withUrl, url),
            capturedAtEpochMs = System.currentTimeMillis(),
        )
    }

    private fun deriveAuthor(tab: ActiveTabData, url: String): String {
        val host = runCatching {
            val uri = java.net.URI(url)
            uri.host?.removePrefix("www.")
        }.getOrNull()
        return host.orEmpty()
    }
}
