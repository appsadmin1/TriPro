package com.tripro.app.ui.tripdocuments

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.initializer
import com.tripro.app.TriProApplication
import com.tripro.app.data.model.Attachment
import com.tripro.app.ui.components.AttachmentViewerDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDocumentsRoute(tripId: String, onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as TriProApplication
    val viewModel: TripDocumentsViewModel = viewModel(
        factory = viewModelFactory { initializer { TripDocumentsViewModel(app.container.tripRepository, tripId) } }
    )
    val entries by viewModel.entries.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var viewing by remember { mutableStateOf<Attachment?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Documents") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        when {
            isLoading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            entries.isEmpty() -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No documents uploaded yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(entries) { entry ->
                    ListItem(
                        headlineContent = { Text(entry.attachment.fileName) },
                        supportingContent = { Text("${entry.itemTitle} · ${entry.date}") },
                        leadingContent = { Icon(Icons.Filled.InsertDriveFile, contentDescription = null) },
                        modifier = Modifier.clickable { viewing = entry.attachment }
                    )
                }
            }
        }
    }

    viewing?.let { attachment ->
        AttachmentViewerDialog(attachment = attachment, onDismiss = { viewing = null })
    }
}