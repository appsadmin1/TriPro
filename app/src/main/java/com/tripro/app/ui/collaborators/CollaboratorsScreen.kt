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
import com.tripro.app.ui.theme.HorizonEthosColors
import com.tripro.app.ui.theme.TriProSpacing
import com.tripro.app.util.localizedLabel

import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.border

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollaboratorsRoute(
    tripId: String,
    currentUid: String,
    currentUserName: String,
    onBack: () -> Unit
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.collaborators_title), style = MaterialTheme.typography.headlineMedium)
                        Text("Paris 2023", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back_cd)) } }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).background(MaterialTheme.colorScheme.surface)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(TriProSpacing.marginMobile),
                verticalArrangement = Arrangement.spacedBy(TriProSpacing.stackLg)
            ) {
                if (uiState.isOwner) {
                    item {
                        // Glass Panel Invite Section
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.7f)),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth().shadow(elevation = 2.dp, shape = RoundedCornerShape(12.dp))
                        ) {
                            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text(stringResource(R.string.collaborators_invite_title), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
                                Text(
                                    stringResource(R.string.collaborators_invite_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
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
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    modifier = Modifier.fillMaxWidth().height(48.dp)
                                ) {
                                    Icon(Icons.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp).padding(end = 8.dp))
                                    Text(stringResource(R.string.collaborators_send_invite), style = MaterialTheme.typography.labelLarge)
                                }

                                if (uiState.inviteError != null) {
                                    Text(uiState.inviteError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                }
                                if (uiState.inviteSuccessMessage != null) {
                                    Text(uiState.inviteSuccessMessage!!, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    item {
                        // Permissions Explainer
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                                .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Column {
                                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(stringResource(R.string.collaborators_editor_title), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                        Text(stringResource(R.string.collaborators_editor_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(stringResource(R.string.collaborators_read_only_title), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                        Text(stringResource(R.string.collaborators_read_only_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        stringResource(R.string.collaborators_current_members, uiState.members.size),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                items(uiState.members, key = { it.profile.uid }) { member ->
                    MemberRowItem(
                        displayName = member.profile.displayName,
                        email = member.profile.email,
                        photoUrl = member.profile.photoUrl,
                        role = member.role,
                        canManage = uiState.isOwner && member.role != Role.OWNER,
                        onRoleChange = { newRole -> viewModel.changeRole(member.profile.uid, newRole) },
                        onRemove = { viewModel.removeMember(member.profile.uid) }
                    )
                }

                if (uiState.pendingInvites.isNotEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.collaborators_pending_invites),
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(uiState.pendingInvites) { (pendingEmail, roleValue) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerLowest)
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.HourglassEmpty, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(pendingEmail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                    Text(
                                        stringResource(R.string.collaborators_invite_pending, Role.fromValue(roleValue).localizedLabel()),
                                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemberRowItem(
    displayName: String,
    email: String,
    photoUrl: String,
    role: Role,
    canManage: Boolean,
    onRoleChange: (Role) -> Unit,
    onRemove: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .border(1.dp, HorizonEthosColors.CardBorder, RoundedCornerShape(12.dp))
            .shadow(elevation = 2.dp, shape = RoundedCornerShape(12.dp))
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = photoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(48.dp).clip(CircleShape)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(displayName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
            }
        }

        if (canManage) {
            Box {
                AssistChip(
                    onClick = { menuExpanded = true },
                    label = { Text(role.localizedLabel()) }
                )
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(text = { Text(Role.EDITOR.localizedLabel()) }, onClick = { onRoleChange(Role.EDITOR); menuExpanded = false })
                    DropdownMenuItem(text = { Text(Role.VIEWER.localizedLabel()) }, onClick = { onRoleChange(Role.VIEWER); menuExpanded = false })
                    DropdownMenuItem(text = { Text(stringResource(R.string.collaborators_remove_menu_item)) }, onClick = { onRemove(); menuExpanded = false })
                }
            }
        } else {
            Text(
                role.localizedLabel(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}