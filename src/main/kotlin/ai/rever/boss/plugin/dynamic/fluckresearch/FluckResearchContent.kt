package ai.rever.boss.plugin.dynamic.fluckresearch

import ai.rever.boss.plugin.ui.BossTheme
import ai.rever.boss.plugin.ui.BossThemeColors
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun FluckResearchContent(viewModel: FluckResearchViewModel) {
    BossTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colors.background,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Toolbar(viewModel)
                Divider(color = BossThemeColors.TextPrimary.copy(alpha = 0.08f))
                Messages(
                    statusMessage = viewModel.statusMessage.collectAsState().value,
                    errorMessage = viewModel.errorMessage.collectAsState().value,
                    onDismiss = { viewModel.clearMessages() },
                )
                FilterRow(
                    filter = viewModel.filter.collectAsState().value,
                    onFilterChange = viewModel::setFilter,
                )
                CitationListSection(viewModel)
                Footer(viewModel)
            }
        }
    }
}

@Composable
private fun Toolbar(viewModel: FluckResearchViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.surface)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Citations",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colors.onSurface,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "${viewModel.citations.collectAsState().value.size}",
            fontSize = 11.sp,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
        )
        Spacer(modifier = Modifier.weight(1f))
        IconButton(onClick = { viewModel.captureCurrentTab() }, modifier = Modifier.size(28.dp)) {
            Icon(
                imageVector = Icons.Default.BookmarkAdd,
                contentDescription = "Capture current page",
                modifier = Modifier.size(16.dp),
                tint = BossThemeColors.AccentColor,
            )
        }
        IconButton(
            onClick = {
                viewModel.copyBibTeXAll()
            },
            modifier = Modifier.size(28.dp),
        ) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Export BibTeX",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
            )
        }
        IconButton(onClick = { viewModel.refresh() }, modifier = Modifier.size(28.dp)) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Refresh",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun Messages(
    statusMessage: String?,
    errorMessage: String?,
    onDismiss: () -> Unit,
) {
    LaunchedEffect(statusMessage, errorMessage) {
        if (statusMessage != null || errorMessage != null) {
            delay(3500)
            onDismiss()
        }
    }
    val isError = errorMessage != null
    val message = errorMessage ?: statusMessage ?: return
    AnimatedVisibility(
        visible = true,
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isError) BossThemeColors.ErrorColor else BossThemeColors.SuccessColor)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (isError) Icons.Default.Error else Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = BossThemeColors.TextPrimary,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                fontSize = 11.sp,
                color = BossThemeColors.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDismiss, modifier = Modifier.size(20.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Dismiss",
                    modifier = Modifier.size(12.dp),
                    tint = BossThemeColors.TextPrimary.copy(alpha = 0.7f),
                )
            }
        }
    }
}

