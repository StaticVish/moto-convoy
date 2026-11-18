package com.staticvish.motoconvoy.domain.model

data class Rider(
    val id: String,
    val name: String,
    val bikeModel: String,
    val role: ConvoyRole,
    val isEmergency: Boolean = false
) {
    val tacticalTag: String
        get() = "$name ($bikeModel) [${role.name}]"

    val isLead: Boolean
        get() = role == ConvoyRole.LEAD

    val isSweep: Boolean
        get() = role == ConvoyRole.SWEEP
}
