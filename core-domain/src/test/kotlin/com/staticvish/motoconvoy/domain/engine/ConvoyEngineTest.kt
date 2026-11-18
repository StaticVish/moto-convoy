package com.staticvish.motoconvoy.domain.engine

import com.staticvish.motoconvoy.domain.model.ConvoyRole
import com.staticvish.motoconvoy.domain.telemetry.ConvoySpreadStatus
import com.staticvish.motoconvoy.domain.telemetry.RiderTelemetry
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ConvoyEngineTest {

    private lateinit var engine: ConvoyEngine
    private val baseTime = 1794554000000L

    @BeforeEach
    fun setUp() {
        engine = ConvoyEngine(maxSpreadThresholdMeters = 2000.0, stalenessThresholdMs = 15_000L)
    }

    @Test
    fun `ingesting rider telemetry sorts active convoy from lead to sweep`() {
        val sweep = RiderTelemetry("r5", "Dennis", "Tenere 1200", ConvoyRole.SWEEP, 3.32, 101.76, 80.0, 215.0, baseTime)
        val lead = RiderTelemetry("r1", "Vish", "Tiger 900", ConvoyRole.LEAD, 3.35, 101.78, 86.0, 215.0, baseTime)
        val flank = RiderTelemetry("r2", "Raj", "R1250GS", ConvoyRole.LEFT_FLANK, 3.34, 101.77, 85.0, 215.0, baseTime)

        engine.ingestTelemetry(sweep)
        engine.ingestTelemetry(lead)
        engine.ingestTelemetry(flank)

        val snapshot = engine.getSnapshot(currentTimeMs = baseTime)

        assertThat(snapshot.activeRiders).containsExactly(lead, flank, sweep)
        assertThat(snapshot.leadRider).isEqualTo(lead)
        assertThat(snapshot.sweepRider).isEqualTo(sweep)
    }

    @Test
    fun `evaluates convoy spread and triggers stretched alert when lead and sweep exceed threshold`() {
        // Normal distance (~800m)
        val lead = RiderTelemetry("r1", "Vish", "Tiger 900", ConvoyRole.LEAD, 3.350, 101.780, 85.0, 215.0, baseTime)
        val normalSweep = RiderTelemetry("r5", "Dennis", "Tenere 1200", ConvoyRole.SWEEP, 3.344, 101.776, 82.0, 215.0, baseTime)

        engine.ingestTelemetry(lead)
        engine.ingestTelemetry(normalSweep)

        assertThat(engine.getSnapshot(baseTime).spreadStatus).isEqualTo(ConvoySpreadStatus.NORMAL)

        // Stretched distance (~3.7 km)
        val stretchedSweep = RiderTelemetry("r5", "Dennis", "Tenere 1200", ConvoyRole.SWEEP, 3.320, 101.760, 75.0, 215.0, baseTime)
        engine.ingestTelemetry(stretchedSweep)

        val stretchedSnapshot = engine.getSnapshot(baseTime)
        assertThat(stretchedSnapshot.spreadStatus).isEqualTo(ConvoySpreadStatus.STRETCHED)
        assertThat(stretchedSnapshot.convoySpreadMeters).isGreaterThan(2000.0)
    }

    @Test
    fun `detects emergency SOS flag on any rider in the convoy`() {
        val lead = RiderTelemetry("r1", "Vish", "Tiger 900", ConvoyRole.LEAD, 3.35, 101.78, 85.0, 215.0, baseTime)
        val injuredRider = RiderTelemetry("r4", "Kevin", "Versys 650", ConvoyRole.SLIDER, 3.34, 101.77, 0.0, 215.0, baseTime, isEmergency = true)

        engine.ingestTelemetry(lead)
        engine.ingestTelemetry(injuredRider)

        val snapshot = engine.getSnapshot(baseTime)
        assertThat(snapshot.hasActiveEmergency).isTrue()
        assertThat(snapshot.emergencyRiders).containsExactly(injuredRider)
    }

    @Test
    fun `filters out stale riders who have not emitted telemetry within threshold`() {
        val freshLead = RiderTelemetry("r1", "Vish", "Tiger 900", ConvoyRole.LEAD, 3.35, 101.78, 85.0, 215.0, baseTime)
        val staleFlank = RiderTelemetry("r2", "Raj", "R1250GS", ConvoyRole.LEFT_FLANK, 3.34, 101.77, 85.0, 215.0, baseTime - 20_000L) // 20s ago

        engine.ingestTelemetry(freshLead)
        engine.ingestTelemetry(staleFlank)

        val snapshot = engine.getSnapshot(currentTimeMs = baseTime)
        assertThat(snapshot.activeRiders).containsExactly(freshLead)
        assertThat(snapshot.staleRiders).containsExactly(staleFlank)
    }
}
