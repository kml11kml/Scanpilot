package com.scanpilot.app.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.scanpilot.app.data.DocumentEntity
import com.scanpilot.app.util.PdfUtils
import com.scanpilot.app.util.formatDate
import com.scanpilot.app.util.formatSize
import com.scanpilot.app.util.toWesternDigits
import com.scanpilot.app.viewmodel.LibraryViewModel
import com.scanpilot.app.viewmodel.ScanViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Home screen tools — the familiar scanner-app pattern, with labels. */
enum class HomeTool(val label: String, val icon: ImageVector) {
    SCAN("Scan", Icons.Default.CameraAlt),
    READ("Read PDF", Icons.Default.Description),
    EDIT("Edit", Icons.Default.Edit),
    MERGE("Merge", Icons.Default.CallMerge),
    SPLIT("Split", Icons.Default.ContentCut),
    IMAGE_TO_PDF("Photo to PDF", Icons.Default.Image),
    PDF_TO_IMAGE("PDF to Photo", Icons.Default.Photo),
    LOCK("Lock with password", Icons.Default.Lock),
    SIGN("Sign", Icons.Default.BorderColor),
    SUMMARY("AI summary", Icons.Default.Summarize)
}

@Composable
fun HomeScreen(
    libraryViewModel: LibraryViewModel,
    scanViewModel: ScanViewModel,
    isPro: Boolean,
    onOpenDocument: (Long) -> Unit,
    onOpenScan: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenTools: () -> Unit,
    onToolClick: (HomeTool) -> Unit,
    onOpenPro: () -> Unit
) {
    val documents by libraryViewModel.documents.collectAsState()
    val searchQuery by libraryViewModel.searchQuery.collectAsState()
    val photos by scanViewModel.photos.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Search bar at the top of the page
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { libraryViewModel.searchQuery.value = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search documents…") },
            leadingIcon = {
                androidx.compose.material3.Icon(Icons.Default.Search, contentDescription = null)
            },
            singleLine = true,
            shape = RoundedCornerShape(28.dp)
        )
        if (searchQuery.isNotBlank()) {
            TextButton(onClick = onOpenLibrary) {
                Text("View all results in the library")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Gentle reminder: unsaved scanned pages
        if (photos.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenScan() },
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("You have unsaved scanned pages")
                    Text(
                        "${toWesternDigits(photos.size.toString())} pages ready — continue to save as PDF.",
                        color = Color.Gray
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Continue reading — most recent documents
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Continue reading", fontSize = 18.sp)
            TextButton(onClick = onOpenLibrary) { Text("All") }
        }
        Spacer(modifier = Modifier.height(8.dp))
        if (documents.isEmpty()) {
            Text("You don't have any documents yet — scan or import your first one.", color = Color.Gray)
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(documents.take(5)) { doc ->
                    RecentDocCard(doc = doc, onClick = { onOpenDocument(doc.id) })
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Free / Pro version banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenPro() },
            colors = CardDefaults.cardColors(containerColor = Color(0xFFE0F2F1))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(if (isPro) "Pro is active" else "Free version")
                    Text(
                        if (isPro) "Unlimited scans, merging and summaries, no watermark"
                        else "3 scans a day, free — upgrade for unlimited",
                        color = Color.Gray
                    )
                }
                if (!isPro) Text("Upgrade", color = Color(0xFF00695C))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        // Tools grid — 2 columns, large cards
        Text("Tools", fontSize = 18.sp)
        Spacer(modifier = Modifier.height(8.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(HomeTool.entries.toList()) { tool ->
                ToolCard(tool = tool, onClick = { onToolClick(tool) })
            }
        }
    }
}

@Composable
private fun RecentDocCard(doc: DocumentEntity, onClick: () -> Unit) {
    var bitmap by remember(doc) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(doc) {
        bitmap = withContext(Dispatchers.IO) { PdfUtils.renderThumbnail(File(doc.filePath)) }
    }
    Card(
        modifier = Modifier
            .width(150.dp)
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
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
            Spacer(modifier = Modifier.height(8.dp))
            Text(doc.fileName, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${formatSize(doc.sizeBytes)} • ${formatDate(doc.dateAdded)}",
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun ToolCard(tool: HomeTool, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            androidx.compose.material3.Icon(
                tool.icon,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = Color(0xFF00695C)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(tool.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
