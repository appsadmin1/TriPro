package com.tripro.app.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import com.tripro.app.R
import com.tripro.app.navigation.Destinations
import com.tripro.app.util.AppLanguage
import com.tripro.app.util.currentAppLanguage
import com.tripro.app.util.setAppLanguage
import com.tripro.app.util.recreateActivity

@Composable
fun AppDrawerContent(
    userName: String,
    userEmail: String,
    userPhotoUrl: String,
    onNavigate: (String) -> Unit,
    onInviteFriends: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    var currentLanguage by remember { mutableStateOf(currentAppLanguage(context)) }

    ModalDrawerSheet {
        Column(modifier = Modifier.fillMaxHeight().padding(vertical = 24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 24.dp)) {
                AsyncImage(
                    model = userPhotoUrl,
                    contentDescription = null,
                    modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(userName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                    Text(userEmail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            DrawerItem(stringResource(R.string.drawer_main_page), Icons.Filled.Explore) { onNavigate(Destinations.tripsList()) }
            DrawerItem(stringResource(R.string.drawer_profile), Icons.Filled.Person) { onNavigate(Destinations.PROFILE) }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            DrawerItem(stringResource(R.string.drawer_settings), Icons.Filled.Settings) { onNavigate(Destinations.SETTINGS) }

            Text(
                stringResource(R.string.drawer_language),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 28.dp, top = 12.dp, bottom = 4.dp)
            )
            AppLanguage.entries.forEach { language ->
                NavigationDrawerItem(
                    label = { Text(language.displayName) },
                    selected = language == currentLanguage,
                    icon = { Icon(Icons.Filled.Language, contentDescription = null) },
                    badge = {
                        if (language == currentLanguage) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    onClick = {
                        if (language != currentLanguage) {
                            currentLanguage = language
                            setAppLanguage(context, language)
                            recreateActivity(context)
                        }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )
            }

            DrawerItem(stringResource(R.string.drawer_invite_friends), Icons.Filled.PersonAdd, onClick = onInviteFriends)

            Spacer(Modifier.weight(1f))
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            DrawerItem(stringResource(R.string.drawer_log_out), Icons.Filled.ExitToApp, onClick = onSignOut)
        }
    }
}

@Composable
private fun DrawerItem(label: String, icon: ImageVector, onClick: () -> Unit) {
    NavigationDrawerItem(
        label = { Text(label) },
        selected = false,
        icon = { Icon(icon, contentDescription = null) },
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 12.dp)
    )
}