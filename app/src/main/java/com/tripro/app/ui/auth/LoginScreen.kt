package com.tripro.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.FlightTakeoff
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tripro.app.R
import com.tripro.app.ui.theme.TriProSpacing
import com.tripro.app.util.AppLanguage
import com.tripro.app.util.currentAppLanguage
import com.tripro.app.util.setAppLanguage

@Composable
fun LoginScreen(
    uiState: AuthUiState,
    onSignInClick: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isWideScreen = configuration.screenWidthDp > 840
    val context = LocalContext.current
    val currentLang = currentAppLanguage(context)

    Row(modifier = Modifier.fillMaxSize()) {
        // Left Side: Form
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = if (isWideScreen) 64.dp else TriProSpacing.marginMobile)
                .padding(vertical = 24.dp),
            horizontalAlignment = if (isWideScreen) Alignment.Start else Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = if (isWideScreen) Arrangement.End else Arrangement.Center
            ) {
                androidx.compose.runtime.CompositionLocalProvider(
                    androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Ltr
                ) {
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { expanded = true }) {
                            Icon(
                                imageVector = Icons.Filled.Language,
                                contentDescription = stringResource(R.string.drawer_language),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            AppLanguage.entries.forEach { lang ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            lang.displayName,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (lang == currentLang) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        setAppLanguage(context, lang)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(Modifier.weight(1f))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Explore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(36.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(48.dp))

            Text(
                stringResource(R.string.login_hero_title),
                style = MaterialTheme.typography.displayLarge.copy(lineHeight = 56.sp),
                color = MaterialTheme.colorScheme.primary,
                textAlign = if (isWideScreen) TextAlign.Start else TextAlign.Center
            )

            Spacer(Modifier.height(24.dp))

            Text(
                stringResource(R.string.login_hero_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = if (isWideScreen) TextAlign.Start else TextAlign.Center,
                modifier = Modifier.width(if (isWideScreen) 400.dp else 320.dp)
            )

            Spacer(Modifier.height(48.dp))

            val isSigningIn = uiState is AuthUiState.SigningIn
            Button(
                onClick = onSignInClick,
                enabled = !isSigningIn,
                shape = RoundedCornerShape(percent = 50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                modifier = Modifier
                    .width(320.dp)
                    .height(52.dp)
                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(percent = 50))
            ) {
                if (isSigningIn) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        stringResource(R.string.login_sign_in_google),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (uiState is AuthUiState.SigningIn && uiState.error != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    uiState.error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center
                )
            }
            
            Spacer(Modifier.weight(1f))
        }

        // Right Side: Hero (Desktop only)
        if (isWideScreen) {
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Background Image Placeholder
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary)
                                )
                            )
                    )
                    
                    // Floating Glass Element
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(48.dp)
                            .width(320.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.3f))
                            .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .padding(24.dp)
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.FlightTakeoff, contentDescription = null, tint = Color.White)
                                Spacer(Modifier.width(12.dp))
                                Text("Next Adventure", style = MaterialTheme.typography.labelLarge, color = Color.White)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.login_quote),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