@Composable
private fun FilterRow(
    filter: String,
    onFilterChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colors.surface)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
        )
        Spacer(modifier = Modifier.width(6.dp))
        OutlinedTextField(
            value = filter,
            onValueChange = onFilterChange,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 36.dp),
            singleLine = true,
            placeholder = {
                Text(
                    "Filter by title, tag, or URL",
                    fontSize = 11.sp,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f),
                )
            },
            colors = TextFieldDefaults.outlinedTextFieldColors(
                textColor = MaterialTheme.colors.onSurface,
                backgroundColor = MaterialTheme.colors.background,
            ),
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
        )
        if (filter.isNotEmpty()) {
            IconButton(onClick = { onFilterChange("") }, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.Cancel,
                    contentDescription = "Clear filter",
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f),
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.CitationListSection(viewModel: FluckResearchViewModel) {
    val citations by viewModel.citations.collectAsState()
    val filter by viewModel.filter.collectAsState()
    val selected by viewModel.selectedIds.collectAsState()
    val visible = remember(citations, filter) { viewModel.filtered() }
    val listState = rememberLazyListState()
    var editing by remember { mutableStateOf<Citation?>(null) }
    var addingManual by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<String?>(null) }
    var exportPreview by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize().weight(1f)) {
        if (visible.isEmpty()) {
            EmptyState(filter.isNotBlank())
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
            ) {
                items(visible, key = { it.id }) { citation ->
                    CitationRow(
                        citation = citation,
                        expanded = false,
                        selected = citation.id in selected,
                        onToggleSelect = { viewModel.toggleSelected(citation.id) },
                        onEdit = { editing = citation },
                        onDelete = { confirmDelete = citation.id },
                    )
                }
            }
        }

        FloatingActions(
            showExport = selected.isNotEmpty(),
            onAdd = { addingManual = true },
            onExportSelected = {
                exportPreview = viewModel.exportBibTeX(selected)
            },
        )
    }

    editing?.let { c ->
        EditDialog(
            citation = c,
            onSave = { updated ->
                viewModel.update(updated)
                editing = null
            },
            onDelete = {
                viewModel.remove(c.id)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }

    if (addingManual) {
        ManualEntryDialog(
            onSave = { url, title ->
                viewModel.addManual(url, title)
                addingManual = false
            },
            onDismiss = { addingManual = false },
        )
    }

    confirmDelete?.let { id ->
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Remove citation?") },
            text = { Text("This permanently removes the citation from the panel and the JSON file.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.remove(id)
                    confirmDelete = null
                }) { Text("Remove", color = BossThemeColors.ErrorColor) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = null }) { Text("Cancel") }
            },
            backgroundColor = MaterialTheme.colors.surface,
        )
    }

    exportPreview?.let { text ->
        AlertDialog(
            onDismissRequest = { exportPreview = null },
            title = { Text("BibTeX export") },
            text = {
                SelectionContainer {
                    Text(
                        text = text,
                        fontSize = 11.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { exportPreview = null }) { Text("Close") }
            },
            backgroundColor = MaterialTheme.colors.surface,
        )
    }
}

@Composable
private fun EmptyState(isFiltered: Boolean) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (isFiltered) "No citations match the filter" else "No citations yet",
                fontSize = 13.sp,
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.6f),
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Click the bookmark icon to capture the current page",
                fontSize = 11.sp,
                color = MaterialTheme.colors.onBackground.copy(alpha = 0.4f),
            )
        }
    }
}

@Composable
private fun CitationRow(
    citation: Citation,
    expanded: Boolean,
    selected: Boolean,
    onToggleSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var localExpanded by remember { mutableStateOf(expanded) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (selected) MaterialTheme.colors.primary.copy(alpha = 0.06f)
                else MaterialTheme.colors.background,
            )
            .clickable { onToggleSelect() }
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SelectionDot(selected)
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = citation.displayTitle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colors.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = citation.url,
                    fontSize = 10.sp,
                    color = MaterialTheme.colors.onBackground.copy(alpha = 0.55f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(
                onClick = { localExpanded = !localExpanded },
                modifier = Modifier.size(24.dp),
            ) {
                Icon(
                    imageVector = if (localExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (localExpanded) "Collapse" else "Expand",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                )
            }
            IconButton(onClick = onEdit, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove",
                    modifier = Modifier.size(14.dp),
                    tint = BossThemeColors.ErrorColor.copy(alpha = 0.8f),
                )
            }
        }
        AnimatedVisibility(visible = localExpanded) {
            Column(modifier = Modifier.padding(start = 20.dp, end = 4.dp, top = 4.dp)) {
                if (citation.authorOrSite.isNotBlank()) {
                    Field("Author/Site", citation.authorOrSite)
                }
                if (citation.excerpt.isNotBlank()) {
                    Field("Excerpt", citation.excerpt, selectable = true)
                }
                if (citation.tagText.isNotBlank()) {
                    Field("Tags", citation.tagText)
                }
                if (citation.note.isNotBlank()) {
                    Field("Note", citation.note, selectable = true)
                }
                Field(
                    "Captured",
                    java.text.DateFormat
                        .getDateTimeInstance(java.text.DateFormat.MEDIUM, java.text.DateFormat.SHORT)
                        .format(java.util.Date(citation.capturedAtEpochMs)),
                )
            }
        }
    }
}

