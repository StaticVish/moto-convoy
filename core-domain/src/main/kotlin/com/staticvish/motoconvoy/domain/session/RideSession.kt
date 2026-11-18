package com.staticvish.motoconvoy.domain.session

import java.security.SecureRandom

data class RideSession(
    val sessionCode: String,
    val sessionName: String,
    val hostRiderId: String,
    val createdAtMs: Long = System.currentTimeMillis(),
    val maxSpreadMeters: Double = 2000.0
) {
    companion object {
        private const val ALPHANUM = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        private val random = SecureRandom()

        fun generateSessionCode(prefix: String = "MOTO"): String {
            val sb = StringBuilder(prefix).append("-")
            for (i in 0 until 4) {
                sb.append(ALPHANUM[random.nextInt(ALPHANUM.length)])
            }
            return sb.toString()
        }
    }
}
