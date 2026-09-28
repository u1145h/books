package com.u1145h.books.feature.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.u1145h.books.domain.model.ThemeMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            // Kavita Server Account
            SectionLabel("Kavita Server (eBooks)")
            InfoRow("Username", viewModel.username.ifBlank { "Not connected" })
            InfoRow("Server URL", viewModel.serverUrl.ifBlank { "Not configured" })

            val lastSync = settings.lastSyncAtUtc
            ClickableRow(
                label = "Last synced",
                value = if (lastSync == 0L) "Never (tap to sync)"
                else SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(lastSync)) + " (tap to sync)",
                onClick = { viewModel.syncNow() },
            )

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()

            // Audiobookshelf Server Account
            SectionLabel("Audiobookshelf Server (Audiobooks)")
            InfoRow("Status", if (viewModel.isAbsLoggedIn) "Connected" else "Not connected")
            InfoRow("Username", viewModel.absUsername.ifBlank { "None" })
            InfoRow("Server URL", viewModel.absServerUrl.ifBlank { "Not configured" })

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()

            // Appearance
            SectionLabel("Appearance")

            val nextTheme = when (settings.themeMode) {
                ThemeMode.SYSTEM -> ThemeMode.LIGHT
                ThemeMode.LIGHT -> ThemeMode.DARK
                ThemeMode.DARK -> ThemeMode.SYSTEM
            }
            val themeName = when (settings.themeMode) {
                ThemeMode.SYSTEM -> "Follow system"
                ThemeMode.LIGHT -> "Light"
                ThemeMode.DARK -> "Dark"
            }
            ClickableRow(
                label = "Theme",
                value = themeName,
                onClick = { viewModel.setThemeMode(nextTheme) },
            )

            SwitchRow(
                label = "Dynamic color",
                subtitle = "Material You wallpaper colors (Android 12+)",
                checked = settings.dynamicColor,
                onCheckedChange = { viewModel.setDynamicColor(it) },
            )

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()

            // Reader defaults
            SectionLabel("Reader defaults")

            SwitchRow(
                label = "Right-to-left",
                subtitle = "Manga reading direction",
                checked = settings.readerRtl,
                onCheckedChange = { viewModel.setReaderRtl(it) },
            )

            SwitchRow(
                label = "Webtoon mode",
                subtitle = "Vertical scroll instead of page swipes",
                checked = settings.readerWebtoon,
                onCheckedChange = { viewModel.setReaderWebtoon(it) },
            )

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()

            // Storage & Offline
            SectionLabel("Storage & Offline")
            val storageStats by viewModel.storageStats.collectAsStateWithLifecycle()
            InfoRow("Downloaded items", "${storageStats.bookCount} items")
            InfoRow("Disk space used", formatBytes(storageStats.totalBytes))
            if (storageStats.bookCount > 0) {
                ClickableRow(
                    label = "Clear all downloads",
                    value = "Delete",
                    onClick = { viewModel.clearDownloads() },
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(Locale.getDefault(), "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

@Composable
private fun SectionLabel(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ClickableRow(label: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun SwitchRow(
    label: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
