package com.scanpilot.app.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.scanpilot.app.data.DocumentEntity
import com.scanpilot.app.data.FolderEntity
import com.scanpilot.app.repository.SortOrder
import com.scanpilot.app.util.PdfUtils
import com.scanpilot.app.util.formatDate
import com.scanpilot.app.util.formatSize
import com.scanpilot.app.util.sharePdf
import com.scanpilot.app.util.toWesternDigits
import com.scanpilot.app.viewmodel.LibraryViewModel
import com.scanpilot.app.viewmodel.UiMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Thumbnail of a document's first page — built with the built-in PdfRenderer. */
@Composable
private fun DocumentThumbnail(file: File, modifier: Modifier = Modifier) {
    var bitmap by remember(file) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(file) {
        bitmap = withContext(Dispatchers.IO) { PdfUtils.renderThumbnail(file) }
    }
    Box(
        modifier = modifier
            .size(64, 84)
            .background(Color(0xFFE0F2F1), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        val image = bitmap
        if (image != null) {
            Image(
                bitmap = image.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text("PDF", color = Color(0xFF00695C))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onOpenDocument: (Long) -> Unit,
    onOpenScan: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPro: () -> Unit
) {
    val context = LocalContext.current
    val documents by viewModel.documents.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val selectedFolderId by viewModel.selectedFolderId.collectAsState()
    val showFavoritesOnly by viewModel.showFavoritesOnly.collectAsState()
    val selectedIds by viewModel.selectedIds.collectAsState()
    val message by viewModel.message.collectAsState()
    val showRatePrompt by viewModel.showRatePrompt.collectAsState()
    val showProUpsell by viewModel.showProUpsell.collectAsState()
    val isGridView by viewModel.isGridView.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) viewModel.importPdf(uri)
    }

    var renameTarget by remember { mutableStateOf<DocumentEntity?>(null) }
    var renameText by remember { mutableStateOf("") }
    var moveTarget by remember { mutableStateOf<DocumentEntity?>(null) }
    var deleteTarget by remember { mutableStateOf<DocumentEntity?>(null) }
    var deleteFolderTarget by remember { mutableStateOf<FolderEntity?>(null) }
    var showNewFolder by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    LaunchedEffect(message) {
        val msg = message as? UiMessage.Text ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg.text)
        viewModel.clearMessage()
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(if (selectedIds.isEmpty()) "ScanPilot" else "${selectedIds.size} documents selected")
                },
                actions = {
                    if (selectedIds.isNotEmpty()) {
                        TextButton(onClick = { viewModel.mergeSelected() }) { Text("Merge") }
                        TextButton(onClick = { viewModel.clearSelection() }) { Text("Cancel") }
                    } else {
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                FloatingActionButton(
                    onClick = { importLauncher.launch(arrayOf("application/pdf")) },
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Import PDF")
                }
                FloatingActionButton(onClick = onOpenScan) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Scan document")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp)
        ) {
            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search documents…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Sorting and favourites
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = { viewModel.sortOrder.value = SortOrder.DATE },
                    label = { Text("Newest") },
                    leadingIcon = null
                )
                AssistChip(
                    onClick = { viewModel.sortOrder.value = SortOrder.NAME },
                    label = { Text("Name") },
                    leadingIcon = null
                )
                AssistChip(
                    onClick = { viewModel.sortOrder.value = SortOrder.SIZE },
                    label = { Text("Size") },
                    leadingIcon = null
                )
                AssistChip(
                    onClick = { viewModel.toggleGridView() },
                    label = { Text(if (isGridView) "List" else "Grid") },
                    leadingIcon = null
                )
                AssistChip(
                    onClick = { viewModel.showFavoritesOnly.value = !showFavoritesOnly },
                    label = { Text(if (showFavoritesOnly) "Favourites only ✓" else "Favourites") },
                    leadingIcon = null
                )
                AssistChip(
                    onClick = { showNewFolder = true },
                    label = { Text("New folder +") },
                    leadingIcon = null
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            // Folder filter
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    AssistChip(
                        onClick = { viewModel.selectedFolderId.value = null },
                        label = { Text("All") },
                        leadingIcon = null
                    )
                }
                items(folders) { folder ->
                    AssistChip(
                        onClick = { viewModel.selectedFolderId.value = folder.id },
                        label = { Text(folder.name) },
                        leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (documents.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "You don't have any documents yet.",
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Scan your first document or import a PDF.",
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { importLauncher.launch(arrayOf("application/pdf")) }) {
                        Text("Add your first file")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onOpenScan) { Text("Scan with camera") }
                }
            } else if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(documents, key = { it.id }) { doc ->
                        GridDocumentCard(
                            doc = doc,
                            selected = selectedIds.contains(doc.id),
                            onClick = {
                                if (selectedIds.isNotEmpty()) viewModel.toggleSelection(doc.id)
                                else onOpenDocument(doc.id)
                            },
                            onLongClick = { viewModel.toggleSelection(doc.id) },
                            onToggleFavorite = { viewModel.toggleFavorite(doc) }
                        )
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(documents, key = { it.id }) { doc ->
                        DocumentRow(
                            doc = doc,
                            selected = selectedIds.contains(doc.id),
                            onClick = {
                                if (selectedIds.isNotEmpty()) viewModel.toggleSelection(doc.id)
                                else onOpenDocument(doc.id)
                            },
                            onLongClick = { viewModel.toggleSelection(doc.id) },
                            onToggleFavorite = { viewModel.toggleFavorite(doc) },
                            onShare = { sharePdf(context, File(doc.filePath), doc.fileName) },
                            onRename = { renameTarget = doc; renameText = doc.fileName.removeSuffix(".pdf") },
                            onMove = { moveTarget = doc },
                            onDelete = { deleteTarget = doc }
                        )
                    }
                }
            }
        }
    }

    // Pro upgrade window — only after the user has felt the value
    // (tried merging or hit the scan cap), never on first launch
    if (showProUpsell) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissProUpsell() },
            title = { Text("ScanPilot Pro") },
            text = {
                Text(
                    "With Pro: unlimited scans, merge and split, AI summaries, " +
                        "password lock and no watermark."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.dismissProUpsell()
                    onOpenPro()
                }) { Text("View Pro") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissProUpsell() }) { Text("Later") }
            }
        )
    }

    // Rating request — only after 3 successful uses and only once, never on first launch
    if (showRatePrompt) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissRatePrompt() },
            title = { Text("Happy with ScanPilot?") },
            text = { Text("If this app has been useful to you, please rate it on Google Play.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.dismissRatePrompt()
                    openPlayStoreRating(context)
                }) { Text("Rate it") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissRatePrompt() }) { Text("Later") }
            }
        )
    }

    // Rename dialog
    renameTarget?.let { doc ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename document") },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.renameDocument(doc.id, renameText)
                    renameTarget = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("Cancel") }
            }
        )
    }

    // Move to folder dialog
    moveTarget?.let { doc ->
        AlertDialog(
            onDismissRequest = { moveTarget = null },
            title = { Text("Move to folder") },
            text = {
                Column {
                    TextButton(onClick = {
                        viewModel.moveToFolder(doc.id, null)
                        moveTarget = null
                    }) { Text("No folder (all)") }
                    folders.forEach { folder ->
                        TextButton(onClick = {
                            viewModel.moveToFolder(doc.id, folder.id)
                            moveTarget = null
                        }) { Text(folder.name) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { moveTarget = null }) { Text("Close") }
            }
        )
    }

    // Delete document dialog
    deleteTarget?.let { doc ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete document") },
            text = { Text("Delete the document “${doc.fileName}” permanently?") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteDocument(doc)
                    deleteTarget = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }

    // New folder dialog
    if (showNewFolder) {
        AlertDialog(
            onDismissRequest = { showNewFolder = false },
            title = { Text("New folder") },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    placeholder = { Text("E.g.: Lecture notes") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.createFolder(newFolderName)
                    newFolderName = ""
                    showNewFolder = false
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolder = false }) { Text("Cancel") }
            }
        )
    }

    // Delete folder (long-press on the folder chip will be added in the next version;
    // currently not reachable from here and recorded in BUILD_NOTES)
    deleteFolderTarget?.let { folder ->
        AlertDialog(
            onDismissRequest = { deleteFolderTarget = null },
            title = { Text("Delete folder") },
            text = { Text("Delete the folder “${folder.name}”? Its documents won't be deleted.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteFolder(folder)
                    deleteFolderTarget = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteFolderTarget = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun DocumentRow(
    doc: DocumentEntity,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DocumentThumbnail(file = File(doc.filePath))
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = doc.fileName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${formatSize(doc.sizeBytes)} • ${toWesternDigits(doc.pageCount.toString())} pages • ${formatDate(doc.dateAdded)}",
                    color = Color.Gray
                )
                if (selected) {
                    Text("Selected for merging", color = Color(0xFF00695C))
                }
            }
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    if (doc.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favourites"
                )
            }
            IconButton(onClick = { menuExpanded = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Options")
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Share") },
                    onClick = { menuExpanded = false; onShare() },
                    leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("Rename") },
                    onClick = { menuExpanded = false; onRename() }
                )
                DropdownMenuItem(
                    text = { Text("Move to folder") },
                    onClick = { menuExpanded = false; onMove() }
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = { menuExpanded = false; onDelete() },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
                )
            }
        }
    }
}

@Composable
private fun GridDocumentCard(
    doc: DocumentEntity,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                DocumentThumbnail(file = File(doc.filePath), modifier = Modifier.fillMaxWidth())
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                doc.fileName,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                "${formatSize(doc.sizeBytes)} • ${toWesternDigits(doc.pageCount.toString())} pages",
                color = Color.Gray
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(formatDate(doc.dateAdded), color = Color.Gray)
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (doc.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favourites"
                    )
                }
            }
            if (selected) {
                Text("Selected", color = Color(0xFF00695C))
            }
        }
    }
}
