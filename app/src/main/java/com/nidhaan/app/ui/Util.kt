package com.nidhaan.app.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun isGranted(ctx: Context, permission: String): Boolean =
    ContextCompat.checkSelfPermission(ctx, permission) == PackageManager.PERMISSION_GRANTED

fun hasLocationPermission(ctx: Context): Boolean =
    isGranted(ctx, Manifest.permission.ACCESS_FINE_LOCATION) ||
        isGranted(ctx, Manifest.permission.ACCESS_COARSE_LOCATION)

/** One-shot location fix. Uses GPS if fine location is allowed, otherwise network location. */
@SuppressLint("MissingPermission")
fun fetchLocation(ctx: Context, onResult: (Location?) -> Unit) {
    try {
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val fine = isGranted(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
        val provider = when {
            fine && lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> null
        }
        if (provider == null) {
            onResult(null)
            return
        }
        LocationManagerCompat.getCurrentLocation(
            lm,
            provider,
            CancellationSignal(),
            ContextCompat.getMainExecutor(ctx)
        ) { loc: Location? -> onResult(loc) }
    } catch (e: SecurityException) {
        onResult(null)
    }
}

fun openInMaps(ctx: Context, lat: Double, lon: Double, label: String) {
    try {
        val uri = Uri.parse("geo:$lat,$lon?q=$lat,$lon(${Uri.encode(label)})")
        ctx.startActivity(Intent(Intent.ACTION_VIEW, uri))
    } catch (e: Exception) {
        Toast.makeText(ctx, "No maps app found", Toast.LENGTH_SHORT).show()
    }
}

fun formatTime(ts: Long): String =
    SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()).format(Date(ts))
