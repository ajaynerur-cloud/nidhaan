@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.nidhaan.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.nidhaan.app.data.ImageStore

val CONTAINER_TYPES = listOf("Box", "Rack", "Wardrobe", "Drawer", "Shelf", "Cabinet", "Other")

fun typeIcon(type: String): ImageVector = when (type) {
    "Box" -> Icons.Filled.Inventory2
    "Rack" -> Icons.Filled.Dns
    "Wardrobe" -> Icons.Filled.Checkroom
    "Drawer" -> Icons.Filled.Archive
    "Shelf" -> Icons.Filled.ViewDay
    "Cabinet" -> Icons.Filled.Kitchen
    else -> Icons.Filled.Category
}

@Composable
fun AppTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    TopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        },
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
    )
}

@Composable
fun BottomTabs(current: String, onSelect: (String) -> Unit) {
    NavigationBar {
        NavigationBarItem(
            selected = current == "places",
            onClick = { onSelect("places") },
            icon = { Icon(Icons.Filled.Home, contentDescription = null) },
            label = { Text("Locations") }
        )
        NavigationBarItem(
            selected = current == "allitems",
            onClick = { onSelect("allitems") },
            icon = { Icon(Icons.Filled.FormatListBulleted, contentDescription = null) },
            label = { Text("Items") }
        )
        NavigationBarItem(
            selected = current == "backup",
            onClick = { onSelect("backup") },
            icon = { Icon(Icons.Filled.Backup, contentDescription = null) },
            label = { Text("Backup") }
        )
    }
}

@Composable
fun PhotoImage(
    fileName: String?,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Filled.Image,
    contentScale: ContentScale = ContentScale.Crop
) {
    if (fileName != null) {
        val f = remember(fileName) { ImageStore.file(fileName) }
        AsyncImage(model = f, contentDescription = null, modifier = modifier, contentScale = contentScale)
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Photo preview with Take / Retake / Remove buttons, used in the edit forms. */
@Composable
fun PhotoField(
    fileName: String?,
    icon: ImageVector,
    onTakePhoto: () -> Unit,
    onRemove: () -> Unit,
    height: Dp = 180.dp
) {
    Column {
        PhotoImage(
            fileName,
            Modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(16.dp)),
            icon
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            FilledTonalButton(onClick = onTakePhoto, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.CameraAlt, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (fileName == null) "Take photo" else "Retake")
            }
            if (fileName != null) {
                OutlinedButton(onClick = onRemove) { Text("Remove") }
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.Lock, contentDescription = null,
                modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.outline
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "Saved inside Nidhaan only, not in your gallery",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
fun SearchField(value: String, onChange: (String) -> Unit, placeholder: String) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = RoundedCornerShape(28.dp),
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = { onChange("") }) {
                    Icon(Icons.Filled.Clear, contentDescription = "Clear")
                }
            }
        }
    )
}

@Composable
fun Breadcrumb(parts: List<Pair<String, (() -> Unit)?>>) {
    FlowRow(verticalArrangement = Arrangement.Center) {
        parts.forEachIndexed { index, part ->
            val label = part.first
            val click = part.second
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = if (click != null) Modifier.clickable { click() } else Modifier
            )
            if (index < parts.lastIndex) {
                Text("  ›  ", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun EmptyState(text: String) {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(
            text,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun PrimaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(14.dp)
    ) { Text(text) }
}
