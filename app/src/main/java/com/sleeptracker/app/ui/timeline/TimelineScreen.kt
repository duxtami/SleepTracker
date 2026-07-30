package com.sleeptracker.app.ui.timeline

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sleeptracker.app.data.datastore.START_DELAY_OPTIONS_MINUTES
import com.sleeptracker.app.data.model.Mood
import com.sleeptracker.app.data.model.SleepSession
import com.sleeptracker.app.ui.components.DateTimeFieldRow
import com.sleeptracker.app.ui.theme.CardShape
import com.sleeptracker.app.util.TimeUtils
import kotlinx.coroutines.launch
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TimelineScreen(viewModel: TimelineViewModel, onOpenDetails: (Long) -> Unit, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showAddSheet by remember { mutableStateOf(false) }
    var editingSession by remember { mutableStateOf<SleepSession?>(null) }
    val expandedMonths = remember { mutableStateOf(setOf<String>()) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }

    fun exitSelectionMode() {
        selectionMode = false
        selectedIds = emptySet()
    }

    fun deleteWithUndo(sessions: List<SleepSession>) {
        sessions.forEach { viewModel.deleteSession(it) }
        scope.launch {
            val label = if (sessions.size == 1) "Sleep deleted" else "${sessions.size} entries deleted"
            val result = snackbarHostState.showSnackbar(
                message = label,
                actionLabel = "UNDO",
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed) {
                sessions.forEach { viewModel.restoreSession(it) }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!selectionMode) {
                FloatingActionButton(onClick = { showAddSheet = true }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add sleep entry")
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
            if (selectionMode) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(onClick = { exitSelectionMode() }) {
                        Icon(Icons.Filled.Close, contentDescription = "Cancel selection")
                    }
                    Text(
                        "${selectedIds.size} selected",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(onClick = {
                        val toDelete = state.groups.flatMap { it.sessions }.filter { selectedIds.contains(it.id) }
                        deleteWithUndo(toDelete)
                        exitSelectionMode()
                    }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete selected")
                    }
                }
            } else {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::updateSearch,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    placeholder = { Text("Search notes, mood, tags") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.extraLarge
                )
            }

            if (state.groups.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (state.searchQuery.isBlank()) "No sleep sessions yet.\nTap + to log your first night."
                        else "No results for \"${state.searchQuery}\"",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 140.dp, top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    state.groups.forEach { group ->
                        val isExpanded = expandedMonths.value.contains(group.monthKey) || state.groups.size == 1
                        item(key = "header_${group.monthKey}") {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(group.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                IconButton(onClick = {
                                    expandedMonths.value = if (isExpanded) expandedMonths.value - group.monthKey else expandedMonths.value + group.monthKey
                                }) {
                                    Icon(if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null)
                                }
                            }
                        }
                        if (isExpanded) {
                            items(group.sessions, key = { it.id }) { session ->
                                SessionCard(
                                    session = session,
                                    selectionMode = selectionMode,
                                    isSelected = selectedIds.contains(session.id),
                                    onTap = {
                                        if (selectionMode) {
                                            selectedIds = if (selectedIds.contains(session.id)) selectedIds - session.id else selectedIds + session.id
                                            if (selectedIds.isEmpty()) selectionMode = false
                                        } else {
                                            onOpenDetails(session.id)
                                        }
                                    },
                                    onLongPress = {
                                        if (!selectionMode) {
                                            selectionMode = true
                                            selectedIds = setOf(session.id)
                                        }
                                    },
                                    onDelete = { deleteWithUndo(listOf(session)) },
                                    onEdit = { editingSession = session }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        SessionEditorSheet(
            initial = null,
            onDismiss = { showAddSheet = false },
            onSave = { start, end, mood, quality, notes, tags, delayUsed ->
                viewModel.addManualSession(start, end ?: start, mood, quality, notes, tags, delayUsed)
                showAddSheet = false
            }
        )
    }

    editingSession?.let { session ->
        SessionEditorSheet(
            initial = session,
            onDismiss = { editingSession = null },
            onSave = { start, end, mood, quality, notes, tags, delayUsed ->
                viewModel.updateSession(session, start, end, mood, quality, notes, tags, delayUsed)
                editingSession = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun SessionCard(
    session: SleepSession,
    selectionMode: Boolean,
    isSelected: Boolean,
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    if (selectionMode) {
        Card(
            shape = CardShape,
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
            ),
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onTap, onLongClick = onLongPress)
        ) {
            SessionCardContent(session = session, leadingSelectionIcon = isSelected)
        }
        return
    }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    true
                }
                SwipeToDismissBoxValue.StartToEnd -> {
                    onEdit()
                    false
                }
                SwipeToDismissBoxValue.Settled -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val isEditSide = dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isEditSide) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer,
                        CardShape
                    )
                    .padding(horizontal = 24.dp),
                contentAlignment = if (isEditSide) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                Icon(
                    if (isEditSide) Icons.Filled.Edit else Icons.Filled.Delete,
                    contentDescription = if (isEditSide) "Edit" else "Delete",
                    tint = if (isEditSide) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    ) {
        Card(
            shape = CardShape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onTap, onLongClick = onLongPress)
        ) {
            SessionCardContent(session = session, leadingSelectionIcon = null)
        }
    }
}

@Composable
private fun SessionCardContent(session: SleepSession, leadingSelectionIcon: Boolean?) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (leadingSelectionIcon != null) {
                    Icon(
                        if (leadingSelectionIcon) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (leadingSelectionIcon) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp).padding(end = 8.dp)
                    )
                }
                Text(
                    text = TimeUtils.formatDate(session.startEpochMillis),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            session.mood?.let { Text(it.emoji, style = MaterialTheme.typography.titleMedium) }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "${TimeUtils.formatTime(session.startEpochMillis)} → ${session.endEpochMillis?.let { TimeUtils.formatTime(it) } ?: "—"}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Duration",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
        Text(
            text = TimeUtils.formatDurationShort(session.durationMillis),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        session.qualityRating?.let { quality ->
            Row(modifier = Modifier.padding(top = 4.dp)) {
                repeat(5) { i ->
                    Icon(
                        if (i < quality) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        if (session.notes.isNotBlank()) {
            Text(
                text = session.notes,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        if (session.tags.isNotEmpty()) {
            Row(modifier = Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                session.tags.forEach { tag ->
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.shapes.small)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(tag, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SessionEditorSheet(
    initial: SleepSession?,
    onDismiss: () -> Unit,
    onSave: (start: Long, end: Long?, mood: Mood?, quality: Int?, notes: String, tags: List<String>, delayUsed: Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val now = System.currentTimeMillis()
    val zone = remember(initial) { initial?.zone ?: ZoneId.systemDefault() }
    var startMillis by remember(initial) { mutableStateOf(initial?.startEpochMillis ?: (now - 8 * 60 * 60 * 1000)) }
    var endMillis by remember(initial) { mutableStateOf(initial?.endEpochMillis ?: now) }
    var notes by remember(initial) { mutableStateOf(initial?.notes ?: "") }
    var tagsText by remember(initial) { mutableStateOf(initial?.tags?.joinToString(", ") ?: "") }
    var selectedMood by remember(initial) { mutableStateOf(initial?.mood) }
    var quality by remember(initial) { mutableStateOf(initial?.qualityRating) }
    var delayUsed by remember(initial) { mutableStateOf(initial?.startDelayMinutesUsed ?: 0) }
    var delayMenuExpanded by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(24.dp).padding(bottom = 24.dp)) {
            Text(
                text = if (initial == null) "Add sleep entry" else "Edit sleep entry",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))

            DateTimeFieldRow(label = "Start", epochMillis = startMillis, zoneId = zone, onChange = { startMillis = it })
            Spacer(modifier = Modifier.height(12.dp))
            DateTimeFieldRow(label = "End", epochMillis = endMillis, zoneId = zone, onChange = { endMillis = it })

            Spacer(modifier = Modifier.height(20.dp))
            Text("Mood", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Mood.entries.forEach { mood ->
                    val selected = mood == selectedMood
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(MaterialTheme.shapes.small)
                            .background(if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh)
                            .clickable { selectedMood = mood }
                            .padding(vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(mood.emoji, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text("Sleep quality", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                for (i in 1..5) {
                    val filled = (quality ?: 0) >= i
                    Icon(
                        imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.Star,
                        contentDescription = "Quality $i",
                        tint = if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.clickable { quality = i }.padding(4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Box {
                Column(modifier = Modifier.fillMaxWidth().clickable { delayMenuExpanded = true }) {
                    Text("Start delay used", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        if (delayUsed == 0) "Off" else "$delayUsed minutes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                DropdownMenu(expanded = delayMenuExpanded, onDismissRequest = { delayMenuExpanded = false }) {
                    START_DELAY_OPTIONS_MINUTES.forEach { minutes ->
                        DropdownMenuItem(
                            text = { Text(if (minutes == 0) "Off" else "$minutes minutes") },
                            onClick = { delayUsed = minutes; delayMenuExpanded = false }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = tagsText,
                onValueChange = { tagsText = it },
                label = { Text("Tags (comma separated)") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    val tags = tagsText.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    onSave(startMillis, endMillis, selectedMood, quality, notes, tags, delayUsed)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = endMillis > startMillis
            ) {
                Text("Save")
            }
        }
    }
}
