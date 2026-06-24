package com.example.security

import androidx.compose.runtime.mutableStateOf
import java.io.File
import java.security.MessageDigest

object SecurityState {
    var isAirgapShieldEnabled = mutableStateOf(true) // Default to secure high-isolated mode
    var isTamperProtectionActive = mutableStateOf(true)
    var isVaultLocked = mutableStateOf(true)
    var vaultPasscode = "0000" // Default offline sandbox pin
    var isVaultConfigured = mutableStateOf(false)
    
    // Track intrusion alerts
    var totalThreatsBlocked = mutableStateOf(0)
    var recentThreatAlerts = mutableStateOf(listOf<String>())
    
    fun registerAlert(alert: String) {
        val currentList = recentThreatAlerts.value
        recentThreatAlerts.value = listOf("[ALERT] $alert") + currentList.take(19)
        totalThreatsBlocked.value += 1
    }

    // SHA-256 Utility for verifying file integrity
    fun calculateSHA256(file: File): String {
        if (!file.exists() || file.isDirectory) return "N/A"
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(8192)
                var bytesRead = input.read(buffer)
                while (bytesRead != -1) {
                    digest.update(buffer, 0, bytesRead)
                    bytesRead = input.read(buffer)
                }
            }
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            "ERR_HASH"
        }
    }
}
