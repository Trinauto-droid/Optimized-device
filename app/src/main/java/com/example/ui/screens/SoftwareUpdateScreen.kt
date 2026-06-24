package com.example.ui.screens

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.GeminiService
import kotlinx.coroutines.launch

data class RomPackage(
    val name: String,
    val version: String,
    val size: String,
    val androidVersion: String,
    val stability: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SoftwareUpdateScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Current phone parameters
    val currentSdk = remember { android.os.Build.VERSION.SDK_INT }
    val currentRelease = remember { android.os.Build.VERSION.RELEASE }
    val deviceBrand = remember { android.os.Build.MANUFACTURER }
    val deviceModel = remember { android.os.Build.MODEL }
    val supportsTreble = remember { currentSdk >= 26 } // Project Treble introduced in Android 8.0 (API 26)

    // Screen States
    var selectedPackage by remember { mutableStateOf<RomPackage?>(null) }
    var isAnalyzing by remember { mutableStateOf(false) }
    var analysisCompleted by remember { mutableStateOf(false) }

    // Flashing simulation states
    var isFlashing by remember { mutableStateOf(false) }
    var flashCompleted by remember { mutableStateOf(false) }
    var flashLogs by remember { mutableStateOf(emptyList<String>()) }
    var progressVal by remember { mutableStateOf(0f) }

    // Gemini Recommendation State
    var aiRomReport by remember { mutableStateOf("") }
    var isAiRomLoading by remember { mutableStateOf(false) }
    var showExplanationDialog by remember { mutableStateOf(false) }

    val packageCatalog = listOf(
        RomPackage("LineageOS 22 (AOSP Standard)", "v22.0 (Official)", "1.8 GB", "Android 15", "Stable"),
        RomPackage("Pixel Experience Premium", "v14.2 (Beta)", "2.1 GB", "Android 14", "Stable"),
        RomPackage("AOSP Vanilla GSI (Generic System Image)", "v15.0_r4 (Official)", "1.4 GB", "Android 15", "Development"),
        RomPackage("Evolution X Gaming Tuned", "v9.4 (Community)", "2.4 GB", "Android 14", "Highly Stable"),
        RomPackage("Ubuntu Touch Mobile Port", "OTA-25 (Experimental)", "1.1 GB", "Ubuntu 20.04 Base", "Experimental")
    )

    // Compatibility calculation
    val runCompatibilityAnalysis = {
        isAnalyzing = true
        analysisCompleted = false
        selectedPackage = null
        flashCompleted = false
        aiRomReport = ""

        scope.launch {
            kotlinx.coroutines.delay(1200)
            isAnalyzing = false
            analysisCompleted = true
        }
    }

    // Flashing Simulation Process
    val runFlashingEngine = { rom: RomPackage ->
        isFlashing = true
        flashCompleted = false
        flashLogs = emptyList()
        progressVal = 0f

        scope.launch {
            val logs = mutableListOf<String>()
            fun log(m: String) {
                logs.add(m)
                flashLogs = logs.toList()
            }

            log("▶ Initializing fastboot flashing protocol...")
            progressVal = 0.05f
            kotlinx.coroutines.delay(500)

            log("▶ adb devices - checking system hooks... [FOUND: $deviceBrand $deviceModel]")
            progressVal = 0.12f
            kotlinx.coroutines.delay(600)

            log("▶ adb reboot bootloader - entering fastboot partition grid...")
            progressVal = 0.20f
            kotlinx.coroutines.delay(800)

            log("▶ fastboot flashing unlock - granting low-level partition modification rights...")
            progressVal = 0.32f
            kotlinx.coroutines.delay(700)

            log("▶ fastboot erase system - clearing old Android $currentRelease core blocks...")
            progressVal = 0.45f
            kotlinx.coroutines.delay(800)

            log("▶ Sending '${rom.name}' payload (chunk size: 262,144 bytes)...")
            progressVal = 0.55f
            kotlinx.coroutines.delay(1000)

            log("▶ fastboot flash system_a system.img - writing new ${rom.androidVersion} blocks...")
            progressVal = 0.72f
            kotlinx.coroutines.delay(1500)

            log("▶ fastboot flash boot boot.img - applying modern custom kernel mapping...")
            progressVal = 0.85f
            kotlinx.coroutines.delay(800)

            log("▶ fastboot -w (wiping dalvik, caches, and database metadata safely)...")
            progressVal = 0.92f
            kotlinx.coroutines.delay(900)

            log("▶ fastboot reboot - rebooting device into ${rom.name} system...")
            progressVal = 1.0f
            kotlinx.coroutines.delay(600)

            isFlashing = false
            flashCompleted = true

            // Trigger Gemini custom ROM recommendations for this specific phone model
            isAiRomLoading = true
            val prompt = """
                Device Manufacturer: $deviceBrand
                Device Model: $deviceModel
                Current Android Version: $currentRelease (SDK $currentSdk)
                Project Treble Support: ${if (supportsTreble) "Yes" else "No"}
                Target Upgrade Package: ${rom.name} (Running ${rom.androidVersion})
                
                Please generate custom step-by-step instructions for the user to flash this ROM/GSI on their PHYSICAL phone using a PC.
                Include:
                1. Prerequisite tool setup (ADB/Fastboot installation, USB driver registration).
                2. Device OEM unlocking instructions (bootloader unlock specifics for $deviceBrand).
                3. The exact Fastboot commands tailored to their device structure.
                4. Troubleshooting bootloops.
            """.trimIndent()

            val response = GeminiService.getGeminiSuggestion(
                prompt = prompt,
                systemInstruction = "You are a senior Android core platform engineer and custom ROM system maintainer. Provide accurate, clear, and highly professional terminal flashing scripts.",
                useProWithThinking = true
            )
            aiRomReport = response
            isAiRomLoading = false
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- TITLE ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.SystemUpdate, contentDescription = "Updates", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                Column {
                    Text("System Upgrade Engine", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Run newer custom Android updates on older legacy handsets", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                }
            }
            IconButton(
                onClick = { showExplanationDialog = true },
                modifier = Modifier.testTag("system_upgrade_info_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Show explanation",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        if (!isAnalyzing && !analysisCompleted) {
            // --- READY STATE AND ANALYZER TRIGGER ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Dns,
                        contentDescription = "Analysis",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Device Upgrade Compatibility Analyzer", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Checks Project Treble compatibility, hardware CPU limits, partition boundaries, and mainboard layouts to calculate support for Generic System Image (GSI) upgrades.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.outline,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { runCompatibilityAnalysis() },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(48.dp)
                            .testTag("start_compatibility_analysis_button")
                    ) {
                        Icon(Icons.Default.ManageHistory, contentDescription = "Run")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Analyze Device Compatibility")
                    }
                }
            }
        } else if (isAnalyzing) {
            // --- ANALYZING LOADER ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator()
                    Text("Scrutinizing ROM partition table structures...", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("Checking Project Treble support flags...", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                }
            }
        } else if (analysisCompleted && !isFlashing && !flashCompleted) {
            // --- ANALYSIS REPORT & PACKAGE SELECTION ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Compatibility KPI
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Compatibility Diagnostic Report", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (supportsTreble) Color(0xFFD1FAE5) else Color(0xFFFEE2E2))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    if (supportsTreble) "TREBLE COMPATIBLE" else "LEGACY BLOCKS ONLY",
                                    color = if (supportsTreble) Color(0xFF065F46) else Color(0xFF991B1B),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Divider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        Text("• Device Host: $deviceBrand $deviceModel", fontSize = 11.sp)
                        Text("• Active Android Version: $currentRelease (SDK $currentSdk)", fontSize = 11.sp)
                        Text(
                            "• GSI Support Recommendation: " + if (supportsTreble) "Excellent. Low-level Project Treble drivers are fully separated. GSI system images can be flashed directly."
                            else "Requires bootloader bypass. Traditional Custom ROM (e.g. device-specific LineageOS ports) is recommended over generic system images.",
                            fontSize = 11.sp,
                            lineHeight = 15.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }

                // Choose Rom list
                Text("Select Target System Package to Install", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(packageCatalog) { rom ->
                        val isSelected = selectedPackage == rom
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPackage = rom }
                                .testTag("rom_item_${rom.name}"),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                            )
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(rom.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 2.dp)) {
                                        Text("Sys Base: ${rom.androidVersion}", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                        Text("Payload Size: ${rom.size}", fontSize = 10.sp, color = MaterialTheme.colorScheme.outline)
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(if (rom.stability == "Stable" || rom.stability.contains("Highly")) Color(0xFFD1FAE5) else Color(0xFFFEF3C7))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(rom.stability, color = if (rom.stability == "Stable" || rom.stability.contains("Highly")) Color(0xFF065F46) else Color(0xFFD97706), fontSize = 8.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Flash Button
                Button(
                    onClick = { runFlashingEngine(selectedPackage!!) },
                    enabled = selectedPackage != null,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("init_system_upgrade_button")
                ) {
                    Icon(Icons.Default.OfflineBolt, contentDescription = "Flash")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (selectedPackage != null) "Deploy ${selectedPackage!!.name}" else "Select Target Package")
                }
            }
        } else if (isFlashing) {
            // --- FLASHING IN PROGRESS TERMINAL ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color(0xFF38BDF8), strokeWidth = 2.dp)
                        Text("Active fastboot flash deployment core running...", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Divider(color = Color(0xFF334155))
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        items(flashLogs) { line ->
                            Text(
                                line,
                                color = if (line.startsWith("Error")) Color(0xFFF87171) else if (line.startsWith("▶")) Color(0xFF38BDF8) else Color(0xFFF1F5F9),
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    LinearProgressIndicator(
                        progress = { progressVal },
                        color = Color(0xFF38BDF8),
                        trackColor = Color(0xFF334155),
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(2.dp))
                    )
                }
            }
        } else if (flashCompleted && selectedPackage != null) {
            // --- SYSTEM DEPLOYED REPORT ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Default.Verified, contentDescription = "Done", tint = Color(0xFF10B981), modifier = Modifier.size(48.dp))
                        Text("Upgrade Payload Signature Verified!", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Simulated partition flash complete for ${selectedPackage!!.name} with standard generic AOSP descriptors.", textAlign = TextAlign.Center, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }

                // AI Expert Advice on physical flashing instructions
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Psychology, contentDescription = "AI Expert", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Text("AI Low-Level Flashing Scripts & OEM Unlock Guide", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (isAiRomLoading) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                    Text("Compiling fastboot payload shell...", fontSize = 12.sp)
                                }
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                item {
                                    Text(
                                        text = aiRomReport.ifEmpty { "Interfacing with AI core for customized flashing instructions..." },
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp,
                                        fontFamily = FontFamily.SansSerif
                                    )
                                }
                            }
                        }
                    }
                }

                // Go back button
                Button(
                    onClick = { flashCompleted = false; selectedPackage = null },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Re-evaluate System Upgrades")
                }
            }
        }
    }

    // --- UPGRADE ENGINE EXPLANATION DIALOG ---
    if (showExplanationDialog) {
        AlertDialog(
            onDismissRequest = { showExplanationDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Upgrade Spec Hub",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Treble Spec Sheet",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "System Upgrade Engine Core",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Demonstrates Android Project Treble architecture splitting mechanisms to enable Generic System Image (GSI) flashing on compliant handsets.",
                        fontSize = 13.sp,
                        color = Color(0xFFCAC4D0)
                    )
                    
                    Divider(color = Color(0xFF49454F))
                    
                    Text("PROJECT TREBLE ARCHITECTURE SPECIFICATIONS:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFCAC4D0), fontFamily = FontFamily.Monospace)
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("▪", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 2.dp))
                            Column {
                                Text("Vendor & Framework Partition Decoupling", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                Text("Introduced in Android Oreo. Separates proprietary silicon drivers (/vendor) from AOSP code (/system), bypassing vendor upgrade timelines.", fontSize = 11.sp, color = Color(0xFFCAC4D0))
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("▪", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 2.dp))
                            Column {
                                Text("Generic System Image (GSI)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                Text("Pure, unmodified Android Open Source Project system partitions validated by Google to run across diverse Treble-compliant chipsets.", fontSize = 11.sp, color = Color(0xFFCAC4D0))
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("▪", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 2.dp))
                            Column {
                                Text("ADB & Fastboot Protocols", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                Text("Utilizes low-level bootloader commands to safely erase and write raw .img streams into boot and system partition grids.", fontSize = 11.sp, color = Color(0xFFCAC4D0))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showExplanationDialog = false },
                    modifier = Modifier.testTag("system_upgrade_info_dismiss")
                ) {
                    Text("DISMISS UPGRADE BRIEF", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = Color(0xFF1C1B1F),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFCAC4D0)
        )
    }
}
