package com.staticvish.motoconvoy.domain.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ConvoyRoleTest {

    @Test
    fun `convoy roles have strict tactical priority ordering from lead to sweep`() {
        val roles = listOf(
            ConvoyRole.PACK,
            ConvoyRole.SWEEP,
            ConvoyRole.LEAD,
            ConvoyRole.RIGHT_FLANK,
            ConvoyRole.LEFT_FLANK,
            ConvoyRole.SLIDER
        )

        val sortedRoles = roles.sortedBy { it.priority }

        assertThat(sortedRoles).containsExactly(
            ConvoyRole.LEAD,
            ConvoyRole.LEFT_FLANK,
            ConvoyRole.RIGHT_FLANK,
            ConvoyRole.SLIDER,
            ConvoyRole.PACK,
            ConvoyRole.SWEEP
        )
    }

    @Test
    fun `lead and sweep have distinct safety badges and colors`() {
        assertThat(ConvoyRole.LEAD.badgeColorHex).isEqualTo("#F59E0B") // Gold
        assertThat(ConvoyRole.SWEEP.badgeColorHex).isEqualTo("#EF4444") // Red
        assertThat(ConvoyRole.LEFT_FLANK.badgeColorHex).isEqualTo("#06B6D4") // Cyan
        assertThat(ConvoyRole.RIGHT_FLANK.badgeColorHex).isEqualTo("#3B82F6") // Blue
        assertThat(ConvoyRole.SLIDER.badgeColorHex).isEqualTo("#A855F7") // Purple
        assertThat(ConvoyRole.PACK.badgeColorHex).isEqualTo("#10B981") // Green
    }

    @Test
    fun `rider model formats tactical tag with bike model`() {
        val rider = Rider(
            id = "rider-1",
            name = "Vish",
            bikeModel = "Tiger 900 Rally Pro",
            role = ConvoyRole.LEAD
        )

        assertThat(rider.tacticalTag).isEqualTo("Vish (Tiger 900 Rally Pro) [LEAD]")
        assertThat(rider.isLead).isTrue()
        assertThat(rider.isSweep).isFalse()
    }
}
