@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.nidhaan.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nidhaan.app.Graph
import com.nidhaan.app.data.Container
import com.nidhaan.app.data.ImageStore
import com.nidhaan.app.data.ItemRow
import kotlinx.coroutines.launch

/** The Photo | Item | Qty | Notes table. */
@Composable
fun ItemsTable(rows: List<ItemRow>, onClick: (Long) -> Unit) {
    val head = MaterialTheme.typography.labelLarge
    Column(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Row(
            Modifier.fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Photo", Modifier.width(56.dp), style = head, fontWeight = FontWeight.Bold)
            Text("Item", Modifier.weight(1f), style = head, fontWeight = FontWeight.Bold)
            Text("Qty", Modifier.width(44.dp), style = head, fontWeight = FontWeight.Bold)
            Text("Notes", Modifier.weight(0.8f), style = head, fontWeight = FontWeight.Bold)
        }
        rows.forEach { r ->
            Row(
                Modifier.fillMaxWidth()
                    .clickable { onClick(r.item.id) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PhotoImage(
                    r.thumb,
                    Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
                    Icons.Filled.Image
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(r.item.name, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (r.item.isGroup) {
                        Text("Group", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                    }
                }
                Text(r.item.qty.toString(), Modifier.width(44.dp))
                Text(
                    r.item.notes, Modifier.weight(0.8f),
                    maxLines = 2, overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            HorizontalDivider()
        }
    }
}

@Composable
fun ContainerDetailScreen(
    containerId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onOpenItem: (Long) -> Unit,
    onAddItem: () -> Unit
) {
    val dao = Graph.db.dao()
    val scope = rememberCoroutineScope()
    val containerFlow = remember(containerId) { dao.container(containerId) }
    val crumbFlow = remember(containerId) { dao.containerCrumb(containerId) }
    val itemsFlow = remember(containerId) { dao.itemsIn(containerId) }
    val container by containerFlow.collectAsState(initial = null)
    val crumb by crumbFlow.collectAsState(initial = null)
    val rows by itemsFlow.collectAsState(initial = emptyList())
    var confirmDelete by remember { mutableStateOf(false) }
    val c = container

    Scaffold(
        topBar = {
            AppTopBar(c?.name ?: "", onBack) {
                IconButton(onClick = { if (c != null) onEdit(c.placeId) }) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Delete")
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddItem,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add item") }
            )
        }
    ) { pad ->
        if (c == null) {
            Box(Modifier.padding(pad).fillMaxSize())
        } else {
            LazyColumn(
                modifier = Modifier.padding(pad).fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 4.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                crumb?.let { cr ->
                    item { Breadcrumb(listOf(cr.placeName to null, cr.containerName to null)) }
                }
                item {
                    PhotoImage(
                        c.photo,
                        Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(16.dp)),
                        typeIcon(c.type)
                    )
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(typeIcon(c.type), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(6.dp))
                            Text(c.type, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.width(12.dp))
                            Text("${rows.size} items", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (c.notes.isNotBlank()) Text(c.notes)
                    }
                }
                item {
                    Text("Items", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }
                if (rows.isEmpty()) {
                    item { EmptyState("Nothing in here yet.\nTap \"Add item\" to list what is stored.") }
                } else {
                    item { ItemsTable(rows, onOpenItem) }
                }
            }
        }
    }

    if (confirmDelete && c != null) {
        ConfirmDialog(
            title = "Delete \"${c.name}\"?",
            text = "This also deletes all items and photos inside it. This cannot be undone.",
            confirmLabel = "Delete",
            onConfirm = {
                confirmDelete = false
                scope.launch {
                    dao.deleteContainer(containerId)
                    ImageStore.cleanOrphans()
                    onBack()
                }
            },
            onDismiss = { confirmDelete = false }
        )
    }
}

@Composable
fun ContainerEditScreen(
    placeId: Long,
    containerId: Long,
    onBack: () -> Unit,
    onSaved: (Long, Boolean) -> Unit
) {
    val dao = Graph.db.dao()
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("Box") }
    var custom by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var photo by remember { mutableStateOf<String?>(null) }
    var showCamera by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(containerId) {
        if (containerId != 0L) {
            dao.containerOnce(containerId)?.let {
                name = it.name
                notes = it.notes
                photo = it.photo
                if (it.type in CONTAINER_TYPES) {
                    type = it.type
                } else {
                    type = "Other"
                    custom = it.type
                }
            }
        }
    }

    if (showCamera) {
        CameraCapture(
            onPhoto = { photo = it; showCamera = false },
            onClose = { showCamera = false }
        )
    } else {
        Scaffold(topBar = { AppTopBar(if (containerId == 0L) "Add container" else "Edit container", onBack) }) { pad ->
            Column(
                Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp).imePadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                PhotoField(photo, typeIcon(type), onTakePhoto = { showCamera = true }, onRemove = { photo = null })
                OutlinedTextField(
                    value = name, onValueChange = { name = it }, label = { Text("Name (e.g. Blue Toolbox)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )

                Text("Type", fontWeight = FontWeight.SemiBold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CONTAINER_TYPES.forEach { t ->
                        FilterChip(
                            selected = type == t,
                            onClick = { type = t },
                            label = { Text(t) },
                            leadingIcon = { Icon(typeIcon(t), contentDescription = null, modifier = Modifier.size(18.dp)) }
                        )
                    }
                }
                if (type == "Other") {
                    OutlinedTextField(
                        value = custom, onValueChange = { custom = it },
                        label = { Text("Custom type (e.g. Trunk, Basket)") },
                        singleLine = true, modifier = Modifier.fillMaxWidth()
                    )
                }

                OutlinedTextField(
                    value = notes, onValueChange = { notes = it }, label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                PrimaryButton("Save", enabled = name.isNotBlank() && !saving) {
                    saving = true
                    val finalType = if (type == "Other") custom.trim().ifBlank { "Other" } else type
                    scope.launch {
                        val existing = if (containerId != 0L) dao.containerOnce(containerId) else null
                        val id: Long
                        if (existing == null) {
                            id = dao.upsertContainer(
                                Container(placeId = placeId, name = name.trim(), type = finalType, notes = notes.trim(), photo = photo)
                            )
                        } else {
                            dao.upsertContainer(
                                existing.copy(name = name.trim(), type = finalType, notes = notes.trim(), photo = photo)
                            )
                            id = existing.id
                        }
                        ImageStore.cleanOrphans()
                        saving = false
                        onSaved(id, existing == null)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
