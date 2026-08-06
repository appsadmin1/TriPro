package com.tripro.app.ui.tripoverview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.UnfoldLess
import androidx.compose.material.icons.filled.UnfoldMore
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.tripro.app.R
import com.tripro.app.TriProApplication
import com.tripro.app.data.model.Attachment
import com.tripro.app.ui.components.AttachmentViewerDialog
import com.tripro.app.ui.theme.TriProColors
import com.tripro.app.ui.theme.TriProSpacing
import com.tripro.app.util.DateUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDocsRoute(tripId: String, onBack: () -> Unit) {
    val app = LocalContext.current.applicationContext as TriProApplication
    val container = app.container
    val viewModel: TripDocsViewModel = viewModel(factory = viewModelFactory { initializer { TripDocsViewModel(container.tripRepository, tripId) } })
    val uiState by viewModel.uiState.collectAsState()
    var viewing by remember { mutableStateOf<Triple<String, String, Attachment>?>(null) }
    
    val allExpanded = uiState.expandedDates.size == uiState.docsByDate.size && uiState.docsByDate.isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.trip_docs_title, uiState.tripName)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back_cd)) } },
                actions = {
                    if (uiState.docsByDate.isNotEmpty()) {
                        IconButton(onClick = { if (allExpanded) viewModel.collapseAll() else viewModel.expandAll() }) {
                            Icon(
                                if (allExpanded) Icons.Filled.UnfoldLess else Icons.Filled.UnfoldMore,
                                contentDescription = if (allExpanded) stringResource(R.string.trip_docs_collapse_all) else stringResource(R.string.trip_docs_expand_all)
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        if (uiState.docsByDate.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.trip_docs_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(TriProSpacing.marginMobile),
            verticalArrangement = Arrangement.spacedBy(TriProSpacing.stackMd)
        ) {
            uiState.docsByDate.forEach { (date, docs) ->
                val isExpanded = uiState.expandedDates.contains(date)
                item(key = date) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleDateExpanded(date) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(DateUtils.formatFullDayLabel(date), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                        Icon(
                            if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (isExpanded) {
                    items(docs, key = { "${it.itemId}-${it.attachment.id}" }) { doc ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                                .border(1.dp, TriProColors.CardBorder, RoundedCornerShape(12.dp))
                                .clickable { viewing = Triple(doc.date, doc.itemId, doc.attachment) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.InsertDriveFile, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                                Text(doc.attachment.fileName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                Text(doc.itemTitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }

    viewing?.let { (date, itemId, attachment) ->
        AttachmentViewerDialog(
            attachment = attachment,
            onDismiss = { viewing = null },
            onRename = { newName -> viewModel.renameAttachment(date, itemId, attachment, newName) }
        )
    }
}