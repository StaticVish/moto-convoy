package com.staticvish.motoconvoy.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.staticvish.motoconvoy.domain.model.ConvoyRole
import com.staticvish.motoconvoy.domain.telemetry.ConvoySpreadStatus
import com.staticvish.motoconvoy.domain.telemetry.RiderTelemetry
import com.staticvish.motoconvoy.domain.telemetry.SpatialCalculator
import com.staticvish.motoconvoy.simulation.ConvoySimulator
import com.staticvish.motoconvoy.simulation.ConvoySnapshot
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

enum class DisplayMode {
    MAP,
    RADAR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConvoyHudScreen(
    simulator: ConvoySimulator = remember { ConvoySimulator() }
) {
    val snapshot by simulator.snapshot.collectAsState()
    var isSimulating by remember { mutableStateOf(true) }
    var displayMode by remember { mutableStateOf(DisplayMode.MAP) }
    var showJoinDialog by remember { mutableStateOf(false) }

    // User profile state
    var myName by remember { mutableStateOf("Vish") }
    var myBike by remember { mutableStateOf("Tiger 900 Rally Pro") }
    var sessionCode by remember { mutableStateOf("MOTO-KL89") }

    // Simulation loop
    LaunchedEffect(isSimulating) {
        while (isSimulating) {
            delay(1500)
            simulator.tick()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0E17))
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        // App Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "MOTO CONVOY RADAR v1.0",
                    color = Color(0xFF38BDF8),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Session: $sessionCode • Karak Loop",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // View Mode Switcher: MAP vs RADAR
                Surface(
                    color = if (displayMode == DisplayMode.MAP) Color(0xFF0284C7) else Color(0xFF1E293B),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable { displayMode = DisplayMode.MAP }
                ) {
                    Text(
                        text = "MAP",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }

                Surface(
                    color = if (displayMode == DisplayMode.RADAR) Color(0xFF0284C7) else Color(0xFF1E293B),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable { displayMode = DisplayMode.RADAR }
                ) {
                    Text(
                        text = "RADAR",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }

                Surface(
                    color = if (isSimulating) Color(0xFF065F46) else Color(0xFF374151),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable { showJoinDialog = true }
                ) {
                    Text(
                        text = if (isSimulating) "● LIVE" else "■ PAUSE",
                        color = if (isSimulating) Color(0xFF34D399) else Color(0xFF9CA3AF),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Top Tactical HUD Bar
        TacticalHudBar(snapshot = snapshot)

        Spacer(modifier = Modifier.height(12.dp))

        // Main Visual Area: OSM Map vs Tactical Radar Canvas
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF111827))
                .border(1.dp, Color(0xFF1F2937), RoundedCornerShape(16.dp))
        ) {
            if (displayMode == DisplayMode.MAP) {
                TacticalOsmMapView(snapshot = snapshot)
            } else {
                TacticalRadarCanvas(snapshot = snapshot)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Glove-Friendly Tactical Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { isSimulating = !isSimulating },
                modifier = Modifier.weight(1f).height(46.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(if (isSimulating) "Pause" else "Resume", fontSize = 12.sp, color = Color.White)
            }

            Button(
                onClick = { simulator.toggleStretchedPack() },
                modifier = Modifier.weight(1.3f).height(46.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (snapshot.spreadStatus == ConvoySpreadStatus.STRETCHED) Color(0xFFB45309) else Color(0xFF1E293B)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Test Stretch", fontSize = 12.sp, color = Color.White)
            }

            Button(
                onClick = { simulator.toggleEmergencySos() },
                modifier = Modifier.weight(1f).height(46.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (snapshot.isEmergencyActive) Color(0xFFDC2626) else Color(0xFF1E293B)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Test SOS", fontSize = 12.sp, color = Color.White)
            }

            Button(
                onClick = { showJoinDialog = true },
                modifier = Modifier.weight(0.9f).height(46.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Role", fontSize = 12.sp, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "ACTIVE CONVOY ROSTER (${snapshot.riders.size})",
            color = Color(0xFF94A3B8),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Roster of active riders
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(snapshot.riders, key = { it.riderId }) { rider ->
                RiderRosterItem(rider = rider, leadRider = snapshot.leadRider)
            }
        }
    }

    // Role / Session Configuration Dialog
    if (showJoinDialog) {
        AlertDialog(
            onDismissRequest = { showJoinDialog = false },
            title = {
                Text("Convoy Role & Session", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = myName,
                        onValueChange = { myName = it },
                        label = { Text("Rider Name") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = myBike,
                        onValueChange = { myBike = it },
                        label = { Text("Bike Model") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = sessionCode,
                        onValueChange = { sessionCode = it.uppercase() },
                        label = { Text("Ride Session Code") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showJoinDialog = false }) {
                    Text("Save & Join")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJoinDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1E293B)
        )
    }
}

@Composable
fun TacticalHudBar(snapshot: ConvoySnapshot) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111827)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Safety Status Alert
            AnimatedVisibility(
                visible = snapshot.isEmergencyActive,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Surface(
                    color = Color(0xFF7F1D1D),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                ) {
                    Text(
                        text = "🚨 SOS BEACON: SWEEP (DENNIS) ASSISTANCE REQUIRED",
                        color = Color(0xFFFECACA),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            if (snapshot.spreadStatus == ConvoySpreadStatus.STRETCHED && !snapshot.isEmergencyActive) {
                Surface(
                    color = Color(0xFF78350F),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                ) {
                    Text(
                        text = "⚠️ CONVOY STRETCHED: SPREAD EXCEEDS 2.0 KM",
                        color = Color(0xFFFDE68A),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Lead Status
                Column {
                    Text("ROAD LEAD", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = snapshot.leadRider?.riderName ?: "N/A",
                        color = Color(0xFFF59E0B),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "${snapshot.leadRider?.speedKmh?.toInt() ?: 0} km/h",
                        color = Color(0xFFE2E8F0),
                        fontSize = 12.sp
                    )
                }

                // Total Convoy Spread
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("PACK SPREAD", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = SpatialCalculator.formatDistanceDelta(snapshot.convoySpreadMeters).replace("+", ""),
                        color = if (snapshot.spreadStatus == ConvoySpreadStatus.STRETCHED) Color(0xFFEF4444) else Color(0xFF10B981),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = snapshot.spreadStatus.name,
                        color = if (snapshot.spreadStatus == ConvoySpreadStatus.STRETCHED) Color(0xFFF87171) else Color(0xFF34D399),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Sweep Status
                Column(horizontalAlignment = Alignment.End) {
                    Text("TAIL SWEEP", color = Color(0xFF94A3B8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = snapshot.sweepRider?.riderName ?: "N/A",
                        color = Color(0xFFEF4444),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "${snapshot.sweepRider?.speedKmh?.toInt() ?: 0} km/h",
                        color = Color(0xFFE2E8F0),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun TacticalRadarCanvas(
    snapshot: ConvoySnapshot,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f

        // Draw radar concentric rings
        drawCircle(color = Color(0xFF1F2937), radius = size.height * 0.42f, style = Stroke(width = 1f))
        drawCircle(color = Color(0xFF1F2937), radius = size.height * 0.25f, style = Stroke(width = 1f))
        drawLine(color = Color(0xFF1F2937), start = Offset(centerX, 0f), end = Offset(centerX, size.height), strokeWidth = 1f)
        drawLine(color = Color(0xFF1F2937), start = Offset(0f, centerY), end = Offset(size.width, centerY), strokeWidth = 1f)

        val lead = snapshot.leadRider ?: return@Canvas

        // Plot riders relative to Lead (Lead at top center)
        snapshot.riders.forEach { rider ->
            val dist = SpatialCalculator.haversineDistanceMeters(lead.latitude, lead.longitude, rider.latitude, rider.longitude)
            val bearing = SpatialCalculator.calculateBearing(lead.latitude, lead.longitude, rider.latitude, rider.longitude)

            // Convert to radar polar coordinates
            val radarScale = size.height * 0.35f / 2500f // 2.5km radar scope
            val r = (dist * radarScale).toFloat().coerceAtMost(size.height * 0.42f)
            val rad = Math.toRadians(bearing - 90.0) // Adjust for canvas coordinates

            val px = (centerX + r * cos(rad)).toFloat()
            val py = (centerY + r * sin(rad)).toFloat()

            val roleColor = when (rider.role) {
                ConvoyRole.LEAD -> Color(0xFFF59E0B)
                ConvoyRole.LEFT_FLANK -> Color(0xFF06B6D4)
                ConvoyRole.RIGHT_FLANK -> Color(0xFF3B82F6)
                ConvoyRole.SLIDER -> Color(0xFFA855F7)
                ConvoyRole.PACK -> Color(0xFF10B981)
                ConvoyRole.SWEEP -> Color(0xFFEF4444)
            }

            // Draw rider blip
            if (rider.isEmergency) {
                drawCircle(color = Color(0xFFEF4444).copy(alpha = 0.4f), radius = 16f, center = Offset(px, py))
            }
            drawCircle(color = roleColor, radius = 7f, center = Offset(px, py))
        }
    }
}

@Composable
fun RiderRosterItem(
    rider: RiderTelemetry,
    leadRider: RiderTelemetry?
) {
    val roleColor = when (rider.role) {
        ConvoyRole.LEAD -> Color(0xFFF59E0B)
        ConvoyRole.LEFT_FLANK -> Color(0xFF06B6D4)
        ConvoyRole.RIGHT_FLANK -> Color(0xFF3B82F6)
        ConvoyRole.SLIDER -> Color(0xFFA855F7)
        ConvoyRole.PACK -> Color(0xFF10B981)
        ConvoyRole.SWEEP -> Color(0xFFEF4444)
    }

    val gapText = if (leadRider != null && rider.riderId != leadRider.riderId) {
        val dist = SpatialCalculator.haversineDistanceMeters(leadRider.latitude, leadRider.longitude, rider.latitude, rider.longitude)
        SpatialCalculator.formatDistanceDelta(-dist)
    } else {
        "LEAD"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF111827))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(roleColor)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "${rider.riderName} • ${rider.bikeModel}",
                    color = Color(0xFFF1F5F9),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${rider.role.title} • ${rider.speedKmh.toInt()} km/h",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
            }
        }

        Surface(
            color = Color(0xFF1E293B),
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                text = gapText,
                color = roleColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}
