package com.staticvish.motoconvoy.ui

import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.staticvish.motoconvoy.domain.model.ConvoyRole
import com.staticvish.motoconvoy.domain.telemetry.RiderTelemetry
import com.staticvish.motoconvoy.simulation.ConvoySnapshot
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline

@Composable
fun TacticalOsmMapView(
    snapshot: ConvoySnapshot,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    AndroidView(
        factory = { ctx ->
            MapView(ctx).apply {
                setTileSource(TileSourceFactory.MAPNIK)
                setMultiTouchControls(true)
                isTilesScaledToDpi = true
                zoomController.setVisibility(org.osmdroid.views.CustomZoomButtonsController.Visibility.NEVER)

                // High-contrast dark tactical filter for OLED dark mode
                val darkMatrix = ColorMatrix(
                    floatArrayOf(
                        -0.80f, 0f, 0f, 0f, 240f,
                        0f, -0.80f, 0f, 0f, 240f,
                        0f, 0f, -0.80f, 0f, 240f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                overlayManager.tilesOverlay.setColorFilter(ColorMatrixColorFilter(darkMatrix))

                controller.setZoom(14.5)
                val initialCenter = snapshot.leadRider?.let { GeoPoint(it.latitude, it.longitude) }
                    ?: GeoPoint(3.35, 101.78)
                controller.setCenter(initialCenter)
                mapViewRef = this
            }
        },
        update = { mapView ->
            mapViewRef = mapView
            updateMapOverlays(context, mapView, snapshot)
        },
        modifier = modifier.fillMaxSize()
    )

    DisposableEffect(Unit) {
        onDispose {
            mapViewRef?.onDetach()
        }
    }
}

private fun updateMapOverlays(
    context: Context,
    mapView: MapView,
    snapshot: ConvoySnapshot
) {
    // Retain only tiles overlay
    val overlaysToKeep = mapView.overlays.filter { it === mapView.overlayManager.tilesOverlay }
    mapView.overlays.clear()
    mapView.overlays.addAll(overlaysToKeep)

    if (snapshot.riders.isEmpty()) return

    // 1. Draw Convoy Formation Polyline connecting Lead down to Sweep
    val polyline = Polyline(mapView).apply {
        outlinePaint.color = Color.CYAN
        outlinePaint.strokeWidth = 6f
        outlinePaint.isAntiAlias = true
        val geoPoints = snapshot.riders.map { GeoPoint(it.latitude, it.longitude) }
        setPoints(geoPoints)
    }
    mapView.overlays.add(polyline)

    // 2. Draw Rider Tactical Markers
    snapshot.riders.forEach { rider ->
        val marker = Marker(mapView).apply {
            position = GeoPoint(rider.latitude, rider.longitude)
            title = "${rider.riderName} (${rider.role.title})"
            snippet = "${rider.bikeModel} • ${rider.speedKmh.toInt()} km/h"
            icon = createTacticalMarkerIcon(context, rider)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            rotation = rider.bearingDegrees.toFloat()
        }
        mapView.overlays.add(marker)
    }

    // Follow the Road Lead smoothly
    snapshot.leadRider?.let { lead ->
        mapView.controller.animateTo(GeoPoint(lead.latitude, lead.longitude))
    }

    mapView.invalidate()
}

private fun createTacticalMarkerIcon(context: Context, rider: RiderTelemetry): Drawable {
    val sizePx = 80
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val roleColorInt = when (rider.role) {
        ConvoyRole.LEAD -> Color.rgb(245, 158, 11)        // Gold/Amber
        ConvoyRole.LEFT_FLANK -> Color.rgb(6, 182, 212)    // Cyan
        ConvoyRole.RIGHT_FLANK -> Color.rgb(59, 130, 246)  // Cobalt Blue
        ConvoyRole.SLIDER -> Color.rgb(168, 85, 247)       // Purple
        ConvoyRole.PACK -> Color.rgb(16, 185, 129)         // Emerald
        ConvoyRole.SWEEP -> Color.rgb(239, 68, 68)         // Crimson Red
    }

    val paint = Paint().apply {
        isAntiAlias = true
    }

    val center = sizePx / 2f

    // Emergency beacon pulse halo if SOS
    if (rider.isEmergency) {
        paint.color = Color.argb(120, 239, 68, 68)
        canvas.drawCircle(center, center, center * 0.95f, paint)
    }

    // Outer tactical ring
    paint.color = Color.BLACK
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 6f
    canvas.drawCircle(center, center, center * 0.70f, paint)

    // Filled role circle
    paint.style = Paint.Style.FILL
    paint.color = roleColorInt
    canvas.drawCircle(center, center, center * 0.65f, paint)

    // Heading direction arrow (triangle pointing forward)
    paint.color = Color.WHITE
    val path = Path().apply {
        moveTo(center, center - 18f)
        lineTo(center - 12f, center + 14f)
        lineTo(center, center + 6f)
        lineTo(center + 12f, center + 14f)
        close()
    }
    canvas.drawPath(path, paint)

    return BitmapDrawable(context.resources, bitmap)
}
