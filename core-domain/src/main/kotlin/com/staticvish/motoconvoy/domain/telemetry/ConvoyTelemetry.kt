package com.staticvish.motoconvoy.domain.telemetry

import com.staticvish.motoconvoy.domain.model.ConvoyRole

data class RiderTelemetry(
    val riderId: String,
    val riderName: String,
    val bikeModel: String,
    val role: ConvoyRole,
    val latitude: Double,
    val longitude: Double,
    val speedKmh: Double,
    val bearingDegrees: Double,
    val timestampMs: Long,
    val isEmergency: Boolean = false
) {
    fun isStale(nowMs: Long = System.currentTimeMillis()): Boolean {
        return SpatialCalculator.isStale(timestampMs, nowMs)
    }
}
