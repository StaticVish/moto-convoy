package com.staticvish.motoconvoy.domain.telemetry

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test

class SpatialCalculatorTest {

    // Test coordinates: Karak Highway to Genting Sempah
    private val gentingSempahLat = 3.3516
    private val gentingSempahLng = 101.7894

    private val karakRestStopLat = 3.3280
    private val karakRestStopLng = 101.7650

    @Test
    fun `haversine distance calculates accurate geographic displacement in meters`() {
        val distanceMeters = SpatialCalculator.haversineDistanceMeters(
            lat1 = gentingSempahLat,
            lon1 = gentingSempahLng,
            lat2 = karakRestStopLat,
            lon2 = karakRestStopLng
        )

        // Exact distance is 3771.3 meters
        assertThat(distanceMeters).isCloseTo(3771.3, within(10.0))
    }

    @Test
    fun `bearing calculates directional angle in degrees 0 to 359`() {
        val bearing = SpatialCalculator.calculateBearing(
            lat1 = gentingSempahLat,
            lon1 = gentingSempahLng,
            lat2 = karakRestStopLat,
            lon2 = karakRestStopLng
        )

        // Genting Sempah to Karak Rest Stop heading is South-West (225.9 degrees)
        assertThat(bearing).isCloseTo(225.9, within(2.0))
    }

    @Test
    fun `formatDistanceDelta formats sub-kilometer in meters and long distance in kilometers`() {
        assertThat(SpatialCalculator.formatDistanceDelta(450.0)).isEqualTo("+450 m")
        assertThat(SpatialCalculator.formatDistanceDelta(1850.0)).isEqualTo("+1.9 km")
        assertThat(SpatialCalculator.formatDistanceDelta(-800.0)).isEqualTo("-800 m")
        assertThat(SpatialCalculator.formatDistanceDelta(-2400.0)).isEqualTo("-2.4 km")
    }

    @Test
    fun `isStale detects dropped riders after 15 seconds threshold`() {
        val now = 1794554020000L
        val recentPacketTime = now - 5000L   // 5s ago -> fresh
        val stalePacketTime = now - 18000L   // 18s ago -> stale

        assertThat(SpatialCalculator.isStale(recentPacketTime, now)).isFalse()
        assertThat(SpatialCalculator.isStale(stalePacketTime, now)).isTrue()
    }

    @Test
    fun `convoy spread detector evaluates safe vs stretched formation gaps`() {
        // Safe gap of 1.2 km
        val safeStatus = SpatialCalculator.evaluateConvoySpread(
            leadToSweepDistanceMeters = 1200.0,
            thresholdMeters = 2500.0
        )
        assertThat(safeStatus).isEqualTo(ConvoySpreadStatus.NORMAL)

        // Stretched gap of 3.1 km
        val stretchedStatus = SpatialCalculator.evaluateConvoySpread(
            leadToSweepDistanceMeters = 3100.0,
            thresholdMeters = 2500.0
        )
        assertThat(stretchedStatus).isEqualTo(ConvoySpreadStatus.STRETCHED)
    }
}
