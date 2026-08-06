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
import com.tripro.app.ui.components.TriProTextField
import com.tripro.app.ui.theme.PillShape
import com.tripro.app.ui.theme.TriProTypography
import com.tripro.app.ui.theme.TriProColors
import com.tripro.app.ui.theme.TriProSpacing
import com.tripro.app.util.localizedLabel

import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.unit.sp
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
    var memberToManage by remember { mutableStateOf<MemberRow?>(null) }

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
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = TriProColors.SurfaceContainerLowest),
                            border = BorderStroke(1.dp, TriProColors.CardBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text(
                                    stringResource(R.string.collaborators_invite_title),
                                    style = TriProTypography.headlineSmall,
                                    color = TriProColors.Primary
                                )
                                TriProTextField(
                                    value = email,
                                    onValueChange = { email = it },
                                    label = stringResource(R.string.collaborators_email_label),
                                    placeholder = "name@example.com",
                                    leadingIcon = { Icon(Icons.Filled.Mail, contentDescription = null, tint = TriProColors.Outline) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Button(
                                    onClick = { viewModel.invite(email, inviteRole); email = "" },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = TriProColors.SecondaryContainer,
                                        contentColor = TriProColors.OnSecondaryContainer
                                    ),
                                    modifier = Modifier.fillMaxWidth().height(56.dp)
                                ) {
                                    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
                                    Icon(
                                        Icons.Filled.Send,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp).padding(end = 8.dp).let {
                                            if (isRtl) it.scale(scaleX = -1f, scaleY = 1f) else it
                                        }
                                    )
                                    Text(stringResource(R.string.collaborators_send_invite), style = TriProTypography.labelLarge)
                                }
                                if (uiState.inviteError != null) Text(uiState.inviteError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                if (uiState.inviteSuccessMessage != null) Text(uiState.inviteSuccessMessage!!, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    item {
                        PermissionsExplainer()
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
                        onClick = { if (uiState.isOwner && member.role != Role.OWNER) memberToManage = member }
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
        com.tripro.app.ui.components.TriProAlertDialog(
            onDismissRequest = { memberToRemove = null },
            title = stringResource(R.string.collaborators_delete_confirm_title),
            text = stringResource(R.string.collaborators_delete_confirm_text, name),
            confirmButtonText = stringResource(R.string.action_delete),
            onConfirm = { viewModel.removeMember(uid); memberToRemove = null },
            dismissButtonText = stringResource(R.string.action_cancel),
            onDismiss = { memberToRemove = null },
            isDestructive = true
        )
    }

    memberToManage?.let { member ->
        RoleSelectionDialog(
            currentRole = member.role,
            onDismiss = { memberToManage = null },
            onRoleSelected = { newRole ->
                viewModel.changeRole(member.profile.uid, newRole)
                memberToManage = null
            },
            onRemove = {
                memberToRemove = member.profile.uid to member.profile.displayName
                memberToManage = null
            }
        )
    }
}

@Composable
private fun RoleSelectionDialog(
    currentRole: Role,
    onDismiss: () -> Unit,
    onRoleSelected: (Role) -> Unit,
    onRemove: () -> Unit
) {
    com.tripro.app.ui.components.TriProDialog(onDismissRequest = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                stringResource(R.string.collaborators_permission_label),
                style = TriProTypography.headlineSmall,
                color = TriProColors.Primary
            )
            
            RoleOptionBox(
                title = stringResource(R.string.collaborators_editor_title),
                description = stringResource(R.string.collaborators_editor_desc),
                isSelected = currentRole == Role.EDITOR,
                onClick = { onRoleSelected(Role.EDITOR) }
            )
            
            RoleOptionBox(
                title = stringResource(R.string.collaborators_read_only_title),
                description = stringResource(R.string.collaborators_read_only_desc),
                isSelected = currentRole == Role.VIEWER,
                onClick = { onRoleSelected(Role.VIEWER) }
            )
            
            Spacer(Modifier.height(8.dp))
            
            Button(
                onClick = onRemove,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text(stringResource(R.string.collaborators_remove_menu_item), style = TriProTypography.labelMedium)
            }
        }
    }
}

@Composable
private fun RoleOptionBox(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) TriProColors.Primary else TriProColors.CardBorder
    val bgColor = if (isSelected) TriProColors.Primary.copy(alpha = 0.05f) else Color.Transparent
    
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(if (isSelected) 2.dp else 1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = TriProTypography.labelMedium, color = TriProColors.Primary)
                Text(description, style = TriProTypography.bodySmall, color = TriProColors.OnSurfaceVariant)
            }
            if (isSelected) {
                Icon(Icons.Filled.Check, contentDescription = null, tint = TriProColors.Primary)
            }
        }
    }
}

@Composable
private fun PermissionsExplainer() {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = TriProColors.SurfaceContainerLow),
        border = BorderStroke(1.dp, TriProColors.CardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(TriProColors.Primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Info, contentDescription = null, tint = TriProColors.Primary, modifier = Modifier.size(18.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.collaborators_editor_title), style = TriProTypography.labelMedium, color = TriProColors.Primary)
                        Text(stringResource(R.string.collaborators_editor_desc), style = TriProTypography.bodySmall, color = TriProColors.OnSurfaceVariant)
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.collaborators_read_only_title), style = TriProTypography.labelMedium, color = TriProColors.Primary)
                        Text(stringResource(R.string.collaborators_read_only_desc), style = TriProTypography.bodySmall, color = TriProColors.OnSurfaceVariant)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MemberRowItem(
    displayName: String,
    photoUrl: String,
    role: Role,
    canManage: Boolean,
    onClick: () -> Unit
) {
    val label = role.localizedLabel()

    val cardContent = @Composable {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                AsyncImage(
                    model = photoUrl.takeIf { it.isNotBlank() },
                    contentDescription = null, contentScale = ContentScale.Crop,
                    modifier = Modifier.size(48.dp).clip(CircleShape).border(2.dp, TriProColors.SurfaceContainerLowest, CircleShape).shadow(1.dp, CircleShape),
                    error = androidx.compose.ui.graphics.painter.ColorPainter(MaterialTheme.colorScheme.primaryContainer),
                    fallback = androidx.compose.ui.graphics.painter.ColorPainter(MaterialTheme.colorScheme.primaryContainer)
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    Text(displayName, style = TriProTypography.headlineSmall.copy(fontSize = 16.sp), color = TriProColors.Primary)
                }
            }
            if (canManage) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .border(1.dp, TriProColors.OutlineVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(label, style = TriProTypography.labelMedium, color = TriProColors.Primary)
                    Icon(Icons.Filled.ExpandMore, contentDescription = null, modifier = Modifier.padding(start = 4.dp).size(18.dp), tint = TriProColors.Primary)
                }
            } else {
                Text(
                    label,
                    style = TriProTypography.labelMedium,
                    color = TriProColors.OnSurfaceVariant,
                    modifier = Modifier.clip(PillShape).background(TriProColors.SurfaceContainer).padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
    }

    if (canManage) {
        Card(
            onClick = onClick,
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = TriProColors.SurfaceContainerLowest),
            border = BorderStroke(1.dp, TriProColors.CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            cardContent()
        }
    } else {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = TriProColors.SurfaceContainerLowest),
            border = BorderStroke(1.dp, TriProColors.CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            cardContent()
        }
    }
}