@Composable
private fun SelectionDot(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(14.dp)
            .clip(RoundedCornerShape(50))
            .background(
                if (selected) BossThemeColors.AccentColor
                else MaterialTheme.colors.onBackground.copy(alpha = 0.12f),
            ),
    )
}

@Composable
private fun Field(label: String, value: String, selectable: Boolean = false) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colors.onBackground.copy(alpha = 0.55f),
        )
        if (selectable) {
            SelectionContainer {
                Text(
                    text = value,
                    fontSize = 11.sp,
                    color = MaterialTheme.colors.onBackground,
                )
            }
        } else {
            Text(
                text = value,
                fontSize = 11.sp,
                color = MaterialTheme.colors.onBackground,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun FloatingActions(
    showExport: Boolean,
    onAdd: () -> Unit,
    onExportSelected: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (showExport) {
                Button(
                    onClick = onExportSelected,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = BossThemeColors.AccentColor,
                    ),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 10.dp,
                        vertical = 4.dp,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = BossThemeColors.TextPrimary,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("BibTeX", fontSize = 11.sp, color = BossThemeColors.TextPrimary)
                }
            }
            Button(
                onClick = onAdd,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = MaterialTheme.colors.surface,
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 10.dp,
                    vertical = 4.dp,
                ),
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colors.onSurface,
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Manual", fontSize = 11.sp, color = MaterialTheme.colors.onSurface)
            }
        }
    }
}

@Composable
private fun Footer(viewModel: FluckResearchViewModel) {
    val path = viewModel.dataPath.collectAsState().value
    if (path != null) {
        Text(
            text = path,
            fontSize = 9.sp,
            color = MaterialTheme.colors.onBackground.copy(alpha = 0.4f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colors.surface)
                .padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun EditDialog(
    citation: Citation,
    onSave: (Citation) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var url by remember { mutableStateOf(citation.url) }
    var title by remember { mutableStateOf(citation.title) }
    var author by remember { mutableStateOf(citation.authorOrSite) }
    var excerpt by remember { mutableStateOf(citation.excerpt) }
    var tags by remember { mutableStateOf(citation.tags.joinToString(", ")) }
    var note by remember { mutableStateOf(citation.note) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit citation") },
        text = {
            Column {
                LabeledField("URL", url) { url = it }
                LabeledField("Title", title) { title = it }
                LabeledField("Author / Site", author) { author = it }
                LabeledField("Excerpt", excerpt, singleLine = false) { excerpt = it }
                LabeledField("Tags (comma-separated)", tags) { tags = it }
                LabeledField("Note", note, singleLine = false) { note = it }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val tagList = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                onSave(
                    citation.copy(
                        url = url.trim(),
                        title = title.trim(),
                        authorOrSite = author.trim(),
                        excerpt = excerpt.trim(),
                        tags = tagList,
                        note = note.trim(),
                    ),
                )
            }) { Text("Save") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) {
                    Text("Remove", color = BossThemeColors.ErrorColor)
                }
                Spacer(modifier = Modifier.width(4.dp))
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
        backgroundColor = MaterialTheme.colors.surface,
    )
}

@Composable
private fun ManualEntryDialog(
    onSave: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var url by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add citation") },
        text = {
            Column {
                LabeledField("URL", url) { url = it }
                LabeledField("Title (optional)", title) { title = it }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(url, title) }) { Text("Add") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        backgroundColor = MaterialTheme.colors.surface,
    )
}

@Composable
private fun LabeledField(
    label: String,
    value: String,
    singleLine: Boolean = true,
    onValueChange: (String) -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 3.dp)) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colors.onBackground.copy(alpha = 0.65f),
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = singleLine,
            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp),
        )
    }
}
