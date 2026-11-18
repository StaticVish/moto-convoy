package com.staticvish.motoconvoy.simulation

import com.staticvish.motoconvoy.domain.model.ConvoyRole
import com.staticvish.motoconvoy.domain.telemetry.ConvoySpreadStatus
import com.staticvish.motoconvoy.domain.telemetry.RiderTelemetry
import com.staticvish.motoconvoy.domain.telemetry.SpatialCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.cos
import kotlin.math.sin

data class ConvoySnapshot(
    val riders: List<RiderTelemetry>,
    val leadRider: RiderTelemetry?,
    val sweepRider: RiderTelemetry?,
    val convoySpreadMeters: Double,
    val spreadStatus: ConvoySpreadStatus,
    val isEmergencyActive: Boolean
)

class ConvoySimulator {

    // Base coordinate: Karak Highway near Genting Sempah
    private var baseLat = 3.3516
    private var baseLng = 101.7894
    private var currentHeading = 215.0 // heading towards KL / Ulu Yam twisties
    private var step = 0
    private var isStretchedMode = false
    private var emergencyRiderId: String? = null

    private val _snapshot = MutableStateFlow(createInitialSnapshot())
    val snapshot: StateFlow<ConvoySnapshot> = _snapshot.asStateFlow()

    private fun createInitialSnapshot(): ConvoySnapshot {
        val now = System.currentTimeMillis()
        val spacing = if (isStretchedMode) 0.006 else 0.0018 // approx 200m normal vs 700m stretched per gap

        val riders = listOf(
            RiderTelemetry("r1", "Vish", "Tiger 900 Rally Pro", ConvoyRole.LEAD, baseLat, baseLng, 86.4, currentHeading, now),
            RiderTelemetry("r2", "Raj", "BMW R1250GS", ConvoyRole.LEFT_FLANK, baseLat - spacing * 0.7, baseLng + 0.0004, 85.0, currentHeading, now),
            RiderTelemetry("r3", "Sam", "Honda Africa Twin", ConvoyRole.RIGHT_FLANK, baseLat - spacing * 1.4, baseLng - 0.0004, 84.8, currentHeading, now),
            RiderTelemetry("r4", "Kevin", "Kawasaki Versys 650", ConvoyRole.SLIDER, baseLat - spacing * 2.2, baseLng, 83.2, currentHeading, now),
            RiderTelemetry("r5", "Dennis", "Yamaha Super Tenere", ConvoyRole.SWEEP, baseLat - spacing * 3.2, baseLng, 81.5, currentHeading, now, isEmergency = emergencyRiderId == "r5")
        )

        val lead = riders.firstOrNull { it.role == ConvoyRole.LEAD }
        val sweep = riders.firstOrNull { it.role == ConvoyRole.SWEEP }

        val spreadMeters = if (lead != null && sweep != null) {
            SpatialCalculator.haversineDistanceMeters(lead.latitude, lead.longitude, sweep.latitude, sweep.longitude)
        } else {
            0.0
        }

        val status = SpatialCalculator.evaluateConvoySpread(spreadMeters, thresholdMeters = 2000.0)

        return ConvoySnapshot(
            riders = riders,
            leadRider = lead,
            sweepRider = sweep,
            convoySpreadMeters = spreadMeters,
            spreadStatus = status,
            isEmergencyActive = emergencyRiderId != null
        )
    }

    fun tick() {
        step++
        // Gentle winding curve simulation (sinusoidal twisty road)
        val deltaAngle = sin(step * 0.15) * 8.0
        currentHeading = (currentHeading + deltaAngle + 360.0) % 360.0

        val speedMs = 23.0 // ~83 km/h
        val distanceTraveledMeters = speedMs * 1.5 // 1.5s tick
        val metersPerDegreeLat = 111_000.0
        val metersPerDegreeLng = 111_000.0 * cos(Math.toRadians(baseLat))

        val rad = Math.toRadians(currentHeading)
        baseLat += (cos(rad) * distanceTraveledMeters) / metersPerDegreeLat
        baseLng += (sin(rad) * distanceTraveledMeters) / metersPerDegreeLng

        _snapshot.value = createInitialSnapshot()
    }

    fun toggleStretchedPack() {
        isStretchedMode = !isStretchedMode
        _snapshot.value = createInitialSnapshot()
    }

    fun toggleEmergencySos() {
        emergencyRiderId = if (emergencyRiderId == null) "r5" else null
        _snapshot.value = createInitialSnapshot()
    }
}
