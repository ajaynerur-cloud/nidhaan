package com.nidhaan.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import com.nidhaan.app.Graph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("nidhaan_prefs", Context.MODE_PRIVATE)

    var onboarded: Boolean
        get() = sp.getBoolean("onboarded", false)
        set(v) = sp.edit().putBoolean("onboarded", v).apply()

    val lastBackup: Long get() = sp.getLong("last_backup", 0L)
    val lastBackupName: String get() = sp.getString("last_backup_name", "") ?: ""

    fun markBackup(name: String) {
        sp.edit().putLong("last_backup", System.currentTimeMillis())
            .putString("last_backup_name", name).apply()
    }
}

/**
 * All photos live in <app files dir>/images. That folder is private to Nidhaan:
 * photos never appear in the phone gallery and other apps cannot read them.
 */
object ImageStore {
    private const val MAX_SIDE = 1024
    private const val JPEG_QUALITY = 80

    fun dir(): File = File(Graph.app.filesDir, "images").also { it.mkdirs() }

    fun file(name: String): File = File(dir(), name)

    /** Takes a raw camera JPEG, fixes rotation, shrinks it, saves it privately and returns the new file name. */
    fun importFromFile(src: File): String {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(src.path, bounds)
        var sample = 1
        while (bounds.outWidth / sample > MAX_SIDE * 2 || bounds.outHeight / sample > MAX_SIDE * 2) sample *= 2

        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        var bmp = BitmapFactory.decodeFile(src.path, opts) ?: error("Could not read the photo")

        val rotation = when (
            ExifInterface(src.path).getAttributeInt(
                ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
            )
        ) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        val m = Matrix()
        if (rotation != 0f) m.postRotate(rotation)
        val scale = MAX_SIDE.toFloat() / max(bmp.width, bmp.height)
        if (scale < 1f) m.postScale(scale, scale)
        if (!m.isIdentity) bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)

        val name = UUID.randomUUID().toString() + ".jpg"
        FileOutputStream(file(name)).use { bmp.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, it) }
        return name
    }

    /** Writes already-compressed JPEG bytes (used by restore) and returns the new file name. */
    fun saveBytes(bytes: ByteArray): String {
        val name = UUID.randomUUID().toString() + ".jpg"
        file(name).writeBytes(bytes)
        return name
    }

    /** Deletes image files that no database row refers to any more. */
    suspend fun cleanOrphans() = withContext(Dispatchers.IO) {
        val used = Graph.db.dao().allPhotoNames().toSet()
        dir().listFiles()?.forEach { if (it.name !in used) it.delete() }
    }
}
