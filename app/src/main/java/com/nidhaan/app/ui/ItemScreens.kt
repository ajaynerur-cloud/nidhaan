@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.nidhaan.app.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nidhaan.app.Graph
import com.nidhaan.app.data.ImageStore
import com.nidhaan.app.data.Item
import com.nidhaan.app.data.ItemPhoto
import kotlinx.coroutines.launch

// ----------------------------------------------------------------- add / edit

@Composable
fun ItemEditScreen(containerId: Long, itemId: Long, onBack: () -> Unit, onDone: () -> Unit) {
    val dao = Graph.db.dao()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val isNew = itemId == 0L

    var name by remember { mutableStateOf("") }
    var isGroup by remember { mutableStateOf(false) }
    var qty by remember { mutableStateOf("1") }
    var notes by remember { mutableStateOf("") }
    var photo by remember { mutableStateOf<String?>(null) }
    var showCamera by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(itemId) {
        if (!isNew) {
            dao.itemOnce(itemId)?.let {
                name = it.name; isGroup = it.isGroup; qty = it.qty.toString(); notes = it.notes
            }
        }
    }

    fun save(another: Boolean) {
        if (name.isBlank() || saving) return
        saving = true
        val q = if (isGroup) (qty.toIntOrNull() ?: 1).coerceAtLeast(1) else 1
        val p = photo
        val n = name.trim()
        val nt = notes.trim()
        val grp = isGroup
        scope.launch {
            if (isNew) {
                val id = dao.upsertItem(Item(containerId = containerId, name = n, isGroup = grp, qty = q, notes = nt))
                if (p != null) dao.upsertItemPhoto(ItemPhoto(itemId = id, fileName = p))
            } else {
                val old = dao.itemOnce(itemId)
                if (old != null) dao.upsertItem(old.copy(name = n, isGroup = grp, qty = q, notes = nt))
            }
            ImageStore.cleanOrphans()
            saving = false
            if (another) {
                name = ""; isGroup = false; qty = "1"; notes = ""; photo = null
                Toast.makeText(ctx, "Saved. Add the next item.", Toast.LENGTH_SHORT).show()
            } else {
                onDone()
            }
        }
    }

    if (showCamera) {
        CameraCapture(
            onPhoto = { photo = it; showCamera = false },
            onClose = { showCamera = false }
        )
    } else {
        Scaffold(topBar = { AppTopBar(if (isNew) "Add item" else "Edit item", onBack) }) { pad ->
            Column(
                Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp).imePadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (isNew) {
                    PhotoField(
                        photo, Icons.Filled.Inventory2,
                        onTakePhoto = { showCamera = true }, onRemove = { photo = null }
                    )
                }
                OutlinedTextField(
                    value = name, onValueChange = { name = it }, label = { Text("Item name") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !isGroup, onClick = { isGroup = false },
                        label = { Text("Single item") }, modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = isGroup, onClick = { isGroup = true },
                        label = { Text("Group of items") }, modifier = Modifier.weight(1f)
                    )
                }
                if (isGroup) {
                    OutlinedTextField(
                        value = qty, onValueChange = { qty = it.filter { ch -> ch.isDigit() }.take(6) },
                        label = { Text("Quantity") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OutlinedTextField(
                    value = notes, onValueChange = { notes = it }, label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                PrimaryButton("Save", enabled = name.isNotBlank() && !saving) { save(false) }
                if (isNew) {
                    OutlinedButton(
                        onClick = { save(true) },
                        enabled = name.isNotBlank() && !saving,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) { Text("Save & add another") }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// --------------------------------------------------------------------- detail

@Composable
fun ItemDetailScreen(
    itemId: Long,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    onOpenPlace: (Long) -> Unit,
    onOpenContainer: (Long) -> Unit
) {
    val dao = Graph.db.dao()
    val scope = rememberCoroutineScope()
    val itemFlow = remember(itemId) { dao.item(itemId) }
    val crumbFlow = remember(itemId) { dao.itemCrumb(itemId) }
    val photosFlow = remember(itemId) { dao.photosOf(itemId) }
    val choicesFlow = remember { dao.containerChoices() }
    val itemData by itemFlow.collectAsState(initial = null)
    val crumb by crumbFlow.collectAsState(initial = null)
    val photos by photosFlow.collectAsState(initial = emptyList())
    val choices by choicesFlow.collectAsState(initial = emptyList())

    var selected by remember { mutableStateOf<String?>(null) }
    var showCamera by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var showMove by remember { mutableStateOf(false) }
    var photoToDelete by remember { mutableStateOf<ItemPhoto?>(null) }

    val it0 = itemData
    val main: String? = selected?.takeIf { s -> photos.any { p -> p.fileName == s } } ?: photos.firstOrNull()?.fileName

    if (showCamera) {
        CameraCapture(
            onPhoto = { name ->
                scope.launch {
                    dao.upsertItemPhoto(ItemPhoto(itemId = itemId, fileName = name))
                    selected = name
                    showCamera = false
                }
            },
            onClose = { showCamera = false }
        )
    } else {
        Scaffold(
            topBar = {
                AppTopBar(it0?.name ?: "", onBack) {
                    IconButton(onClick = { crumb?.let { c -> onEdit(c.containerId) } }) {
                        Icon(Icons.Filled.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete")
                    }
                }
            }
        ) { pad ->
            if (it0 == null) {
                Box(Modifier.padding(pad).fillMaxSize())
            } else {
                Column(
                    Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    PhotoImage(
                        main,
                        Modifier.fillMaxWidth().height(260.dp).clip(RoundedCornerShape(16.dp)),
                        Icons.Filled.Inventory2
                    )

                    // photo strip: + Photo tile, then every photo of this item
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            Column(
                                Modifier.size(72.dp).clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .clickable { showCamera = true },
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Filled.CameraAlt, contentDescription = "Add photo", tint = MaterialTheme.colorScheme.primary)
                                Text("+ Photo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        items(photos, key = { ph -> ph.id }) { ph ->
                            Box(Modifier.size(72.dp)) {
                                PhotoImage(
                                    ph.fileName,
                                    Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp))
                                        .then(
                                            if (ph.fileName == main)
                                                Modifier.border(2.dp, MaterialTheme.colorScheme.secondary, RoundedCornerShape(10.dp))
                                            else Modifier
                                        )
                                        .clickable { selected = ph.fileName },
                                    Icons.Filled.Inventory2
                                )
                                Box(
                                    Modifier.align(Alignment.TopEnd).padding(2.dp).size(22.dp)
                                        .clip(CircleShape).background(Color(0x99000000))
                                        .clickable { photoToDelete = ph },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Filled.Close, contentDescription = "Delete photo", tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }

                    Text(it0.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

                    crumb?.let { c ->
                        Breadcrumb(
                            listOf<Pair<String, (() -> Unit)?>>(
                                c.placeName to { onOpenPlace(c.placeId) },
                                c.containerName to { onOpenContainer(c.containerId) }
                            )
                        )
                    }

                    HorizontalDivider()
                    Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Column {
                            Text("Type", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                            Text(if (it0.isGroup) "Group of items" else "Single item")
                        }
                        Column {
                            Text("Quantity", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                            Text(it0.qty.toString())
                        }
                    }
                    if (it0.notes.isNotBlank()) {
                        Column {
                            Text("Notes", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
                            Text(it0.notes)
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { crumb?.let { c -> onEdit(c.containerId) } },
                            modifier = Modifier.weight(1f)
                        ) { Text("Edit") }
                        OutlinedButton(onClick = { showMove = true }, modifier = Modifier.weight(1f)) { Text("Move") }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    if (confirmDelete && it0 != null) {
        ConfirmDialog(
            title = "Delete \"${it0.name}\"?",
            text = "The item and all its photos will be removed.",
            confirmLabel = "Delete",
            onConfirm = {
                confirmDelete = false
                scope.launch {
                    dao.deleteItem(itemId)
                    ImageStore.cleanOrphans()
                    onBack()
                }
            },
            onDismiss = { confirmDelete = false }
        )
    }

    photoToDelete?.let { ph ->
        ConfirmDialog(
            title = "Delete this photo?",
            text = "The photo will be removed from the app.",
            confirmLabel = "Delete",
            onConfirm = {
                photoToDelete = null
                scope.launch {
                    dao.deleteItemPhoto(ph.id)
                    ImageStore.cleanOrphans()
                }
            },
            onDismiss = { photoToDelete = null }
        )
    }

    if (showMove) {
        AlertDialog(
            onDismissRequest = { showMove = false },
            title = { Text("Move to...") },
            text = {
                LazyColumn(Modifier.height(320.dp)) {
                    items(choices, key = { ch -> ch.containerId }) { ch ->
                        Column(
                            Modifier.fillMaxWidth()
                                .clickable {
                                    showMove = false
                                    scope.launch { dao.moveItem(itemId, ch.containerId) }
                                }
                                .padding(vertical = 10.dp)
                        ) {
                            Text(ch.containerName, fontWeight = FontWeight.SemiBold)
                            Text(ch.placeName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        HorizontalDivider()
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showMove = false }) { Text("Cancel") } }
        )
    }
}
