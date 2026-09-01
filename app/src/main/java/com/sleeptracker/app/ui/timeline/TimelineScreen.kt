package com.sleeptracker.app.ui.timeline

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.sleeptracker.app.data.datastore.START_DELAY_OPTIONS_MINUTES
import com.sleeptracker.app.data.model.Mood
import com.sleeptracker.app.data.model.SleepSession
import com.sleeptracker.app.ui.components.DateTimeFieldRow
import com.sleeptracker.app.ui.components.ExpressiveSnackbarHost
import com.sleeptracker.app.ui.navigation.LocalBottomBarSpace
import com.sleeptracker.app.ui.navigation.LocalNavBarWidth

import com.sleeptracker.app.util.BedtimeDetector
import com.sleeptracker.app.util.TimeUtils
import kotlinx.coroutines.launch
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TimelineScreen(
    viewModel: TimelineViewModel,
    onOpenDetails: (Long) -> Unit,
    modifier: Modifier = Modifier,
    addRequestTrigger: Int = 0
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showAddSheet by remember { mutableStateOf(false) }
    var editingSession by remember { mutableStateOf<SleepSession?>(null) }
    val expandedMonths = remember { mutableStateOf(setOf(TimeUtils.monthKey(System.currentTimeMillis()))) }
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }

    // Lets the floating nav bar's trailing "+" button (only shown while this tab is active) open
    // the same Add sheet as the FAB below, without this screen needing to know anything about
    // the nav bar itself. Guarded so the initial default value of 0 never opens the sheet on
    // first composition - only a genuine increment from outside does.
    var lastHandledAddTrigger by remember { mutableStateOf(addRequestTrigger) }
    LaunchedEffect(addRequestTrigger) {
        if (addRequestTrigger != lastHandledAddTrigger) {
            lastHandledAddTrigger = addRequestTrigger
            showAddSheet = true
        }
    }

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
            } else {
                sessions.forEach { viewModel.confirmPermanentDelete(it.id) }
            }
        }
    }

    val bottomBarSpace = LocalBottomBarSpace.current
    val navBarWidth = LocalNavBarWidth.current

    // The FAB and Snackbar are deliberately placed here, in this Box, rather than through
    // Scaffold's own `floatingActionButton`/`snackbarHost` slots. Scaffold has real built-in
    // logic that automatically raises a Snackbar above a FAB it's hosting - but that logic
    // measures the FAB slot's own placed size, and a `Modifier.padding(bottom = bottomBarSpace)`
    // on the FAB (needed so it clears FloatingNavBar, which sits outside every screen's
    // Scaffold entirely) inflates that measured size by the same amount. The two clearances
    // then stack on top of each other - Scaffold's automatic FAB-avoidance offset PLUS this
    // screen's own manual nav-bar clearance - and the Snackbar ends up floating far higher than
    // intended. Placing both directly in this Box instead means their spacing above the nav bar
    // is set exactly once, in exactly one place, with nothing implicit stacking on top of it.
    Box(modifier = modifier.fillMaxSize()) {
        Scaffold { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(top = padding.calculateTopPadding())) {
            var searchExpanded by remember { mutableStateOf(false) }
            val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
            val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }

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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .height(64.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = !searchExpanded,
                        enter = fadeIn() + androidx.compose.animation.expandHorizontally(expandFrom = Alignment.Start),
                        exit = fadeOut() + androidx.compose.animation.shrinkHorizontally(shrinkTowards = Alignment.Start)
                    ) {
                        Text(
                            "History",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (!searchExpanded) {
                        IconButton(onClick = { searchExpanded = true }) {
                            Icon(Icons.Filled.Search, contentDescription = "Search")
                        }
                    }

                    androidx.compose.animation.AnimatedVisibility(
                        visible = searchExpanded,
                        enter = fadeIn() + androidx.compose.animation.expandHorizontally(expandFrom = Alignment.End),
                        exit = fadeOut() + androidx.compose.animation.shrinkHorizontally(shrinkTowards = Alignment.End),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LaunchedEffect(searchExpanded) {
                            if (searchExpanded) focusRequester.requestFocus()
                        }
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = viewModel::updateSearch,
                            modifier = Modifier
                                .fillMaxWidth()
                                .androidx.compose.ui.focus.focusRequester(focusRequester)
                                .androidx.compose.ui.focus.onFocusChanged { focusState ->
                                    if (!focusState.isFocused && state.searchQuery.isEmpty()) {
                                        searchExpanded = false
                                    }
                                },
                            placeholder = { Text("Search notes, mood, tags") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            trailingIcon = {
                                IconButton(onClick = {
                                    if (state.searchQuery.isNotEmpty()) {
                                        viewModel.updateSearch("")
                                    } else {
                                        focusManager.clearFocus()
                                        searchExpanded = false
                                    }
                                }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear search")
                                }
                            },
                            singleLine = true,
                            shape = MaterialTheme.shapes.extraLarge
                        )
                    }
                }
            }

            if (state.groups.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(bottom = bottomBarSpace),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (state.searchQuery.isBlank()) "No sleep sessions yet.\nTap + to log your first night."
                        else "No results for \"${state.searchQuery}\"",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = bottomBarSpace + 24.dp, top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    state.groups.forEach { group ->
                        val isExpanded = expandedMonths.value.contains(group.monthKey) || state.groups.size == 1
                        item(key = "header_${group.monthKey}") {
                            val rotation by animateFloatAsState(
                                targetValue = if (isExpanded) 180f else 0f,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
                                label = "monthChevronRotation"
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(MaterialTheme.shapes.large)
                                    .clickable(
                                        onClickLabel = if (isExpanded) "Collapse ${group.label}" else "Expand ${group.label}"
                                    ) {
                                        expandedMonths.value = if (isExpanded) expandedMonths.value - group.monthKey else expandedMonths.value + group.monthKey
                                    }
                                    .heightIn(min = 48.dp)
                                    .padding(horizontal = 4.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(group.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Icon(
                                    Icons.Filled.ExpandMore,
                                    contentDescription = null,
                                    modifier = Modifier.graphicsLayer { rotationZ = rotation }
                                )
                            }
                        }
                        if (isExpanded) {
                            items(group.sessions, key = { it.id }) { session ->
                                SessionCard(
                                    session = session,
                                    selectionMode = selectionMode,
                                    isSelected = selectedIds.contains(session.id),
                                    modifier = Modifier.animateItem(
                                        placementSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                                    ),
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

        // The floating nav bar's own trailing "+" button now covers this (see FloatingNavBar's
        // NavTrailingAction.ADD_ENTRY) whenever this tab is active, so this screen no longer
        // renders its own separate FAB for the same action - having both showed two overlapping
        // "+" buttons at once.

        // ExpressiveSnackbar is now a compact, content-hugging pill (not a full-width bar), so
        // at the same low height as where the FAB used to sit, it naturally clears the nav bar
        // for any realistically short message without needing an asymmetric width/end-padding
        // workaround.
        ExpressiveSnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomBarSpace + 8.dp)
                .width(navBarWidth)
        )
    }

    if (showAddSheet) {
        SessionEditorSheet(
            initial = null,
            settings = state.settings,
            onDismiss = { showAddSheet = false },
            onSave = { start, end, mood, quality, notes, tags, delayUsed, totalPausedMillis ->
                viewModel.addManualSession(start, end ?: start, mood, quality, notes, tags, delayUsed, totalPausedMillis)
                showAddSheet = false
            }
        )
    }

    editingSession?.let { session ->
        SessionEditorSheet(
            initial = session,
            settings = state.settings,
            onDismiss = { editingSession = null },
            onSave = { start, end, mood, quality, notes, tags, delayUsed, totalPausedMillis ->
                viewModel.updateSession(session, start, end, mood, quality, notes, tags, delayUsed, totalPausedMillis)
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
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (selectionMode) {
        Card(
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer
            ),
            modifier = modifier
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
        modifier = modifier,
        backgroundContent = {
            val isEditSide = dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (isEditSide) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.errorContainer,
                        MaterialTheme.shapes.extraLarge
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
            shape = MaterialTheme.shapes.extraLarge,
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = TimeUtils.formatDurationShort(session.durationMillis),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        if (session.totalPausedMillis > 0) {
            Text(
                text = "Includes ${TimeUtils.formatDurationShort(session.totalPausedMillis)} awake",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
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

/** Status line shown under the "Detect longest screen-off time" button: what was found and where
 *  it came from, or a plain heads-up that nothing could be detected this time. */
@Composable
private fun DetectionCaption(detection: BedtimeDetector.Result?, zone: ZoneId) {
    when (detection) {
        is BedtimeDetector.Result.Detected -> {
            val durationLabel = TimeUtils.formatDurationShort(detection.endEpochMillis - detection.startEpochMillis)
            val sourceLabel = when (detection.source) {
                BedtimeDetector.Source.SCREEN_EVENTS -> "your screen's longest continuous off period"
                BedtimeDetector.Source.APP_INACTIVITY_GAP -> "your longest recent stretch without phone activity"
            }
            Text(
                "Set Start to ${TimeUtils.formatTime(detection.startEpochMillis, zone.id)} and End to " +
                    "${TimeUtils.formatTime(detection.endEpochMillis, zone.id)} ($durationLabel), based on $sourceLabel",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        BedtimeDetector.Result.Unavailable -> {
            Text(
                "Couldn't detect a sleep period automatically this time - set Start and End manually instead",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        BedtimeDetector.Result.PermissionRequired, null -> Unit
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SessionEditorSheet(
    initial: SleepSession?,
    settings: com.sleeptracker.app.data.datastore.AppSettings,
    onDismiss: () -> Unit,
    onSave: (start: Long, end: Long?, mood: Mood?, quality: Int?, notes: String, tags: List<String>, delayUsed: Int, totalPausedMillis: Long) -> Unit
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
    var totalPausedMillis by remember(initial) { mutableStateOf(initial?.totalPausedMillis ?: 0L) }
    var delayMenuExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    var sleepDetection by remember(initial) { mutableStateOf<BedtimeDetector.Result?>(null) }
    var showUsageAccessDialog by remember { mutableStateOf(false) }
    var showRecentPeriodsDialog by remember { mutableStateOf(false) }
    var smartAnalyzeEnabled by remember { mutableStateOf(true) }
    var recentPeriods by remember(initial) { mutableStateOf<List<BedtimeDetector.ScreenOffPeriod>>(emptyList()) }
    // Set right before deep-linking out to the system's Usage Access settings screen, so that
    // when the user comes back, detection can retry automatically once instead of making them
    // tap "Detect" a second time.
    var awaitingUsageAccessGrant by remember { mutableStateOf(false) }

    fun runSleepDetection() {
        val result = BedtimeDetector.detectLongestScreenOffPeriod(context, settings, smartAnalyzeEnabled)
        sleepDetection = result
        when (result) {
            is BedtimeDetector.Result.Detected -> {
                startMillis = result.startEpochMillis
                endMillis = result.endEpochMillis
                totalPausedMillis = result.totalPausedMillis
            }
            BedtimeDetector.Result.PermissionRequired -> {
                awaitingUsageAccessGrant = true
                showUsageAccessDialog = true
            }
            BedtimeDetector.Result.Unavailable -> Unit
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && awaitingUsageAccessGrant) {
                awaitingUsageAccessGrant = false
                if (BedtimeDetector.hasUsageAccess(context)) {
                    runSleepDetection()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.padding(24.dp).padding(bottom = 24.dp)) {
            Text(
                text = if (initial == null) "Add sleep entry" else "Edit sleep entry",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))

            DateTimeFieldRow(
                label = "Start",
                epochMillis = startMillis,
                zoneId = zone,
                onChange = { startMillis = it }
            )
            Spacer(modifier = Modifier.height(12.dp))
            DateTimeFieldRow(
                label = "End",
                epochMillis = endMillis,
                zoneId = zone,
                onChange = { endMillis = it }
            )

            // Only offered for brand-new entries - inferring "when did you sleep" from recent
            // screen activity only makes sense relative to *now*, not when backfilling or
            // correcting an entry from another night.
            if (initial == null) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    FilledTonalButton(
                        onClick = {
                            if (BedtimeDetector.hasUsageAccess(context)) {
                                runSleepDetection()
                            } else {
                                awaitingUsageAccessGrant = true
                                showUsageAccessDialog = true
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.Bedtime, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Detect longest screen-off time")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    // Small, secondary escape hatch next to the main detect action: lets the
                    // user pick manually from every long screen-off stretch in the last 24h,
                    // in case the single "longest" guess above picked the wrong one (e.g. an
                    // evening away from the phone rather than actual sleep).
                    TextButton(
                        onClick = {
                            if (BedtimeDetector.hasUsageAccess(context)) {
                                recentPeriods = BedtimeDetector.findRecentScreenOffPeriods(context, settings, smartAnalyzeEnabled)
                                showRecentPeriodsDialog = true
                            } else {
                                awaitingUsageAccessGrant = true
                                showUsageAccessDialog = true
                            }
                        }
                    ) {
                        Text("View recent")
                    }
                }
                DetectionCaption(detection = sleepDetection, zone = zone)
            }

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
                            .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
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
                    onSave(startMillis, endMillis, selectedMood, quality, notes, tags, delayUsed, totalPausedMillis)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = endMillis > startMillis
            ) {
                Text("Save")
            }
        }
    }

    if (showUsageAccessDialog) {
        var backProgress by remember { mutableStateOf(0f) }
        androidx.activity.compose.PredictiveBackHandler { progress ->
            try {
                progress.collect { backProgress = it.progress }
                showUsageAccessDialog = false
                awaitingUsageAccessGrant = false
            } catch (e: java.util.concurrent.CancellationException) {
                backProgress = 0f
            }
        }
        AlertDialog(
            modifier = Modifier.graphicsLayer {
                val scale = 1f - (0.1f * backProgress)
                scaleX = scale
                scaleY = scale
                alpha = 1f - (0.5f * backProgress)
            },
            onDismissRequest = {
                showUsageAccessDialog = false
                awaitingUsageAccessGrant = false
            },
            icon = { Icon(Icons.Filled.Bedtime, contentDescription = null) },
            title = { Text("Allow usage access?") },
            text = {
                Text(
                    "SleepTracker can estimate your sleep period from the longest stretch your " +
                        "screen stayed off. That needs Usage Access, which Android only lets " +
                        "you grant from Settings - SleepTracker only reads the timing of that, " +
                        "never which apps you used, and nothing leaves your device."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showUsageAccessDialog = false
                    BedtimeDetector.openUsageAccessSettings(context)
                }) { Text("Open Settings") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showUsageAccessDialog = false
                    awaitingUsageAccessGrant = false
                }) { Text("Not now") }
            }
        )
    }

    if (showRecentPeriodsDialog) {
        var backProgress by remember { mutableStateOf(0f) }
        androidx.activity.compose.PredictiveBackHandler { progress ->
            try {
                progress.collect { backProgress = it.progress }
                showRecentPeriodsDialog = false
            } catch (e: java.util.concurrent.CancellationException) {
                backProgress = 0f
            }
        }
        AlertDialog(
            modifier = Modifier.graphicsLayer {
                val scale = 1f - (0.1f * backProgress)
                scaleX = scale
                scaleY = scale
                alpha = 1f - (0.5f * backProgress)
            },
            onDismissRequest = { showRecentPeriodsDialog = false },
            icon = { Icon(Icons.Filled.Bedtime, contentDescription = null) },
            title = { Text("Recent screen-off periods") },
            text = {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth().clickable {
                            smartAnalyzeEnabled = !smartAnalyzeEnabled
                            recentPeriods = BedtimeDetector.findRecentScreenOffPeriods(context, settings, smartAnalyzeEnabled)
                        }.padding(vertical = 8.dp)
                    ) {
                        Text("Smart Analyze (merge short wake-ups)", style = MaterialTheme.typography.bodyMedium)
                        androidx.compose.material3.Switch(
                            checked = smartAnalyzeEnabled,
                            onCheckedChange = {
                                smartAnalyzeEnabled = it
                                recentPeriods = BedtimeDetector.findRecentScreenOffPeriods(context, settings, smartAnalyzeEnabled)
                            }
                        )
                    }
                    if (recentPeriods.isEmpty()) {
                        Text(
                            "No screen-off stretches of an hour or longer were found in the last 24 hours.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        // Capped so the dialog never grows past a comfortable scroll height even on
                        // a day with many long off-stretches.
                        LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                            items(recentPeriods) { period ->
                                val durationLabel = TimeUtils.formatDurationShort(period.endEpochMillis - period.startEpochMillis - period.totalPausedMillis)
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(MaterialTheme.shapes.small)
                                        .clickable {
                                            startMillis = period.startEpochMillis
                                            endMillis = period.endEpochMillis
                                            totalPausedMillis = period.totalPausedMillis
                                            sleepDetection = null
                                            showRecentPeriodsDialog = false
                                        }
                                        .padding(vertical = 10.dp, horizontal = 4.dp)
                                ) {
                                Text(
                                    TimeUtils.formatDate(period.startEpochMillis, zone.id),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "${TimeUtils.formatTime(period.startEpochMillis, zone.id)} \u2192 " +
                                        TimeUtils.formatTime(period.endEpochMillis, zone.id),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    durationLabel,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (period.totalPausedMillis > 0) {
                                    Text(
                                        "Merged ${TimeUtils.formatDurationShort(period.totalPausedMillis)} awake",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { showRecentPeriodsDialog = false }) { Text("Close") }
        }
        )
    }
}
