package com.staticvish.motoconvoy.domain.engine

import com.staticvish.motoconvoy.domain.model.ConvoyRole
import com.staticvish.motoconvoy.domain.telemetry.ConvoySpreadStatus
import com.staticvish.motoconvoy.domain.telemetry.RiderTelemetry
import com.staticvish.motoconvoy.domain.telemetry.SpatialCalculator
import java.util.concurrent.ConcurrentHashMap

data class ConvoyStateSnapshot(
    val activeRiders: List<RiderTelemetry>,
    val staleRiders: List<RiderTelemetry>,
    val leadRider: RiderTelemetry?,
    val sweepRider: RiderTelemetry?,
    val convoySpreadMeters: Double,
    val spreadStatus: ConvoySpreadStatus,
    val hasActiveEmergency: Boolean,
    val emergencyRiders: List<RiderTelemetry>
)

class ConvoyEngine(
    private val maxSpreadThresholdMeters: Double = 2000.0,
    private val stalenessThresholdMs: Long = 15_000L
) {
    private val telemetryStore = ConcurrentHashMap<String, RiderTelemetry>()

    fun ingestTelemetry(telemetry: RiderTelemetry) {
        telemetryStore[telemetry.riderId] = telemetry
    }

    fun removeRider(riderId: String) {
        telemetryStore.remove(riderId)
    }

    fun clear() {
        telemetryStore.clear()
    }

    fun getSnapshot(currentTimeMs: Long = System.currentTimeMillis()): ConvoyStateSnapshot {
        val allRiders = telemetryStore.values.toList()

        val active = mutableListOf<RiderTelemetry>()
        val stale = mutableListOf<RiderTelemetry>()
        val emergencies = mutableListOf<RiderTelemetry>()

        for (rider in allRiders) {
            if (SpatialCalculator.isStale(rider.timestampMs, currentTimeMs, stalenessThresholdMs)) {
                stale.add(rider)
            } else {
                active.add(rider)
            }

            if (rider.isEmergency) {
                emergencies.add(rider)
            }
        }

        // Sort active riders by tactical convoy priority: LEAD -> LEFT_FLANK -> RIGHT_FLANK -> SLIDER -> PACK -> SWEEP
        val sortedActive = active.sortedBy { it.role.priority }

        val lead = sortedActive.firstOrNull { it.role == ConvoyRole.LEAD }
        val sweep = sortedActive.lastOrNull { it.role == ConvoyRole.SWEEP }

        val spreadMeters = if (lead != null && sweep != null) {
            SpatialCalculator.haversineDistanceMeters(
                lead.latitude, lead.longitude,
                sweep.latitude, sweep.longitude
            )
        } else {
            0.0
        }

        val spreadStatus = SpatialCalculator.evaluateConvoySpread(spreadMeters, maxSpreadThresholdMeters)

        return ConvoyStateSnapshot(
            activeRiders = sortedActive,
            staleRiders = stale.sortedBy { it.role.priority },
            leadRider = lead,
            sweepRider = sweep,
            convoySpreadMeters = spreadMeters,
            spreadStatus = spreadStatus,
            hasActiveEmergency = emergencies.isNotEmpty(),
            emergencyRiders = emergencies
        )
    }
}
