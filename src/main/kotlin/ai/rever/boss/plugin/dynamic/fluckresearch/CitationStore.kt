package ai.rever.boss.plugin.dynamic.fluckresearch

import ai.rever.boss.plugin.api.FileSystemDataProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * JSON file-backed persistence for [Citation]s.
 *
 * Stored under `FileSystemDataProvider.getDownloadsDirectory()` so the data is
 * reachable to the user without any host-specific path knowledge: every OS the
 * host supports has a Downloads folder and the provider resolves it. The
 * single file is `fluck-research/citations.json` under that root - one
 * subdirectory keeps it from being mistaken for the user's downloads.
 *
 * Reads tolerate a missing file (returns empty), a blank file (returns empty),
 * and a partial/corrupt file (returns empty - the user can recover by deleting
 * the file). Writes go through `withContext(IO)` and write the whole file
 * every time; the volume is low and the simpler path keeps two writers from
 * racing on the same JSON document.
 */
class CitationStore(
    private val fileSystem: FileSystemDataProvider?,
) {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        encodeDefaults = true
    }

    private val listSerializer = ListSerializer(Citation.serializer())

    /**
     * Resolve the absolute path to the citations file. Returns null when the
     * file system provider is unavailable so callers can surface a "data
     * provider unavailable" state rather than crash.
     */
    fun resolvePath(): String? {
        val fs = fileSystem ?: return null
        val downloads = runCatching { fs.getDownloadsDirectory() }.getOrNull() ?: return null
        val separator = pathSeparator()
        val trimmed = downloads.trimEnd('/', '\\')
        return "$trimmed${separator}fluck-research${separator}$FILE_NAME"
    }

    suspend fun load(): List<Citation> = withContext(Dispatchers.IO) {
        val fs = fileSystem ?: return@withContext emptyList()
        val path = resolvePath() ?: return@withContext emptyList()
        val raw = fs.readFile(path).getOrNull() ?: return@withContext emptyList()
        if (raw.isBlank()) return@withContext emptyList()
        val decoded: List<Citation> = runCatching {
            json.decodeFromString(listSerializer, raw)
        }.getOrDefault(emptyList())
        decoded
    }

    suspend fun save(citations: List<Citation>): Boolean = withContext(Dispatchers.IO) {
        val fs = fileSystem ?: return@withContext false
        val path = resolvePath() ?: return@withContext false
        val separator = pathSeparator()
        val parent = path.substringBeforeLast(separator)
        val parentParent = parent.substringBeforeLast(separator)
        val parentName = parent.substringAfterLast(separator)
        runCatching { fs.createFolder(parentParent, parentName) }
        val payload: String = json.encodeToString(listSerializer, citations)
        fs.writeFile(path, payload).isSuccess
    }

    private fun pathSeparator(): String =
        if (System.getProperty("os.name").lowercase().contains("win")) "\\" else "/"

    private companion object {
        const val FILE_NAME = "citations.json"
    }
}
