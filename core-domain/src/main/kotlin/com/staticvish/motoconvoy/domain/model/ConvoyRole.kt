package com.staticvish.motoconvoy.domain.model

enum class ConvoyRole(
    val priority: Int,
    val title: String,
    val badgeColorHex: String
) {
    LEAD(priority = 0, title = "Lead / Road Captain", badgeColorHex = "#F59E0B"),
    LEFT_FLANK(priority = 1, title = "Left Flank Marshal", badgeColorHex = "#06B6D4"),
    RIGHT_FLANK(priority = 2, title = "Right Flank Marshal", badgeColorHex = "#3B82F6"),
    SLIDER(priority = 3, title = "Mid Slider Marshal", badgeColorHex = "#A855F7"),
    PACK(priority = 4, title = "Pack Rider", badgeColorHex = "#10B981"),
    SWEEP(priority = 5, title = "Sweep / Tail Gunner", badgeColorHex = "#EF4444");
}
