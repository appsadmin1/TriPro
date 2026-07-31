package com.tripro.app.ui.components

import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.tripro.app.BuildConfig
import com.tripro.app.data.model.Attachment
import com.tripro.app.util.FileDownloader
import com.tripro.app.util.PdfPageRenderer
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachmentViewerDialog(
    attachment: Attachment,
    onDismiss: () -> Unit,
    onRemove: (() -> Unit)? = null,
    onRename: ((String) -> Unit)? = null
) {
    val context = LocalContext.current
    var renaming by remember { mutableStateOf(false) }
    var currentName by remember(attachment.id) { mutableStateOf(attachment.fileName) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(modifier = Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text(currentName, maxLines = 1) },
                navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.Filled.Close, contentDescription = "Close") } },
                actions = {
                    if (onRename != null) {
                        IconButton(onClick = { renaming = true }) { Icon(Icons.Filled.Edit, contentDescription = "Rename file") }
                    }
                    IconButton(onClick = { FileDownloader.downloadToDeviceDownloads(context, attachment.downloadUrl, currentName) }) {
                        Icon(Icons.Filled.Download, contentDescription = "Download")
                    }
                    if (onRemove != null) {
                        IconButton(onClick = { onRemove(); onDismiss() }) {
                            Icon(Icons.Filled.Delete, contentDescription = "Remove from itinerary item", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest)
            )
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                when {
                    attachment.mimeType.startsWith("image/") -> ZoomableImage(url = attachment.downloadUrl)
                    attachment.mimeType == "application/pdf" -> PdfViewer(attachment = attachment)
                    else -> GenericFileFallback(attachment = attachment)
                }
            }
        }
    }

    if (renaming) {
        AlertDialog(
            onDismissRequest = { renaming = false },
            title = { Text("Rename file") },
            text = {
                var text by remember { mutableStateOf(currentName) }
                OutlinedTextField(value = text, onValueChange = { text = it; currentName = it }, singleLine = true, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                TextButton(onClick = { onRename?.invoke(currentName); renaming = false }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { renaming = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun ZoomableImage(url: String) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    AsyncImage(
        model = url, contentDescription = null, contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxSize()
            .pointerInput(Unit) { detectTransformGestures { _, pan, zoom, _ -> scale = (scale * zoom).coerceIn(1f, 5f); offsetX += pan.x; offsetY += pan.y } }
            .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offsetX, translationY = offsetY)
    )
}

@Composable
private fun PdfViewer(attachment: Attachment) {
    val context = LocalContext.current
    var pages by remember { mutableStateOf<List<Bitmap>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(attachment.downloadUrl) {
        try {
            val file = FileDownloader.downloadToCache(context, attachment.downloadUrl, attachment.fileName)
            pages = PdfPageRenderer.renderPages(file)
        } catch (e: Exception) {
            error = e.message ?: "Couldn't open this PDF"
        }
    }

    when {
        error != null -> Text(error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(24.dp))
        pages == null -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Text("Opening PDF…", modifier = Modifier.padding(top = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        else -> LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(pages!!) { bitmap -> Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxWidth()) }
        }
    }
}

@Composable
private fun GenericFileFallback(attachment: Attachment) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
        Icon(Icons.Filled.InsertDriveFile, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(bottom = 16.dp))
        Text("Preview isn't available for this file type in-app.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Button(
            onClick = {
                scope.launch {
                    val file = FileDownloader.downloadToCache(context, attachment.downloadUrl, attachment.fileName)
                    val uri = FileProvider.getUriForFile(context, "${BuildConfig.APPLICATION_ID}.fileprovider", file)
                    val intent = Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri, attachment.mimeType.ifBlank { "*/*" }); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION) }
                    context.startActivity(Intent.createChooser(intent, "Open with"))
                }
            },
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text("Open with…")
        }
    }
}