@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.nidhaan.app.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.nidhaan.app.Graph
import com.nidhaan.app.R
import com.nidhaan.app.data.BackupManager
import kotlinx.coroutines.launch

// -------------------------------------------------------------------- welcome

@Composable
fun WelcomeScreen(onStartFresh: () -> Unit, onRestored: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val openLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            busy = true
            error = null
            scope.launch {
                val result = BackupManager.restoreFromUri(ctx, uri)
                busy = false
                result.onSuccess { onRestored() }
                result.onFailure { error = "That file could not be restored: ${it.message ?: "not a valid Nidhaan backup"}" }
            }
        }
    }

    Scaffold { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().padding(28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier.size(112.dp).clip(RoundedCornerShape(28.dp)).background(Color(0xFF00695C)),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().scale(1.5f)
                )
            }
            Spacer(Modifier.height(20.dp))
            Text("Nidhaan", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            Text("निधान", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.height(6.dp))
            Text(
                "Your things, in place",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(36.dp))

            Button(
                onClick = onStartFresh, enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)
            ) { Text("Start fresh") }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = { openLauncher.launch(arrayOf("*/*")) }, enabled = !busy,
                modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.Backup, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Restore from backup file")
            }
            if (busy) {
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text("Restoring photos and data...", style = MaterialTheme.typography.bodySmall)
            }
            error?.let {
                Spacer(Modifier.height(14.dp))
                Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "Reinstalling or changing phone? Choose the nidhaan_backup JSON file you saved earlier.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center
            )
        }
    }
}

// --------------------------------------------------------------------- backup

@Composable
fun BackupScreen(onTab: (String) -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var lastTime by remember { mutableStateOf(Graph.prefs.lastBackup) }
    var lastName by remember { mutableStateOf(Graph.prefs.lastBackupName) }
    var pendingRestore by remember { mutableStateOf<Uri?>(null) }

    fun report(ok: Boolean, text: String) {
        isError = !ok
        message = text
        lastTime = Graph.prefs.lastBackup
        lastName = Graph.prefs.lastBackupName
    }

    val createLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            busy = true
            scope.launch {
                runCatching { BackupManager.exportToUri(ctx, uri) }
                    .onSuccess { report(true, "Backup saved: ${it.describe()}.") }
                    .onFailure { report(false, "Backup failed: ${it.message}") }
                busy = false
            }
        }
    }
    val openLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pendingRestore = uri
    }

    Scaffold(
        topBar = { AppTopBar("Backup & Restore") },
        bottomBar = { BottomTabs("backup", onTab) }
    ) { pad ->
        LazyColumn(
            modifier = Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text(
                    "Photos and data live only inside Nidhaan. Android deletes them when the app is uninstalled, " +
                        "so save a backup file to your phone first.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Export
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Back up to this phone", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "One JSON file with locations, containers, items and all photos.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            Button(
                                onClick = {
                                    busy = true
                                    scope.launch {
                                        runCatching { BackupManager.quickBackupToDownloads(ctx) }
                                            .onSuccess { (name, counts) ->
                                                report(true, "Saved to Downloads/Nidhaan/$name (${counts.describe()}).")
                                            }
                                            .onFailure { report(false, "Backup failed: ${it.message}") }
                                        busy = false
                                    }
                                },
                                enabled = !busy,
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                shape = RoundedCornerShape(14.dp)
                            ) { Text("Quick backup to Downloads") }
                        }
                        OutlinedButton(
                            onClick = { createLauncher.launch(BackupManager.defaultFileName()) },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("Choose where to save...") }
                        if (lastTime > 0L) {
                            Text(
                                "Last backup: ${formatTime(lastTime)}" + if (lastName.isNotBlank()) "\n$lastName" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        } else {
                            Text("No backup yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                }
            }

            // Restore
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Restore from a backup file", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "Replaces everything in the app with the content of the backup file you pick.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = { openLauncher.launch(arrayOf("*/*")) },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("Restore...") }
                    }
                }
            }

            if (busy) {
                item {
                    Column {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        Text("Working... please keep the app open.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            message?.let { m ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isError) MaterialTheme.colorScheme.errorContainer
                            else MaterialTheme.colorScheme.primaryContainer
                        )
                    ) { Text(m, Modifier.padding(14.dp)) }
                }
            }

            // How to
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "Before you uninstall or change phone",
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            "1. Tap Quick backup (or Choose where to save).\n" +
                                "2. Copy the JSON file somewhere safe, for example Google Drive or your computer.\n" +
                                "3. Uninstall and reinstall Nidhaan.\n" +
                                "4. On the first screen tap \"Restore from backup file\" and pick the JSON file.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
            item { Spacer(Modifier.height(8.dp)) }
        }
    }

    pendingRestore?.let { uri ->
        ConfirmDialog(
            title = "Replace all data?",
            text = "Everything currently in Nidhaan will be replaced by the content of the backup file.",
            confirmLabel = "Restore",
            onConfirm = {
                pendingRestore = null
                busy = true
                scope.launch {
                    val result = BackupManager.restoreFromUri(ctx, uri)
                    result.onSuccess { report(true, "Restored ${it.describe()}.") }
                    result.onFailure { report(false, "Restore failed, nothing was changed: ${it.message ?: "invalid file"}") }
                    busy = false
                }
            },
            onDismiss = { pendingRestore = null }
        )
    }
}

// ---------------------------------------------------------------- permissions

@Composable
private fun PermissionRow(icon: ImageVector, title: String, description: String, status: String, ok: Boolean?) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    status,
                    style = MaterialTheme.typography.labelMedium,
                    color = when (ok) {
                        true -> MaterialTheme.colorScheme.primary
                        false -> MaterialTheme.colorScheme.secondary
                        null -> MaterialTheme.colorScheme.outline
                    }
                )
            }
        }
    }
}

@Composable
fun PermissionsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var tick by remember { mutableIntStateOf(0) }

    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) tick++
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }

    val camera = remember(tick) { isGranted(ctx, Manifest.permission.CAMERA) }
    val location = remember(tick) { hasLocationPermission(ctx) }

    val camLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { tick++ }
    val locLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { tick++ }

    Scaffold(topBar = { AppTopBar("Permissions", onBack) }) { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Nidhaan only asks for the permissions it needs to help you organise your things.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            PermissionRow(
                Icons.Filled.CameraAlt, "Camera", "Capture photos of items and places",
                if (camera) "Allowed" else "Not allowed", camera
            )
            if (!camera) {
                Button(onClick = { camLauncher.launch(Manifest.permission.CAMERA) }) { Text("Allow camera") }
            }
            PermissionRow(
                Icons.Filled.LocationOn, "Location", "Optional: tag where a place is",
                if (location) "Allowed" else "Not allowed (optional)", location
            )
            if (!location) {
                Button(onClick = {
                    locLauncher.launch(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                    )
                }) { Text("Allow location") }
            }
            PermissionRow(
                Icons.Filled.Image, "Gallery / storage", "Not needed. Photos stay inside the app",
                "Not required", null
            )
            PermissionRow(
                Icons.Filled.Lock, "Internet", "Not used. Nidhaan works fully offline",
                "Not required", null
            )
            OutlinedButton(
                onClick = {
                    ctx.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", ctx.packageName, null)
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Open Android settings") }
        }
    }
}
