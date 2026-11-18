package com.staticvish.motoconvoy.domain.telemetry

import kotlin.math.*

enum class ConvoySpreadStatus {
    NORMAL,
    STRETCHED
}

object SpatialCalculator {
    private const val EARTH_RADIUS_METERS = 6371000.0

    /**
     * Calculates great-circle distance between two geographic coordinates using the Haversine formula.
     */
    fun haversineDistanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val originLatRad = Math.toRadians(lat1)
        val targetLatRad = Math.toRadians(lat2)

        val a = sin(dLat / 2.0).pow(2.0) +
                sin(dLon / 2.0).pow(2.0) * cos(originLatRad) * cos(targetLatRad)
        val c = 2.0 * atan2(sqrt(a), sqrt(1.0 - a))

        return EARTH_RADIUS_METERS * c
    }

    /**
     * Calculates initial compass bearing from (lat1, lon1) to (lat2, lon2) in degrees [0, 360).
     */
    fun calculateBearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val phi1 = Math.toRadians(lat1)
        val phi2 = Math.toRadians(lat2)
        val deltaLambda = Math.toRadians(lon2 - lon1)

        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)

        val theta = atan2(y, x)
        val degrees = Math.toDegrees(theta)
        return (degrees + 360.0) % 360.0
    }

    /**
     * Formats relative distance delta for HUD rendering (e.g. "+450 m", "+1.9 km", "-800 m").
     */
    fun formatDistanceDelta(meters: Double): String {
        val sign = if (meters >= 0) "+" else "-"
        val absMeters = abs(meters)
        return if (absMeters < 1000.0) {
            "$sign${absMeters.roundToInt()} m"
        } else {
            val km = absMeters / 1000.0
            String.format("%.1f km", km).let { "$sign$it" }
        }
    }

    /**
     * Determines if telemetry is stale based on a threshold (default 15 seconds).
     */
    fun isStale(packetTimestampMs: Long, currentTimestampMs: Long, thresholdMs: Long = 15_000L): Boolean {
        return (currentTimestampMs - packetTimestampMs) > thresholdMs
    }

    /**
     * Evaluates whether the convoy gap between Lead and Sweep exceeds safety limits.
     */
    fun evaluateConvoySpread(leadToSweepDistanceMeters: Double, thresholdMeters: Double = 2500.0): ConvoySpreadStatus {
        return if (leadToSweepDistanceMeters > thresholdMeters) {
            ConvoySpreadStatus.STRETCHED
        } else {
            ConvoySpreadStatus.NORMAL
        }
    }
}
