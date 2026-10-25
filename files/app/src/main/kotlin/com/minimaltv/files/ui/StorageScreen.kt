package com.minimaltv.files.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.minimaltv.files.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean

private val TileShape = RoundedCornerShape(20.dp)
private val RowShape = RoundedCornerShape(16.dp)

/** A quick way to jump to a storage volume or a standard folder. */
private class Shortcut(val label: String, val icon: ImageVector, val dir: File)

/** One tile or row in the browser; [entry] is null for the "New folder" cell. */
private class Cell(val entry: Entry?) {
    val key: String get() = entry?.file?.absolutePath ?: "\u0000new"
}

/**
 * A small file manager driven entirely with the remote: shortcuts on top, the folder with its path, then tiles or a list.
 * OK opens a folder or a file; the three dots on a tile or row (or long-press OK, or the Menu key) offer Open, Copy,
 * Move, Rename, Delete and Details; Back goes up one folder.
 */
@Composable
fun StorageScreen(startAt: File?, onStartConsumed: () -> Unit) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var granted by remember { mutableStateOf(hasStorageAccess(ctx)) }
    var resumed by remember { mutableStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, e ->
            if (e == Lifecycle.Event.ON_RESUME) { granted = hasStorageAccess(ctx); resumed++ }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (!granted) {
        AccessPrompt(onGranted = { granted = hasStorageAccess(ctx) })
        return
    }

    val prefs = remember { ViewPrefs(ctx) }
    var grid by remember { mutableStateOf(prefs.grid) }
    var sortBy by remember { mutableStateOf(prefs.sort) }
    var descending by remember { mutableStateOf(prefs.descending) }
    var showHidden by remember { mutableStateOf(prefs.hidden) }

    val internal = remember { internalRoot() }
    var volumes by remember { mutableStateOf(removableVolumes()) }
    var current by remember { mutableStateOf(startAt ?: internal) }
    var query by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableStateOf(0) }
    var comeFrom by remember { mutableStateOf<String?>(null) }
    var anyFocused by remember { mutableStateOf(false) }

    var searching by remember { mutableStateOf(false) }
    var optionsOpen by remember { mutableStateOf(false) }
    var newFolder by remember { mutableStateOf(false) }
    var menuFor by remember { mutableStateOf<Entry?>(null) }
    var renaming by remember { mutableStateOf<Entry?>(null) }
    var toDelete by remember { mutableStateOf<Entry?>(null) }
    var detailsFor by remember { mutableStateOf<Entry?>(null) }
    var clip by remember { mutableStateOf<Clip?>(null) }
    var busy by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    val cancel = remember { AtomicBoolean(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(startAt) {
        if (startAt != null) { current = startAt; query = null; comeFrom = null; onStartConsumed() }
    }
    LaunchedEffect(resumed) { volumes = withContext(Dispatchers.IO) { removableVolumes() } }

    val atRoot = current == internal || volumes.any { it == current }
    BackHandler(enabled = !atRoot) { comeFrom = current.name; current = current.parentFile ?: internal }
    BackHandler(enabled = query != null) { query = null }

    fun go(dir: File) { comeFrom = null; query = null; current = dir }

    val entries by produceState<List<Entry>?>(initialValue = null, current, query, refresh, resumed, sortBy, descending, showHidden) {
        value = null
        value = withContext(Dispatchers.IO) {
            val q = query
            if (q != null) searchTree(current, q, sortBy, descending, showHidden)
            else listDir(current, sortBy, descending, showHidden).orEmpty()
        }
    }

    val shortcuts = remember(volumes, resumed) { buildShortcuts(ctx, internal, volumes) }
    val activeShortcut = shortcuts
        .filter { current == it.dir || current.path.startsWith(it.dir.path + "/") }
        .maxByOrNull { it.dir.path.length }
    val title = folderTitle(ctx, current, internal, volumes)
    val where = pathLabel(ctx, current, internal, volumes)

    Column(
        Modifier.fillMaxSize().onFocusChanged { anyFocused = it.hasFocus }.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.files_app_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f).padding(start = 4.dp),
            )
            SearchBox(query) { searching = true }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(shortcuts, key = { it.dir.path }) { s ->
                ShortcutChip(s, active = s === activeShortcut) { go(s.dir) }
            }
        }

        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f).padding(start = 4.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    query?.let { stringResource(R.string.search_results, it) } ?: where,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
            }
            IconPill(Icons.AutoMirrored.Filled.Sort, stringResource(R.string.sort_button)) { optionsOpen = true }
            IconPill(
                if (grid) Icons.AutoMirrored.Filled.ViewList else Icons.Default.GridView,
                stringResource(if (grid) R.string.layout_to_list else R.string.layout_to_tiles),
            ) { grid = !grid; prefs.grid = grid }
        }

        clip?.let { c ->
            PasteBar(
                clip = c, busy = busy, progress = progress, canPaste = !busy,
                onPaste = {
                    val src = c.file
                    val inside = current.canonicalPath == src.canonicalPath || current.canonicalPath.startsWith(src.canonicalPath + "/")
                    if (inside && src.isDirectory) {
                        Toast.makeText(ctx, R.string.storage_into_itself, Toast.LENGTH_LONG).show()
                    } else if (c.move && current.canonicalPath == src.parentFile?.canonicalPath) {
                        Toast.makeText(ctx, R.string.storage_same_folder, Toast.LENGTH_SHORT).show()
                        clip = null
                    } else {
                        busy = true; progress = 0f; cancel.set(false)
                        val dest = current
                        scope.launch {
                            val made = withContext(Dispatchers.IO) { paste(ctx, c, dest, cancel) { progress = it } }
                            busy = false
                            if (made != null) { clip = null; comeFrom = made.name; refresh++ }
                            else Toast.makeText(ctx, if (cancel.get()) R.string.storage_cancelled else R.string.storage_op_failed, Toast.LENGTH_LONG).show()
                        }
                    }
                },
                onCancel = { if (busy) cancel.set(true) else clip = null },
            )
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            val items = entries
            when {
                items == null -> Unit
                items.isEmpty() && query != null -> Message(R.string.search_none)
                items.isEmpty() && current.listFiles() == null -> Message(R.string.storage_unreadable)
                else -> {
                    val cells = remember(items, query) {
                        (if (query == null) listOf(Cell(null)) else emptyList()) + items.map { Cell(it) }
                    }
                    Browser(
                        cells = cells, grid = grid, comeFrom = comeFrom, anyFocused = { anyFocused },
                        onOpen = { e ->
                            if (e.isDir) go(e.file)
                            else { comeFrom = e.file.name; openFile(ctx, e.file) }
                        },
                        onMore = { menuFor = it },
                        onNew = { newFolder = true },
                    )
                }
            }
        }
    }

    if (searching) {
        TextEditDialog(
            label = stringResource(R.string.search_title, title),
            initial = query.orEmpty(),
            onDismiss = { searching = false },
            onConfirm = { q -> comeFrom = null; query = q.trim() },
        )
    }

    if (newFolder) {
        TextEditDialog(
            label = stringResource(R.string.new_folder_name),
            initial = "",
            validate = { n -> val t = n.trim(); t.isNotEmpty() && !t.contains('/') && !File(current, t).exists() },
            onDismiss = { newFolder = false },
            onConfirm = { n ->
                val made = createFolder(current, n.trim())
                if (made != null) { comeFrom = made.name; refresh++ }
                else Toast.makeText(ctx, R.string.storage_op_failed, Toast.LENGTH_LONG).show()
            },
        )
    }

    if (optionsOpen) {
        OptionsDialog(
            sortBy = sortBy, descending = descending, hidden = showHidden,
            onSort = { sortBy = it; prefs.sort = it },
            onOrder = { descending = !descending; prefs.descending = descending },
            onHidden = { showHidden = !showHidden; prefs.hidden = showHidden },
            onDismiss = { optionsOpen = false },
        )
    }

    menuFor?.let { e ->
        // the standard top-level folders (Download, DCIM, Android ...) can only be copied
        val protected = e.isDir && e.file.parentFile == internal
        ActionMenu(
            name = e.file.name,
            isDir = e.isDir,
            protected = protected,
            enabled = !busy,
            onOpen = { menuFor = null; comeFrom = e.file.name; openFile(ctx, e.file) },
            onCopy = { menuFor = null; clip = Clip(e.file, move = false) },
            onMove = { menuFor = null; clip = Clip(e.file, move = true) },
            onRename = { menuFor = null; renaming = e },
            onDelete = { menuFor = null; toDelete = e },
            onDetails = { menuFor = null; detailsFor = e },
            onDismiss = { menuFor = null },
        )
    }

    renaming?.let { e ->
        TextEditDialog(
            label = stringResource(R.string.storage_rename),
            initial = e.file.name,
            validate = { n -> val t = n.trim(); t.isNotEmpty() && !t.contains('/') && (t == e.file.name || !File(e.file.parentFile, t).exists()) },
            onDismiss = { renaming = null },
            onConfirm = { n ->
                val made = renameTo(ctx, e.file, n.trim())
                if (made != null) { comeFrom = made.name; refresh++ }
                else Toast.makeText(ctx, R.string.storage_op_failed, Toast.LENGTH_LONG).show()
            },
        )
    }

    detailsFor?.let { e -> DetailsDialog(e) { detailsFor = null } }

    toDelete?.let { e ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text(stringResource(R.string.storage_delete_title, e.file.name), maxLines = 2, overflow = TextOverflow.Ellipsis) },
            text = {
                Text(
                    stringResource(if (e.isDir) R.string.storage_delete_folder else R.string.storage_delete_file),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            confirmButton = {
                val cancelButton = remember { FocusRequester() }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            val ok = deletePath(ctx, e.file)
                            toDelete = null
                            refresh++
                            Toast.makeText(ctx, if (ok) R.string.storage_deleted else R.string.storage_delete_failed, Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.dpadFocus(CircleShape),
                    ) { Text(stringResource(R.string.files_delete)) }
                    TextButton(onClick = { toDelete = null }, modifier = Modifier.dpadFocus(CircleShape).focusRequester(cancelButton)) {
                        Text(stringResource(android.R.string.cancel))
                    }
                }
                // start on Cancel so one stray OK press deletes nothing
                LaunchedEffect(Unit) { cancelButton.requestFocusUntilLanded() }
            },
        )
    }
}

