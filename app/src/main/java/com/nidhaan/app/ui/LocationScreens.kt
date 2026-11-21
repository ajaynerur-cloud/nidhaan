@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)

package com.nidhaan.app.ui

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nidhaan.app.Graph
import com.nidhaan.app.data.ContainerRow
import com.nidhaan.app.data.ImageStore
import com.nidhaan.app.data.Place
import com.nidhaan.app.data.PlaceRow
import com.nidhaan.app.data.SearchRow
import kotlinx.coroutines.launch

// ------------------------------------------------------------------ cards

@Composable
fun PlaceCard(row: PlaceRow, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            PhotoImage(
                row.place.photo,
                Modifier.size(88.dp).clip(RoundedCornerShape(14.dp)),
                Icons.Filled.Home
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    row.place.name, fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${row.containerCount} containers · ${row.itemCount} items",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (row.place.area.isNotBlank()) {
                    Text(
                        row.place.area,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
            Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@Composable
fun ContainerCard(row: ContainerRow, onClick: () -> Unit) {
    val c = row.container
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            PhotoImage(c.photo, Modifier.size(88.dp).clip(RoundedCornerShape(14.dp)), typeIcon(c.type))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        typeIcon(c.type), contentDescription = null,
                        modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(c.type, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
                Text(
                    c.name, fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${row.itemCount} items",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@Composable
fun SearchResultCard(r: SearchRow, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            PhotoImage(r.thumb, Modifier.size(60.dp).clip(RoundedCornerShape(12.dp)), Icons.Filled.Inventory2)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    r.itemName, fontWeight = FontWeight.SemiBold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${r.placeName}  ›  ${r.containerName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            if (r.qty > 1) {
                Spacer(Modifier.width(8.dp))
                Text("×${r.qty}", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun BackupBanner(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                "Back up your data",
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Text(
                "Your photos live only inside this app. Save a backup file to your phone so nothing is lost if the app is uninstalled.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

// ----------------------------------------------------------- home / lists

@Composable
fun LocationsScreen(
    onTab: (String) -> Unit,
    onOpenPlace: (Long) -> Unit,
    onAddPlace: () -> Unit,
    onOpenItem: (Long) -> Unit,
    onPermissions: () -> Unit
) {
    val dao = Graph.db.dao()
    val placesFlow = remember { dao.placeRows() }
    val totalFlow = remember { dao.totalItems() }
    val places by placesFlow.collectAsState(initial = emptyList())
    val totalItems by totalFlow.collectAsState(initial = 0)
    var query by remember { mutableStateOf("") }
    val searchFlow = remember(query) { dao.search(query.trim()) }
    val results by searchFlow.collectAsState(initial = emptyList())

    val lastBackup = Graph.prefs.lastBackup
    val thirtyDays = 30L * 24 * 3600 * 1000
    val needsBackup = totalItems > 0 && (lastBackup == 0L || System.currentTimeMillis() - lastBackup > thirtyDays)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Nidhaan", fontWeight = FontWeight.Bold)
                        Text(
                            "Your things, in place",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onPermissions) {
                        Icon(Icons.Filled.Settings, contentDescription = "Permissions")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        bottomBar = { BottomTabs("places", onTab) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddPlace,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add location") }
            )
        }
    ) { pad ->
        LazyColumn(
            modifier = Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { SearchField(query, { query = it }, "Search items, boxes, places") }

            if (query.isBlank()) {
                if (needsBackup) item { BackupBanner { onTab("backup") } }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Locations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("${places.size} locations", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (places.isEmpty()) {
                    item { EmptyState("No locations yet.\nAdd your first place, like Home, Garage or Office.") }
                }
                items(places, key = { it.place.id }) { row ->
                    PlaceCard(row) { onOpenPlace(row.place.id) }
                }
            } else {
                item { Text("${results.size} result(s)", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                if (results.isEmpty()) item { EmptyState("Nothing matches \"${query.trim()}\"") }
                items(results, key = { it.itemId }) { r ->
                    SearchResultCard(r) { onOpenItem(r.itemId) }
                }
            }
        }
    }
}

@Composable
fun AllItemsScreen(onTab: (String) -> Unit, onOpenItem: (Long) -> Unit) {
    val dao = Graph.db.dao()
    var query by remember { mutableStateOf("") }
    val flow = remember(query) { dao.search(query.trim()) }
    val results by flow.collectAsState(initial = emptyList())

    Scaffold(
        topBar = { AppTopBar("All items") },
        bottomBar = { BottomTabs("allitems", onTab) }
    ) { pad ->
        LazyColumn(
            modifier = Modifier.padding(pad).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { SearchField(query, { query = it }, "Search items, boxes, places") }
            item { Text("${results.size} item(s)", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (results.isEmpty()) item { EmptyState("No items yet. Open a container and tap \"Add item\".") }
            items(results, key = { it.itemId }) { r ->
                SearchResultCard(r) { onOpenItem(r.itemId) }
            }
        }
    }
}

// ----------------------------------------------------------- place detail

@Composable
fun PlaceDetailScreen(
    placeId: Long,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onOpenContainer: (Long) -> Unit,
    onAddContainer: () -> Unit
) {
    val dao = Graph.db.dao()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val placeFlow = remember(placeId) { dao.place(placeId) }
    val containersFlow = remember(placeId) { dao.containerRows(placeId) }
    val place by placeFlow.collectAsState(initial = null)
    val containers by containersFlow.collectAsState(initial = emptyList())
    var confirmDelete by remember { mutableStateOf(false) }
    val p = place

    Scaffold(
        topBar = {
            AppTopBar(p?.name ?: "", onBack) {
                IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Edit") }
                IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddContainer,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add container") }
            )
        }
    ) { pad ->
        if (p == null) {
            Box(Modifier.padding(pad).fillMaxSize())
        } else {
            LazyColumn(
                modifier = Modifier.padding(pad).fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (p.photo != null) {
                    item {
                        PhotoImage(
                            p.photo,
                            Modifier.fillMaxWidth().height(200.dp).clip(RoundedCornerShape(16.dp)),
                            Icons.Filled.Home
                        )
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (p.area.isNotBlank()) {
                            Text(p.area, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (p.notes.isNotBlank()) Text(p.notes)
                        val lat = p.lat
                        val lon = p.lon
                        if (lat != null && lon != null) {
                            AssistChip(
                                onClick = { openInMaps(ctx, lat, lon, p.name) },
                                label = { Text("Open in Maps") },
                                leadingIcon = { Icon(Icons.Filled.OpenInNew, contentDescription = null) }
                            )
                        }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Containers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("${containers.size} containers", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (containers.isEmpty()) {
                    item { EmptyState("No containers yet.\nAdd a box, rack, wardrobe, drawer...") }
                }
                items(containers, key = { it.container.id }) { row ->
                    ContainerCard(row) { onOpenContainer(row.container.id) }
                }
            }
        }
    }

    if (confirmDelete && p != null) {
        ConfirmDialog(
            title = "Delete \"${p.name}\"?",
            text = "This also deletes all its containers, items and photos. This cannot be undone.",
            confirmLabel = "Delete",
            onConfirm = {
                confirmDelete = false
                scope.launch {
                    dao.deletePlace(placeId)
                    ImageStore.cleanOrphans()
                    onBack()
                }
            },
            onDismiss = { confirmDelete = false }
        )
    }
}

// ------------------------------------------------------------- place edit

@Composable
fun PlaceEditScreen(placeId: Long, onBack: () -> Unit, onSaved: (Long, Boolean) -> Unit) {
    val dao = Graph.db.dao()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var photo by remember { mutableStateOf<String?>(null) }
    var lat by remember { mutableStateOf<Double?>(null) }
    var lon by remember { mutableStateOf<Double?>(null) }
    var showCamera by remember { mutableStateOf(false) }
    var locating by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(placeId) {
        if (placeId != 0L) {
            dao.placeOnce(placeId)?.let {
                name = it.name; area = it.area; notes = it.notes
                photo = it.photo; lat = it.lat; lon = it.lon
            }
        }
    }

    val doFetch: () -> Unit = {
        locating = true
        fetchLocation(ctx) { loc ->
            locating = false
            if (loc != null) {
                lat = loc.latitude
                lon = loc.longitude
            } else {
                Toast.makeText(ctx, "Could not get location. Turn on location and try again.", Toast.LENGTH_LONG).show()
            }
        }
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) doFetch()
        else Toast.makeText(ctx, "Location permission was not granted", Toast.LENGTH_SHORT).show()
    }

    if (showCamera) {
        CameraCapture(
            onPhoto = { photo = it; showCamera = false },
            onClose = { showCamera = false }
        )
    } else {
        Scaffold(topBar = { AppTopBar(if (placeId == 0L) "Add location" else "Edit location", onBack) }) { pad ->
            Column(
                Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp).imePadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                PhotoField(photo, Icons.Filled.Home, onTakePhoto = { showCamera = true }, onRemove = { photo = null })
                OutlinedTextField(
                    value = name, onValueChange = { name = it }, label = { Text("Location name") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = area, onValueChange = { area = it }, label = { Text("Area (optional)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes, onValueChange = { notes = it }, label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("GPS tag (optional)", fontWeight = FontWeight.SemiBold)
                    val la = lat
                    val lo = lon
                    if (la != null && lo != null) {
                        Text(
                            "%.5f, %.5f".format(la, lo),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            enabled = !locating,
                            onClick = {
                                if (hasLocationPermission(ctx)) doFetch()
                                else permLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        ) {
                            Icon(Icons.Filled.MyLocation, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (locating) "Locating..." else "Tag current location")
                        }
                        if (la != null) {
                            TextButton(onClick = { lat = null; lon = null }) { Text("Clear") }
                        }
                    }
                }

                PrimaryButton("Save", enabled = name.isNotBlank() && !saving) {
                    saving = true
                    scope.launch {
                        val existing = if (placeId != 0L) dao.placeOnce(placeId) else null
                        val id: Long
                        if (existing == null) {
                            id = dao.upsertPlace(
                                Place(name = name.trim(), area = area.trim(), notes = notes.trim(), photo = photo, lat = lat, lon = lon)
                            )
                        } else {
                            dao.upsertPlace(
                                existing.copy(name = name.trim(), area = area.trim(), notes = notes.trim(), photo = photo, lat = lat, lon = lon)
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
