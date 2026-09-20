package ai.rever.boss.plugin.dynamic.fluckresearch

import java.net.URI
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * Formats a list of [Citation]s as a BibTeX `.bib` snippet.
 *
 * One `@misc` entry per citation. [citeKey] is built from the citation id with
 * any non-alphanumeric character collapsed to `_`, which keeps the field
 * identifier safe without any escaping. The `url`, `title`, `author`/`howpublished`,
 * `note`, `keywords` and `urldate` fields are written only when they have a
 * value, so a citation the user has only captured renders as the minimum row.
 *
 * `urldate` is rendered as `YYYY-MM-DD` (UTC) because BibTeX has no portable
 * time format and a date alone is what every reference manager expects.
 */
object BibTeX {

    private val DATE_FORMAT: DateTimeFormatter =
        DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneOffset.UTC)

    fun render(citations: List<Citation>): String {
        if (citations.isEmpty()) return ""
        return citations.joinToString(separator = "\n\n") { entry(it) }
    }

    private fun entry(citation: Citation): String {
        val fields = mutableListOf<String>()
        fields += "  url  = {${escape(citation.url)}}"
        if (citation.title.isNotBlank()) {
            fields += "  title  = {${escape(citation.title)}}"
        }
        if (citation.authorOrSite.isNotBlank()) {
            fields += "  howpublished  = {${escape(citation.authorOrSite)}}"
        }
        if (citation.note.isNotBlank()) {
            fields += "  note  = {${escape(citation.note)}}"
        }
        if (citation.tags.isNotEmpty()) {
            val keywords = citation.tags.joinToString(", ") { escape(it.trim()) }
            fields += "  keywords  = {$keywords}"
        }
        if (citation.capturedAtEpochMs > 0) {
            val date = DATE_FORMAT.format(Instant.ofEpochMilli(citation.capturedAtEpochMs))
            fields += "  urldate  = {$date}"
        }
        val key = citeKey(citation)
        return "@misc{$key,\n${fields.joinToString(",\n")}\n}"
    }

    private fun citeKey(citation: Citation): String {
        val raw = citation.id.ifBlank { fallbackKey(citation) }
        val cleaned = raw.map { ch ->
            if (ch.isLetterOrDigit()) ch else '_'
        }.joinToString("")
        return if (cleaned.isEmpty()) "fluck" else cleaned
    }

    private fun fallbackKey(citation: Citation): String {
        val host = runCatching {
            val uri = URI(citation.url)
            uri.host ?: citation.url
        }.getOrDefault(citation.url)
        return "fluck-${host.replace('.', '_')}"
    }

    private fun escape(value: String): String =
        value.replace("\\", "\\\\").replace("{", "\\{").replace("}", "\\}")
}
