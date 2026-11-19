package com.scanpilot.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.scanpilot.app.data.DocumentEntity
import com.scanpilot.app.util.toWesternDigits
import com.scanpilot.app.viewmodel.LibraryViewModel
import com.scanpilot.app.viewmodel.UiMessage

/**
 * Tools screen — the same tools as the reference app, with an English description for each tool.
 * Tools that need a file first ask you to pick a document from the library.
 */
@Composable
fun ToolsScreen(
    viewModel: LibraryViewModel,
    onToolClick: (HomeTool) -> Unit
) {
    val documents by viewModel.documents.collectAsState()
    val message by viewModel.message.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var splitTarget by remember { mutableStateOf<DocumentEntity?>(null) }
    var splitFrom by remember { mutableStateOf("1") }
    var splitTo by remember { mutableStateOf("") }
    var lockTarget by remember { mutableStateOf<DocumentEntity?>(null) }
    var lockPassword by remember { mutableStateOf("") }
    var exportTarget by remember { mutableStateOf<DocumentEntity?>(null) }
    var pickFor by remember { mutableStateOf<HomeTool?>(null) }

    LaunchedEffect(message) {
        val msg = message as? UiMessage.Text ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg.text)
        viewModel.clearMessage()
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("All tools", fontSize = 20.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Tap a tool; if it needs a document, you will pick it first.",
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(16.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(HomeTool.entries.toList()) { tool ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clickable {
                            when (tool) {
                                HomeTool.SPLIT, HomeTool.LOCK, HomeTool.PDF_TO_IMAGE -> {
                                    pickFor = tool
                                }
                                else -> onToolClick(tool)
                            }
                        }
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
                        Text(tool.label)
                        Text(
                            text = when (tool) {
                                HomeTool.SCAN -> "Take a photo, get a PDF"
                                HomeTool.READ -> "Read and search documents"
                                HomeTool.EDIT -> "Highlight and annotate"
                                HomeTool.MERGE -> "Combine several PDFs into one"
                                HomeTool.SPLIT -> "Split out a few pages"
                                HomeTool.IMAGE_TO_PDF -> "Turn gallery photos into a PDF"
                                HomeTool.PDF_TO_IMAGE -> "Turn pages into images"
                                HomeTool.LOCK -> "Lock a document with a password"
                                HomeTool.SIGN -> "Place your signature on a document"
                                HomeTool.SUMMARY -> "Summarize the document"
                            },
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }

    // Pick a document for tools that need a file
    pickFor?.let { tool ->
        AlertDialog(
            onDismissRequest = { pickFor = null },
            title = { Text("Choose a document") },
            text = {
                if (documents.isEmpty()) {
                    Text("You don't have any documents yet. Import or scan one first.")
                } else {
                    LazyColumn {
                        items(documents) { doc ->
                            TextButton(
                                onClick = {
                                    pickFor = null
                                    when (tool) {
                                        HomeTool.SPLIT -> {
                                            splitTarget = doc
                                            splitFrom = "1"
                                            splitTo = doc.pageCount.toString()
                                        }
                                        HomeTool.LOCK -> lockTarget = doc
                                        HomeTool.PDF_TO_IMAGE -> exportTarget = doc
                                        else -> Unit
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("${doc.fileName} (${toWesternDigits(doc.pageCount.toString())} pages)")
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { pickFor = null }) { Text("Close") }
            }
        )
    }

    // Split PDF dialog
    splitTarget?.let { doc ->
        AlertDialog(
            onDismissRequest = { splitTarget = null },
            title = { Text("Split “${doc.fileName}”") },
            text = {
                Column {
                    Text("Which pages should be split out? (1 to ${toWesternDigits(doc.pageCount.toString())})")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = splitFrom,
                        onValueChange = { splitFrom = it.filter { ch -> ch.isDigit() } },
                        label = { Text("From page") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = splitTo,
                        onValueChange = { splitTo = it.filter { ch -> ch.isDigit() } },
                        label = { Text("To page") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val from = splitFrom.toIntOrNull() ?: 1
                    val to = splitTo.toIntOrNull() ?: doc.pageCount
                    viewModel.splitDocument(doc.id, from, to)
                    splitTarget = null
                }) { Text("Split") }
            },
            dismissButton = {
                TextButton(onClick = { splitTarget = null }) { Text("Cancel") }
            }
        )
    }

    // Lock with password dialog
    lockTarget?.let { doc ->
        AlertDialog(
            onDismissRequest = { lockTarget = null },
            title = { Text("Lock “${doc.fileName}” with a password") },
            text = {
                Column {
                    Text("Enter a password. Without this password the document cannot be opened, so write it down somewhere safe.")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = lockPassword,
                        onValueChange = { lockPassword = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.lockDocument(doc.id, lockPassword)
                        lockPassword = ""
                        lockTarget = null
                    },
                    enabled = lockPassword.length >= 4
                ) { Text("Lock") }
            },
            dismissButton = {
                TextButton(onClick = { lockTarget = null }) { Text("Cancel") }
            }
        )
    }

    // Convert PDF to images dialog
    exportTarget?.let { doc ->
        AlertDialog(
            onDismissRequest = { exportTarget = null },
            title = { Text("Convert to images") },
            text = { Text("All pages of “${doc.fileName}” will be converted to images and prepared for sharing.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.exportPdfToImages(doc.id)
                    exportTarget = null
                }) { Text("Convert") }
            },
            dismissButton = {
                TextButton(onClick = { exportTarget = null }) { Text("Cancel") }
            }
        )
    }
}