@Composable
private fun Message(res: Int) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(stringResource(res), style = MaterialTheme.typography.bodyLarge, color = LocalGlass.current.textDim)
    }
}

/** The tiles or the list. Puts the selection on the folder we came from, or the first item. */
@Composable
private fun Browser(
    cells: List<Cell>, grid: Boolean, comeFrom: String?, anyFocused: () -> Boolean,
    onOpen: (Entry) -> Unit, onMore: (Entry) -> Unit, onNew: () -> Unit,
) {
    val focus = remember { FocusRequester() }
    val windowFocused = LocalWindowInfo.current.isWindowFocused
    val gridState = rememberLazyGridState()
    val listState = rememberLazyListState()
    val focusIndex = remember(cells) {
        comeFrom?.let { n -> cells.indexOfFirst { it.entry?.file?.name == n }.takeIf { it >= 0 } }
            ?: if (cells.size > 1 && cells[0].entry == null) 1 else 0
    }
    LaunchedEffect(cells) {
        if (grid) gridState.scrollToItem(focusIndex) else listState.scrollToItem(focusIndex)
        focus.requestFocusUntilLanded()
    }
    // when another app brings this one to the front the window gets focus a moment later, so ask again then
    LaunchedEffect(cells, windowFocused) {
        if (windowFocused && !anyFocused()) focus.requestFocusUntilLanded(attempts = 30, isFocused = anyFocused)
    }

    if (grid) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(200.dp),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 8.dp),
        ) {
            itemsIndexed(cells, key = { _, c -> c.key }) { index, c ->
                Tile(
                    c.entry,
                    modifier = if (index == focusIndex) Modifier.focusRequester(focus) else Modifier,
                    onOpen = { c.entry?.let(onOpen) ?: onNew() },
                    onMore = c.entry?.let { e -> { onMore(e) } },
                )
            }
        }
    } else {
        Column(Modifier.fillMaxSize().glass().padding(vertical = 10.dp)) {
            LazyColumn(Modifier.fillMaxSize().padding(horizontal = 8.dp), state = listState) {
                itemsIndexed(cells, key = { _, c -> c.key }) { index, c ->
                    EntryRow(
                        c.entry,
                        modifier = if (index == focusIndex) Modifier.focusRequester(focus) else Modifier,
                        onOpen = { c.entry?.let(onOpen) ?: onNew() },
                        onMore = c.entry?.let { e -> { onMore(e) } },
                    )
                }
            }
        }
    }
}

