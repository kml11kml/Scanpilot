package com.scanpilot.app.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FormatStrikethrough
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.scanpilot.app.util.AnnotationStroke
import com.scanpilot.app.util.AnnotationTool
import com.scanpilot.app.util.PdfUtils
import com.scanpilot.app.util.sharePdf
import com.scanpilot.app.util.toWesternDigits
import com.scanpilot.app.viewmodel.SummaryState
import com.scanpilot.app.viewmodel.ViewerViewModel
import java.io.File

/** Color inversion matrix for night mode. */
private val NIGHT_MATRIX = ColorMatrix(
    floatArrayOf(
        -1f, 0f, 0f, 0f, 255f,
        0f, -1f, 0f, 0f, 255f,
        0f, 0f, -1f, 0f, 255f,
        0f, 0f, 0f, 1f, 0f
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    docId: Long,
    viewModel: ViewerViewModel,
    onBack: () -> Unit,
    editModeInitial: Boolean = false
) {
    val context = LocalContext.current
    val document by viewModel.document.collectAsState()
    val currentPage by viewModel.currentPage.collectAsState()
    val pageBitmap by viewModel.pageBitmap.collectAsState()
    val summaryState by viewModel.summaryState.collectAsState()

    var showSummary by remember { mutableStateOf(false) }
    var showGoToPage by remember { mutableStateOf(false) }
    var goToPageText by remember { mutableStateOf("") }
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var nightMode by remember { mutableStateOf(false) }
    var editMode by remember(editModeInitial) { mutableStateOf(editModeInitial) }
    var selectedTool by remember { mutableStateOf(AnnotationTool.HIGHLIGHT) }
    val strokes = remember { mutableStateListOf<AnnotationStroke>() }
    var currentPoints by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var overlaySize by remember { mutableStateOf(IntSize.Zero) }

    LaunchedEffect(docId) {
        viewModel.loadDocument(docId)
        strokes.clear()
    }
    // When the page changes, the previous page's annotations are cleared (not saved)
    LaunchedEffect(currentPage) {
        strokes.clear()
        currentPoints = emptyList()
    }

    val doc = document
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(doc?.fileName ?: "View document") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { nightMode = !nightMode }) {
                        Icon(Icons.Default.Brightness4, contentDescription = "Night mode")
                    }
                    IconButton(onClick = { editMode = !editMode }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = {
                        showSummary = true
                        viewModel.requestSummary()
                    }) {
                        Icon(Icons.Default.Summarize, contentDescription = "AI summary")
                    }
                    IconButton(onClick = { viewModel.toggleFavorite() }) {
                        Icon(
                            if (doc?.isFavorite == true) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite"
                        )
                    }
                    IconButton(onClick = {
                        doc?.let { sharePdf(context, File(it.filePath), it.fileName) }
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                }
            )
        },
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                if (editMode) {
                    // Annotation toolbar — only in edit mode
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AnnotationTool.entries.forEach { tool ->
                            FilterChip(
                                selected = selectedTool == tool,
                                onClick = { selectedTool = tool },
                                label = { Text(tool.label) }
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = {
                            strokes.clear()
                            currentPoints = emptyList()
                        }) { Text("Clear all") }
                        Button(
                            onClick = {
                                viewModel.saveAnnotations(
                                    pageIndex = currentPage,
                                    strokes = strokes.toList(),
                                    onSaved = { strokes.clear() }
                                )
                            },
                            enabled = strokes.isNotEmpty()
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save annotations")
                        }
                    }
                }
                if (doc != null && doc.pageCount > 0) {
                    // Page slider
                    Slider(
                        value = currentPage.toFloat(),
                        onValueChange = { viewModel.goToPage(it.toInt()) },
                        valueRange = 0f..(doc.pageCount - 1).coerceAtLeast(1).toFloat(),
                        steps = if (doc.pageCount > 2) doc.pageCount - 2 else 0,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { viewModel.previousPage() },
                            enabled = currentPage > 0
                        ) { Text("Previous") }
                        TextButton(onClick = { showGoToPage = true }) {
                            Text(
                                "Page ${toWesternDigits((currentPage + 1).toString())} of " +
                                    toWesternDigits(doc.pageCount.toString())
                            )
                        }
                        Button(
                            onClick = { viewModel.nextPage() },
                            enabled = currentPage < doc.pageCount - 1
                        ) { Text("Next") }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            when {
                doc == null -> {
                    CircularProgressIndicator()
                }
                doc.pageCount == 0 -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text("This file cannot be opened.")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "The file may be damaged or password-protected. Try another valid PDF file.",
                            color = Color.Gray
                        )
                    }
                }
                pageBitmap == null -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Preparing page…")
                    }
                }
                else -> {
                    val bitmap: Bitmap = pageBitmap as Bitmap
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .onSizeChanged { overlaySize = it }
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            )
                            .pointerInput(editMode) {
                                if (editMode) {
                                    detectDragGestures(
                                        onDragStart = { offset ->
                                            currentPoints = listOf(offset)
                                        },
                                        onDrag = { change, _ ->
                                            currentPoints = currentPoints + change.position
                                        },
                                        onDragEnd = {
                                            if (currentPoints.size >= 2 && overlaySize.width > 0) {
                                                val normalized = currentPoints.map {
                                                    Offset(
                                                        it.x / overlaySize.width,
                                                        it.y / overlaySize.height
                                                    )
                                                }
                                                strokes.add(
                                                    AnnotationStroke(
                                                        tool = selectedTool,
                                                        points = normalized,
                                                        colorArgb = when (selectedTool) {
                                                            AnnotationTool.HIGHLIGHT -> android.graphics.Color.YELLOW
                                                            AnnotationTool.DRAW, AnnotationTool.SIGN -> android.graphics.Color.BLUE
                                                            else -> android.graphics.Color.RED
                                                        },
                                                        strokeWidth = if (selectedTool == AnnotationTool.SIGN) 4f else 10f
                                                    )
                                                )
                                            }
                                            currentPoints = emptyList()
                                        }
                                    )
                                } else {
                                    detectTransformGestures { _, pan, zoom, _ ->
                                        val newScale = (scale * zoom).coerceIn(1f, 4f)
                                        scale = newScale
                                        if (newScale > 1f) {
                                            offsetX += pan.x
                                            offsetY += pan.y
                                        } else {
                                            offsetX = 0f
                                            offsetY = 0f
                                        }
                                    }
                                }
                            }
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Page ${currentPage + 1}",
                            modifier = Modifier.fillMaxSize(),
                            colorFilter = if (nightMode) ColorFilter.colorMatrix(NIGHT_MATRIX) else null
                        )
                        // Annotation drawing layer over the page
                        if (editMode) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val allStrokes = strokes.toList() +
                                    if (currentPoints.size >= 2) {
                                        listOf(
                                            AnnotationStroke(
                                                tool = selectedTool,
                                                points = currentPoints.map {
                                                    Offset(
                                                        it.x / size.width,
                                                        it.y / size.height
                                                    )
                                                }
                                            )
                                        )
                                    } else emptyList()
                                for (stroke in allStrokes) {
                                    val pts = stroke.points.map {
                                        Offset(it.x * size.width, it.y * size.height)
                                    }
                                    when (stroke.tool) {
                                        AnnotationTool.HIGHLIGHT -> {
                                            val minX = pts.minOf { it.x }
                                            val maxX = pts.maxOf { it.x }
                                            val minY = pts.minOf { it.y }
                                            val maxY = pts.maxOf { it.y }
                                            drawRect(
                                                color = Color.Yellow.copy(alpha = 0.4f),
                                                topLeft = Offset(minX, minY),
                                                size = androidx.compose.ui.geometry.Size(
                                                    (maxX - minX).coerceAtLeast(20f),
                                                    (maxY - minY).coerceAtLeast(24f)
                                                )
                                            )
                                        }
                                        AnnotationTool.UNDERLINE -> {
                                            val y = pts.maxOf { it.y }
                                            drawLine(
                                                color = Color.Red,
                                                start = Offset(pts.minOf { it.x }, y),
                                                end = Offset(pts.maxOf { it.x }, y),
                                                strokeWidth = 4f
                                            )
                                        }
                                        AnnotationTool.STRIKE -> {
                                            val y = (pts.minOf { it.y } + pts.maxOf { it.y }) / 2
                                            drawLine(
                                                color = Color.Red,
                                                start = Offset(pts.minOf { it.x }, y),
                                                end = Offset(pts.maxOf { it.x }, y),
                                                strokeWidth = 4f
                                            )
                                        }
                                        AnnotationTool.DRAW, AnnotationTool.SIGN -> {
                                            for (i in 0 until pts.size - 1) {
                                                drawLine(
                                                    color = stroke.color,
                                                    start = pts[i],
                                                    end = pts[i + 1],
                                                    strokeWidth = stroke.strokeWidth,
                                                    cap = StrokeCap.Round
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // AI summary dialog
    if (showSummary) {
        AlertDialog(
            onDismissRequest = {
                showSummary = false
                viewModel.clearSummary()
            },
            title = { Text("AI summary") },
            text = {
                when (val state = summaryState) {
                    is SummaryState.Idle -> Text("Preparing…")
                    is SummaryState.Loading -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Summarizing document…")
                    }
                    is SummaryState.Success -> {
                        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                            Text(state.text)
                        }
                    }
                    is SummaryState.Error -> Text(state.message)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    showSummary = false
                    viewModel.clearSummary()
                }) { Text("Close") }
            }
        )
    }

    // Go to page dialog
    if (showGoToPage && doc != null) {
        AlertDialog(
            onDismissRequest = { showGoToPage = false },
            title = { Text("Go to page") },
            text = {
                OutlinedTextField(
                    value = goToPageText,
                    onValueChange = { goToPageText = it.filter { ch -> ch.isDigit() } },
                    placeholder = { Text("Page number (1 to ${toWesternDigits(doc.pageCount.toString())})") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    goToPageText.toIntOrNull()?.let { viewModel.goToPage(it - 1) }
                    goToPageText = ""
                    showGoToPage = false
                }) { Text("Go") }
            },
            dismissButton = {
                TextButton(onClick = { showGoToPage = false }) { Text("Cancel") }
            }
        )
    }
}
