package com.tripro.app.ui.collaborators

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.lifecycle.viewmodel.initializer
import coil.compose.AsyncImage
import com.tripro.app.R
import com.tripro.app.TriProApplication
import com.tripro.app.data.model.Role
import com.tripro.app.ui.theme.TriProColors
import com.tripro.app.ui.theme.TriProSpacing
import com.tripro.app.util.localizedLabel

import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.border
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.draw.scale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollaboratorsRoute(
    tripId: String,
    currentUid: String,
    currentUserName: String,
    onBack: () -> Unit,
    onOpenDrawer: () -> Unit
) {
    val app = LocalContext.current.applicationContext as TriProApplication
    val container = app.container
    val viewModel: CollaboratorsViewModel = viewModel(
        factory = viewModelFactory {
            initializer { CollaboratorsViewModel(container.tripRepository, container.userRepository, container.pushNotificationRepository, container.activityRepository, tripId, currentUid, currentUserName) }
        }
    )
    val uiState by viewModel.uiState.collectAsState()

    var email by remember { mutableStateOf("") }
    var inviteRole by remember { mutableStateOf(Role.EDITOR) }
    var memberToRemove by remember { mutableStateOf<Pair<String, String>?>(null) } // uid to name

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.collaborators_title), style = MaterialTheme.typography.headlineMedium)
                        Text(uiState.trip?.name ?: "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    Row {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.nav_menu_cd))
                        }
                        IconButton(onClick = onBack) {
                            val backIcon = if (LocalLayoutDirection.current == LayoutDirection.Rtl) Icons.Filled.ArrowForward else Icons.Filled.ArrowBack
                            Icon(backIcon, contentDescription = stringResource(R.string.common_back_cd))
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.surface)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (uiState.isOwner) {
                    item {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.7f)),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth().shadow(elevation = 2.dp, shape = RoundedCornerShape(12.dp))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(stringResource(R.string.collaborators_invite_title), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                                OutlinedTextField(
                                    value = email,
                                    onValueChange = { email = it },
                                    placeholder = { Text(stringResource(R.string.collaborators_email_label)) },
                                    leadingIcon = { Icon(Icons.Filled.Mail, contentDescription = null, tint = MaterialTheme.colorScheme.outline) },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                Button(
                                    onClick = { viewModel.invite(email, inviteRole); email = "" },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
                                    Icon(
                                        Icons.Filled.Send, 
                                        contentDescription = null, 
                                        modifier = Modifier.size(18.dp).padding(end = 8.dp).let {
                                            if (isRtl) it.scale(scaleX = -1f, scaleY = 1f) else it
                                        }
                                    )
                                    Text(stringResource(R.string.collaborators_send_invite), style = MaterialTheme.typography.labelLarge)
                                }
                                if (uiState.inviteError != null) Text(uiState.inviteError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                if (uiState.inviteSuccessMessage != null) Text(uiState.inviteSuccessMessage!!, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                item {
                    Text(
                        stringResource(R.string.collaborators_current_members, uiState.members.size),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                items(uiState.members, key = { it.profile.uid }) { member ->
                    MemberRowItem(
                        displayName = member.profile.displayName,
                        photoUrl = member.profile.photoUrl,
                        role = member.role,
                        canManage = uiState.isOwner && member.role != Role.OWNER,
                        onRoleChange = { newRole -> viewModel.changeRole(member.profile.uid, newRole) },
                        onRemove = { memberToRemove = member.profile.uid to member.profile.displayName }
                    )
                }

                if (uiState.pendingInvites.isNotEmpty()) {
                    item {
                        Text(stringResource(R.string.collaborators_pending_invites), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    items(uiState.pendingInvites) { (pendingEmail, roleValue) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerLowest).padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.HourglassEmpty, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(pendingEmail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                    Text(stringResource(R.string.collaborators_invite_pending, Role.fromValue(roleValue).localizedLabel()), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
            if (uiState.isLoading) Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        }
    }

    memberToRemove?.let { (uid, name) ->
        AlertDialog(
            onDismissRequest = { memberToRemove = null },
            title = { Text(stringResource(R.string.collaborators_delete_confirm_title)) },
            text = { Text(stringResource(R.string.collaborators_delete_confirm_text, name)) },
            confirmButton = {
                TextButton(onClick = { viewModel.removeMember(uid); memberToRemove = null }) {
                    Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { memberToRemove = null }) { Text(stringResource(R.string.action_cancel)) } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemberRowItem(
    displayName: String,
    photoUrl: String,
    role: Role,
    canManage: Boolean,
    onRoleChange: (Role) -> Unit,
    onRemove: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceContainerLowest).border(1.dp, TriProColors.CardBorder, RoundedCornerShape(12.dp)).padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = photoUrl.takeIf { it.isNotBlank() },
                contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
                error = androidx.compose.ui.graphics.painter.ColorPainter(MaterialTheme.colorScheme.primaryContainer),
                fallback = androidx.compose.ui.graphics.painter.ColorPainter(MaterialTheme.colorScheme.primaryContainer)
            )
            Spacer(Modifier.width(12.dp))
            Text(displayName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        }
        if (canManage) {
            Box {
                AssistChip(onClick = { menuExpanded = true }, label = { Text(role.localizedLabel()) })
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(text = { Text(Role.EDITOR.localizedLabel()) }, onClick = { onRoleChange(Role.EDITOR); menuExpanded = false })
                    DropdownMenuItem(text = { Text(Role.VIEWER.localizedLabel()) }, onClick = { onRoleChange(Role.VIEWER); menuExpanded = false })
                    DropdownMenuItem(text = { Text(stringResource(R.string.collaborators_remove_menu_item)) }, onClick = { onRemove(); menuExpanded = false })
                }
            }
        } else {
            Text(role.localizedLabel(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