private fun iconFor(kind: Kind): ImageVector = when (kind) {
    Kind.FOLDER -> Icons.Default.Folder
    Kind.VIDEO -> Icons.Default.Movie
    Kind.AUDIO -> Icons.Default.MusicNote
    Kind.IMAGE -> Icons.Default.Image
    Kind.DOCUMENT -> Icons.Default.Description
    Kind.APK -> Icons.Default.Android
    Kind.ARCHIVE -> Icons.Default.FolderZip
    Kind.OTHER -> Icons.Default.InsertDriveFile
}

/** The small rounded square behind an icon. */
@Composable
private fun IconChip(icon: ImageVector, size: Int, modifier: Modifier = Modifier) {
    val g = LocalGlass.current
    Box(
        modifier.size(size.dp).clip(RoundedCornerShape((size / 3).dp)).background(g.divider),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription = null, tint = g.text, modifier = Modifier.size((size * 0.58f).dp)) }
}

/**
 * A tile: small icon top left, three dots top right, name bottom left. OK opens it; long-press OK or the Menu key
 * opens the same menu as the dots. The "New folder" tile has no dots.
 */
@Composable
private fun Tile(e: Entry?, modifier: Modifier, onOpen: () -> Unit, onMore: (() -> Unit)?) {
    val g = LocalGlass.current
    Box(Modifier.fillMaxWidth().height(100.dp)) {
        Column(
            modifier
                .fillMaxSize()
                .clip(TileShape)
                .background(g.fillBottom)
                .dpadFocus(TileShape)
                .pressable(onOpen, onMore)
                .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            IconChip(if (e == null) Icons.Default.CreateNewFolder else iconFor(kindOf(e)), 34)
            Text(
                if (e == null) stringResource(R.string.new_folder) else e.file.name,
                style = MaterialTheme.typography.bodyLarge, color = g.text,
                maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
        }
        if (onMore != null) {
            // the remote reaches the menu by long-pressing OK or the Menu key; the dots are for touch (list rows keep them focusable)
            Box(Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 4.dp).focusProperties { canFocus = false }) {
                RowAction(stringResource(R.string.storage_more), Icons.Default.MoreVert) { onMore() }
            }
        }
    }
}

