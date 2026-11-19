package com.scanpilot.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.scanpilot.app.util.PdfUtils
import com.scanpilot.app.util.ScanFilter
import com.scanpilot.app.viewmodel.ScanViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors

/**
 * Scanner with a live camera preview (CameraX) and a document-edge guide frame.
 * Filters are applied when saving the PDF: colour, black & white, greyscale.
 * If the camera is unavailable, a photo can also be picked from the gallery.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    viewModel: ScanViewModel,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    onOpenPro: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val photos by viewModel.photos.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val message by viewModel.message.collectAsState()
    val showProUpsell by viewModel.showProUpsell.collectAsState()

    var documentName by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(ScanFilter.COLOR) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var cameraBound by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        cameraBound = false
    }
    val pickImageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) viewModel.addPhoto(uri)
    }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    // Upgrade window — only when the free scan cap is used up
    if (showProUpsell) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissProUpsell() },
            title = { Text("Today's free scan limit is reached") },
            text = {
                Text(
                    "You've scanned 3 free documents today. With Pro, you get unlimited scans " +
                        "with no watermark."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.dismissProUpsell()
                    onOpenPro()
                }) { Text("View Pro") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissProUpsell() }) { Text("I'll come back tomorrow") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Scan document") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (!hasCameraPermission) {
                // Without camera permission, scanning from the gallery is still possible
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Camera permission is needed to scan with the camera.")
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }) {
                        Text("Grant camera permission")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { pickImageLauncher.launch("image/*") }) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Choose a photo from the gallery")
                    }
                }
            } else {
                // Live camera preview with a document-edge guide frame
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f / 4f)
                ) {
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).apply {
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        update = { previewView ->
                            if (!cameraBound) {
                                val providerFuture = ProcessCameraProvider.getInstance(context)
                                providerFuture.addListener(
                                    {
                                        try {
                                            val provider = providerFuture.get()
                                            val preview = Preview.Builder().build().also {
                                                it.setSurfaceProvider(previewView.surfaceProvider)
                                            }
                                            val capture = ImageCapture.Builder()
                                                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                                                .build()
                                            imageCapture = capture
                                            provider.unbindAll()
                                            provider.bindToLifecycle(
                                                lifecycleOwner,
                                                CameraSelector.DEFAULT_BACK_CAMERA,
                                                preview,
                                                capture
                                            )
                                            cameraBound = true
                                        } catch (e: Exception) {
                                            // Camera unavailable — the user scans from the gallery instead
                                        }
                                    },
                                    ContextCompat.getMainExecutor(context)
                                )
                            }
                        }
                    )
                    // Guide frame: place the document inside this frame
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(28.dp)
                            .border(3.dp, Color.White, RoundedCornerShape(12.dp))
                    )
                    Text(
                        "Place the document inside the frame",
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { pickImageLauncher.launch("image/*") }) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = "From gallery")
                    }
                    // Big capture button
                    IconButton(
                        onClick = {
                            val capture = imageCapture ?: return@IconButton
                            val photoFile = File(
                                File(context.cacheDir, "scan").apply { mkdirs() },
                                "photo_${System.currentTimeMillis()}.jpg"
                            )
                            val outputOptions = ImageCapture.OutputFileOptions
                                .Builder(photoFile)
                                .build()
                            capture.takePicture(
                                outputOptions,
                                cameraExecutor,
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                        val uri = FileProvider.getUriForFile(
                                            context,
                                            "${context.packageName}.fileprovider",
                                            photoFile
                                        )
                                        viewModel.addPhoto(uri)
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        // The error is shown via a message in the ViewModel
                                    }
                                }
                            )
                        },
                        modifier = Modifier
                            .size(72.dp)
                            .border(3.dp, Color(0xFF00695C), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Take a photo",
                            modifier = Modifier.size(36.dp),
                            tint = Color(0xFF00695C)
                        )
                    }
                    IconButton(
                        onClick = {
                            if (photos.isNotEmpty()) viewModel.removePhoto(photos.size - 1)
                        },
                        enabled = photos.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete last photo (retake)")
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Scan filters
            Text("Scan filter")
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                ScanFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = {
                            Text(
                                when (filter) {
                                    ScanFilter.COLOR -> "Colour"
                                    ScanFilter.BLACK_WHITE -> "Black & white"
                                    ScanFilter.GRAY -> "Greyscale"
                                }
                            )
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))

            // List of captured photos, with reorder and rotate
            if (photos.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(photos) { index, photo ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PhotoThumbnail(uri = photo.uri, rotation = photo.rotation)
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Page ${index + 1}")
                                if (photo.rotation != 0) {
                                    Text("Rotation: ${photo.rotation}°")
                                }
                            }
                            IconButton(onClick = { viewModel.movePhotoUp(index) }) {
                                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Up")
                            }
                            IconButton(onClick = { viewModel.movePhotoDown(index) }) {
                                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Down")
                            }
                            IconButton(onClick = { viewModel.rotatePhoto(index) }) {
                                Icon(Icons.Default.RotateRight, contentDescription = "Rotate")
                            }
                            IconButton(onClick = { viewModel.removePhoto(index) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete")
                            }
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(12.dp))
            OutlinedTextField(
                value = documentName,
                onValueChange = { documentName = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Document name (e.g.: Rental contract)") },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { viewModel.saveAsPdf(documentName, selectedFilter, onSaved) },
                enabled = photos.isNotEmpty() && !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("Save as PDF")
                }
            }
        }
    }
}

/** Thumbnail of one scanned photo. */
@Composable
private fun PhotoThumbnail(uri: Uri, rotation: Int) {
    var bitmap by remember(uri, rotation) { mutableStateOf<Bitmap?>(null) }
    val context = LocalContext.current
    LaunchedEffect(uri, rotation) {
        bitmap = withContext(Dispatchers.IO) {
            PdfUtils.loadBitmapScaled(context, uri, 480)?.let {
                PdfUtils.rotateBitmap(it, rotation)
            }
        }
    }
    Box(
        modifier = Modifier.size(96, 128),
        contentAlignment = Alignment.Center
    ) {
        val image = bitmap
        if (image != null) {
            Image(
                bitmap = image.asImageBitmap(),
                contentDescription = "Scanned photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
        }
    }
}
