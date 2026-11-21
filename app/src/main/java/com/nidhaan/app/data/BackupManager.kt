package com.nidhaan.app.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import androidx.annotation.RequiresApi
import androidx.room.withTransaction
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import com.nidhaan.app.Graph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStream
import java.io.InputStreamReader
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BackupCounts(val places: Int, val containers: Int, val items: Int, val photos: Int) {
    fun describe(): String =
        "$places location(s), $containers container(s), $items item(s), $photos photo(s)"
}

/**
 * One JSON file holds everything: locations, containers, items and all photos (Base64 JPEG).
 * It is streamed in both directions so a large photo collection does not exhaust memory.
 */
object BackupManager {
    private const val APP_ID = "Nidhaan"
    private const val FORMAT = 1L

    fun defaultFileName(): String =
        "nidhaan_backup_" + SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date()) + ".json"

    // ------------------------------------------------------------------ export

    suspend fun export(out: OutputStream): BackupCounts = withContext(Dispatchers.IO) {
        val dao = Graph.db.dao()
        val places = dao.allPlaces()
        val containers = dao.allContainers()
        val items = dao.allItems()
        val photosByItem = dao.allItemPhotos().groupBy { it.itemId }
        var photoCount = 0

        val w = JsonWriter(BufferedWriter(OutputStreamWriter(out, Charsets.UTF_8), 1 shl 16))
        w.beginObject()
        w.name("app").value(APP_ID)
        w.name("format").value(FORMAT)
        w.name("exportedAt").value(System.currentTimeMillis())

        w.name("places").beginArray()
        for (p in places) {
            w.beginObject()
            w.name("id").value(p.id)
            w.name("name").value(p.name)
            w.name("area").value(p.area)
            w.name("notes").value(p.notes)
            w.name("lat")
            val lat = p.lat
            if (lat != null) w.value(lat) else w.nullValue()
            w.name("lon")
            val lon = p.lon
            if (lon != null) w.value(lon) else w.nullValue()
            w.name("createdAt").value(p.createdAt)
            w.name("photo")
            if (writePhoto(w, p.photo)) photoCount++
            w.endObject()
        }
        w.endArray()

        w.name("containers").beginArray()
        for (c in containers) {
            w.beginObject()
            w.name("id").value(c.id)
            w.name("placeId").value(c.placeId)
            w.name("name").value(c.name)
            w.name("type").value(c.type)
            w.name("notes").value(c.notes)
            w.name("createdAt").value(c.createdAt)
            w.name("photo")
            if (writePhoto(w, c.photo)) photoCount++
            w.endObject()
        }
        w.endArray()

        w.name("items").beginArray()
        for (i in items) {
            w.beginObject()
            w.name("id").value(i.id)
            w.name("containerId").value(i.containerId)
            w.name("name").value(i.name)
            w.name("isGroup").value(i.isGroup)
            w.name("qty").value(i.qty.toLong())
            w.name("notes").value(i.notes)
            w.name("createdAt").value(i.createdAt)
            w.name("photos").beginArray()
            for (ph in photosByItem[i.id].orEmpty()) {
                if (writePhoto(w, ph.fileName)) photoCount++
            }
            w.endArray()
            w.endObject()
        }
        w.endArray()

        w.endObject()
        w.flush()
        w.close()
        BackupCounts(places.size, containers.size, items.size, photoCount)
    }

    /** Writes the Base64 string for the photo, or JSON null if the file is missing. Returns true if written. */
    private fun writePhoto(w: JsonWriter, fileName: String?): Boolean {
        val f = fileName?.let { ImageStore.file(it) }
        return if (f != null && f.exists()) {
            w.value(Base64.encodeToString(f.readBytes(), Base64.NO_WRAP))
            true
        } else {
            w.nullValue()
            false
        }
    }

    suspend fun exportToUri(ctx: Context, uri: Uri): BackupCounts {
        val counts = ctx.contentResolver.openOutputStream(uri, "wt")?.use { export(it) }
            ?: error("Cannot write to the chosen location")
        Graph.prefs.markBackup(uri.lastPathSegment ?: "backup")
        return counts
    }

    /** One tap backup to Downloads/Nidhaan (Android 10+, no permission needed). */
    @RequiresApi(Build.VERSION_CODES.Q)
    suspend fun quickBackupToDownloads(ctx: Context): Pair<String, BackupCounts> =
        withContext(Dispatchers.IO) {
            val name = defaultFileName()
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, "application/json")
                put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Nidhaan")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val resolver = ctx.contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("Could not create the backup file in Downloads")
            try {
                val counts = resolver.openOutputStream(uri)?.use { export(it) }
                    ?: error("Could not open the backup file")
                val done = ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }
                resolver.update(uri, done, null, null)
                Graph.prefs.markBackup(name)
                name to counts
            } catch (e: Exception) {
                resolver.delete(uri, null, null)
                throw e
            }
        }

    // ----------------------------------------------------------------- restore

    suspend fun restoreFromUri(ctx: Context, uri: Uri): Result<BackupCounts> = runCatching {
        ctx.contentResolver.openInputStream(uri)?.use { restore(it) }
            ?: error("Cannot open the chosen file")
    }

    /** Replaces everything in the app with the content of the backup. All or nothing (single transaction). */
    suspend fun restore(input: InputStream): BackupCounts = withContext(Dispatchers.IO) {
        val db = Graph.db
        val dao = db.dao()
        var places = 0
        var containers = 0
        var items = 0
        var photos = 0
        val r = JsonReader(BufferedReader(InputStreamReader(input, Charsets.UTF_8), 1 shl 16))
        try {
            db.withTransaction {
                dao.clearAll()
                var sawHeader = false
                r.beginObject()
                while (r.hasNext()) {
                    when (r.nextName()) {
                        "app" -> {
                            if (r.nextString() != APP_ID) throw IllegalArgumentException("This is not a Nidhaan backup")
                            sawHeader = true
                        }
                        "places" -> {
                            r.beginArray()
                            while (r.hasNext()) {
                                if (readPlace(r, dao)) photos++
                                places++
                            }
                            r.endArray()
                        }
                        "containers" -> {
                            r.beginArray()
                            while (r.hasNext()) {
                                if (readContainer(r, dao)) photos++
                                containers++
                            }
                            r.endArray()
                        }
                        "items" -> {
                            r.beginArray()
                            while (r.hasNext()) {
                                photos += readItem(r, dao)
                                items++
                            }
                            r.endArray()
                        }
                        else -> r.skipValue()
                    }
                }
                r.endObject()
                if (!sawHeader) throw IllegalArgumentException("This is not a Nidhaan backup")
            }
        } finally {
            try { r.close() } catch (_: Exception) { }
            ImageStore.cleanOrphans()
        }
        BackupCounts(places, containers, items, photos)
    }

    private fun nextStringOrNull(r: JsonReader): String? =
        if (r.peek() == JsonToken.NULL) { r.nextNull(); null } else r.nextString()

    private fun nextDoubleOrNull(r: JsonReader): Double? =
        if (r.peek() == JsonToken.NULL) { r.nextNull(); null } else r.nextDouble()

    /** Decodes a Base64 photo into the private image folder; returns the new file name or null. */
    private fun readPhoto(r: JsonReader): String? {
        val b64 = nextStringOrNull(r) ?: return null
        if (b64.isEmpty()) return null
        return ImageStore.saveBytes(Base64.decode(b64, Base64.DEFAULT))
    }

    private suspend fun readPlace(r: JsonReader, dao: AppDao): Boolean {
        var id = 0L; var name = ""; var area = ""; var notes = ""
        var lat: Double? = null; var lon: Double? = null
        var created = System.currentTimeMillis(); var photo: String? = null
        r.beginObject()
        while (r.hasNext()) {
            when (r.nextName()) {
                "id" -> id = r.nextLong()
                "name" -> name = r.nextString()
                "area" -> area = r.nextString()
                "notes" -> notes = r.nextString()
                "lat" -> lat = nextDoubleOrNull(r)
                "lon" -> lon = nextDoubleOrNull(r)
                "createdAt" -> created = r.nextLong()
                "photo" -> photo = readPhoto(r)
                else -> r.skipValue()
            }
        }
        r.endObject()
        dao.upsertPlace(Place(id, name, area, notes, photo, lat, lon, created))
        return photo != null
    }

    private suspend fun readContainer(r: JsonReader, dao: AppDao): Boolean {
        var id = 0L; var placeId = 0L; var name = ""; var type = "Box"; var notes = ""
        var created = System.currentTimeMillis(); var photo: String? = null
        r.beginObject()
        while (r.hasNext()) {
            when (r.nextName()) {
                "id" -> id = r.nextLong()
                "placeId" -> placeId = r.nextLong()
                "name" -> name = r.nextString()
                "type" -> type = r.nextString()
                "notes" -> notes = r.nextString()
                "createdAt" -> created = r.nextLong()
                "photo" -> photo = readPhoto(r)
                else -> r.skipValue()
            }
        }
        r.endObject()
        dao.upsertContainer(Container(id, placeId, name, type, notes, photo, created))
        return photo != null
    }

    /** Returns the number of photos restored for the item. */
    private suspend fun readItem(r: JsonReader, dao: AppDao): Int {
        var id = 0L; var containerId = 0L; var name = ""; var isGroup = false
        var qty = 1; var notes = ""; var created = System.currentTimeMillis()
        val photoNames = mutableListOf<String>()
        r.beginObject()
        while (r.hasNext()) {
            when (r.nextName()) {
                "id" -> id = r.nextLong()
                "containerId" -> containerId = r.nextLong()
                "name" -> name = r.nextString()
                "isGroup" -> isGroup = r.nextBoolean()
                "qty" -> qty = r.nextInt()
                "notes" -> notes = r.nextString()
                "createdAt" -> created = r.nextLong()
                "photos" -> {
                    if (r.peek() == JsonToken.NULL) {
                        r.nextNull()
                    } else {
                        r.beginArray()
                        while (r.hasNext()) readPhoto(r)?.let { photoNames.add(it) }
                        r.endArray()
                    }
                }
                else -> r.skipValue()
            }
        }
        r.endObject()
        dao.upsertItem(Item(id, containerId, name, isGroup, qty, notes, created))
        for (f in photoNames) dao.upsertItemPhoto(ItemPhoto(itemId = id, fileName = f))
        return photoNames.size
    }
}