@Composable
private fun EntryRow(e: Entry?, modifier: Modifier, onOpen: () -> Unit, onMore: (() -> Unit)?) {
    val g = LocalGlass.current
    Row(
        Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier
                .weight(1f)
                .dpadFocus(RowShape)
                .pressable(onOpen, onMore)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            IconChip(if (e == null) Icons.Default.CreateNewFolder else iconFor(kindOf(e)), 40)
            Column(Modifier.weight(1f)) {
                Text(
                    if (e == null) stringResource(R.string.new_folder) else e.file.name,
                    style = MaterialTheme.typography.bodyLarge, color = g.text, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                val sub = e?.let { subtitle(it) }.orEmpty()
                if (sub.isNotEmpty()) Text(sub, style = MaterialTheme.typography.bodySmall, color = g.textDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (e?.isDir == true) Icon(Icons.Default.ChevronRight, contentDescription = null, tint = g.textDim)
        }
        if (onMore != null) RowAction(stringResource(R.string.storage_more), Icons.Default.MoreVert) { onMore() }
    }
}

/** Size for files, the folder a search hit lives in. No dates. */
private fun subtitle(e: Entry): String =
    listOfNotNull(e.where?.takeIf { it.isNotEmpty() }, if (e.isDir) null else sizeText(e.size)).joinToString("  ·  ")

@Composable
private fun SearchBox(query: String?, onClick: () -> Unit) {
    val g = LocalGlass.current
    Row(
        Modifier
            .widthIn(min = 220.dp, max = 340.dp)
            .heightIn(min = 44.dp)
            .clip(CircleShape)
            .background(g.pill)
            .dpadFocus(CircleShape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = g.textDim)
        Text(
            query ?: stringResource(R.string.search_hint),
            style = MaterialTheme.typography.bodyLarge, color = if (query == null) g.textDim else g.text,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun IconPill(icon: ImageVector, description: String, onClick: () -> Unit) {
    val g = LocalGlass.current
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(g.pill)
            .dpadFocus(CircleShape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, contentDescription = description, tint = g.text) }
}

@Composable
private fun ShortcutChip(s: Shortcut, active: Boolean, onClick: () -> Unit) {
    val g = LocalGlass.current
    Row(
        Modifier
            .clip(CircleShape)
            .background(if (active) g.pill else g.fillBottom)
            .dpadFocus(CircleShape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(start = 6.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        IconChip(s.icon, 28)
        Text(s.label, style = MaterialTheme.typography.labelLarge, color = if (active) g.text else g.textDim, maxLines = 1)
    }
}

private fun usbLabel(ctx: Context, volume: File) = ctx.getString(R.string.storage_usb) + " " + volume.name

private fun buildShortcuts(ctx: Context, internal: File, volumes: List<File>): List<Shortcut> {
    val list = ArrayList<Shortcut>()
    list += Shortcut(ctx.getString(R.string.storage_internal), Icons.Default.Storage, internal)
    volumes.forEach { list += Shortcut(usbLabel(ctx, it), Icons.Default.Usb, it) }
    listOf(
        Triple("Download", R.string.cat_downloads, Icons.Default.Download),
        Triple("Movies", R.string.cat_videos, Icons.Default.Movie),
        Triple("Music", R.string.cat_music, Icons.Default.MusicNote),
        Triple("Pictures", R.string.cat_pictures, Icons.Default.Image),
        Triple("Documents", R.string.cat_documents, Icons.Default.Description),
    ).forEach { (dir, label, icon) ->
        File(internal, dir).takeIf { it.isDirectory }?.let { list += Shortcut(ctx.getString(label), icon, it) }
    }
    return list
}

private fun folderTitle(ctx: Context, folder: File, internal: File, volumes: List<File>): String = when {
    folder == internal -> ctx.getString(R.string.storage_internal)
    volumes.any { it == folder } -> usbLabel(ctx, folder)
    else -> folder.name
}

/** "Internal storage › Download › Movies" */
private fun pathLabel(ctx: Context, folder: File, internal: File, volumes: List<File>): String {
    val path = folder.absolutePath
    val root = internal.absolutePath
    val volume = volumes.firstOrNull { path == it.absolutePath || path.startsWith(it.absolutePath + "/") }
    val shown = when {
        path == root || path.startsWith("$root/") -> ctx.getString(R.string.storage_internal) + path.removePrefix(root)
        volume != null -> usbLabel(ctx, volume) + path.removePrefix(volume.absolutePath)
        else -> path
    }
    return shown.split('/').filter { it.isNotEmpty() }.joinToString("  ›  ")
}

private fun openFile(ctx: Context, file: File) {
    val uri = try { FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", file) } catch (_: Exception) { null }
    openContent(ctx, uri, mimeOf(file.name), file.name)
}

/** Sort order and whether hidden files show. Changes apply at once; Done (or Back) closes. */
@Composable
private fun OptionsDialog(
    sortBy: SortBy, descending: Boolean, hidden: Boolean,
    onSort: (SortBy) -> Unit, onOrder: () -> Unit, onHidden: () -> Unit, onDismiss: () -> Unit,
) {
    val first = remember { FocusRequester() }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = { Text(stringResource(R.string.sort_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                listOf(
                    SortBy.NAME to R.string.sort_name, SortBy.SIZE to R.string.sort_size,
                    SortBy.DATE to R.string.sort_date, SortBy.TYPE to R.string.sort_type,
                ).forEachIndexed { i, (s, label) ->
                    MenuItem(
                        stringResource(label), null, true, if (i == 0) Modifier.focusRequester(first) else Modifier,
                        checked = s == sortBy, onClick = { onSort(s) },
                    )
                }
                MenuItem(
                    stringResource(if (descending) R.string.order_desc else R.string.order_asc),
                    if (descending) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward, true, Modifier, onClick = onOrder,
                )
                MenuItem(stringResource(R.string.show_hidden), null, true, Modifier, checked = hidden, onClick = onHidden)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss, modifier = Modifier.dpadFocus(CircleShape)) { Text(stringResource(R.string.done)) } },
    )
    LaunchedEffect(Unit) { first.requestFocusUntilLanded() }
}

/** The "..." menu of a tile or row. */
@Composable
private fun ActionMenu(
    name: String, isDir: Boolean, protected: Boolean, enabled: Boolean,
    onOpen: () -> Unit, onCopy: () -> Unit, onMove: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit,
    onDetails: () -> Unit, onDismiss: () -> Unit,
) {
    val first = remember { FocusRequester() }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = { Text(name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                var firstRow = true
                fun modifierForNext(): Modifier = if (firstRow) { firstRow = false; Modifier.focusRequester(first) } else Modifier
                if (!isDir) MenuItem(stringResource(R.string.menu_open), Icons.AutoMirrored.Filled.OpenInNew, enabled, modifierForNext(), onClick = onOpen)
                MenuItem(stringResource(R.string.storage_copy), Icons.Default.ContentCopy, enabled, modifierForNext(), onClick = onCopy)
                if (!protected) {
                    MenuItem(stringResource(R.string.storage_move), Icons.Default.DriveFileMove, enabled, modifierForNext(), onClick = onMove)
                    MenuItem(stringResource(R.string.storage_rename), Icons.Default.DriveFileRenameOutline, enabled, modifierForNext(), onClick = onRename)
                    MenuItem(stringResource(R.string.files_delete), Icons.Default.DeleteOutline, enabled, modifierForNext(), onClick = onDelete)
                }
                MenuItem(stringResource(R.string.menu_details), Icons.Default.Info, true, modifierForNext(), onClick = onDetails)
            }
        },
        confirmButton = {},
    )
    LaunchedEffect(Unit) { first.requestFocusUntilLanded() }
}

@Composable
private fun DetailsDialog(e: Entry, onDismiss: () -> Unit) {
    val g = LocalGlass.current
    val ctx = LocalContext.current
    val info by produceState<Pair<Long, Int>?>(initialValue = null, e) {
        value = withContext(Dispatchers.IO) { treeSize(e.file) to (if (e.isDir) itemCount(e.file) else 0) }
    }
    val ok = remember { FocusRequester() }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = { Text(e.file.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailLine(stringResource(R.string.details_location), pathLabel(ctx, e.file.parentFile ?: e.file, internalRoot(), removableVolumes()))
                DetailLine(stringResource(R.string.details_size), info?.first?.let(::sizeText) ?: stringResource(R.string.details_calculating))
                if (e.isDir) DetailLine(stringResource(R.string.details_items), info?.second?.toString() ?: stringResource(R.string.details_calculating))
                DetailLine(stringResource(R.string.details_modified), DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(e.modified)))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.dpadFocus(CircleShape).focusRequester(ok)) { Text(stringResource(R.string.done)) }
        },
    )
    LaunchedEffect(Unit) { ok.requestFocusUntilLanded() }
}

@Composable
private fun DetailLine(label: String, value: String) {
    val g = LocalGlass.current
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = g.textDim)
        Text(value, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun MenuItem(
    label: String, icon: ImageVector?, enabled: Boolean, modifier: Modifier,
    checked: Boolean? = null, onClick: () -> Unit,
) {
    val g = LocalGlass.current
    Row(
        modifier
            .fillMaxWidth()
            .dpadFocus(RowShape)
            .clip(RowShape)
            .clickable(enabled = enabled, interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = g.textDim)
        Text(label, style = MaterialTheme.typography.bodyLarge, color = if (enabled) g.text else g.textDim, modifier = Modifier.weight(1f))
        if (checked == true) Icon(Icons.Default.Check, contentDescription = null, tint = g.text)
    }
}

/** Shown while a copy or move waits for its destination: browse to the folder, then Paste here. */
@Composable
private fun PasteBar(clip: Clip, busy: Boolean, progress: Float, canPaste: Boolean, onPaste: () -> Unit, onCancel: () -> Unit) {
    val g = LocalGlass.current
    Row(
        Modifier.fillMaxWidth().glass().padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(
                    when { busy && clip.move -> R.string.storage_moving; busy -> R.string.storage_copying; clip.move -> R.string.storage_move_what; else -> R.string.storage_copy_what },
                    clip.file.name,
                ),
                style = MaterialTheme.typography.bodyLarge, color = g.text, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
            if (busy) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = g.text, trackColor = g.divider)
            else Text(stringResource(R.string.storage_paste_hint), style = MaterialTheme.typography.bodySmall, color = g.textDim)
        }
        PillButton(stringResource(R.string.storage_paste), canPaste, onPaste)
        PillButton(stringResource(android.R.string.cancel), true, onCancel)
    }
}

@Composable
private fun PillButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    val g = LocalGlass.current
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = if (enabled) g.text else g.textDim,
        modifier = Modifier
            .clip(CircleShape)
            .background(g.pill)
            .dpadFocus(CircleShape)
            .clickable(enabled = enabled, interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 10.dp),
    )
}

/** Asks for access to the storage the first time. */
@Composable
private fun AccessPrompt(onGranted: () -> Unit) {
    val ctx = LocalContext.current
    val g = LocalGlass.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { onGranted() }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocusUntilLanded() }
    Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
        Column(
            Modifier.glass().padding(36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            Text(stringResource(R.string.storage_need_access), style = MaterialTheme.typography.titleMedium, color = g.text)
            Row(
                Modifier
                    .focusRequester(focus)
                    .clip(CircleShape)
                    .background(g.pill)
                    .dpadFocus(CircleShape)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        if (Build.VERSION.SDK_INT >= 30) {
                            val i = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:" + ctx.packageName))
                            if (i.resolveActivity(ctx.packageManager) != null) ctx.startActivity(i)
                            else Toast.makeText(ctx, R.string.storage_no_settings, Toast.LENGTH_LONG).show()
                        } else {
                            launcher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE))
                        }
                    }
                    .padding(horizontal = 30.dp, vertical = 12.dp),
            ) { Text(stringResource(R.string.storage_allow), style = MaterialTheme.typography.labelLarge, color = g.text) }
        }
    }
}
