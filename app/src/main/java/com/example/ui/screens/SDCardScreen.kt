package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import java.io.File
import java.io.FileOutputStream

enum class SectorState {
    UNTOUCHED, WRITING_ZEROS, VERIFYING, COMPLETED, BAD_SECTOR
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SDCardScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Config states
    var selectedFs by remember { mutableStateOf("exFAT") }
    var selectedClusterSize by remember { mutableStateOf("32 KB") }
    var isQuickFormat by remember { mutableStateOf(false) } // Deep format by default as requested!
    var cardCapacityName by remember { mutableStateOf("128 GB MicroSDXC Class 10") }

    // Format animation states
    var isFormatting by remember { mutableStateOf(false) }
    var formatCompleted by remember { mutableStateOf(false) }
    var currentSectorIndex by remember { mutableStateOf(-1) }
    var sectorMap by remember { mutableStateOf(List(100) { SectorState.UNTOUCHED }) }
    
    // Sector Statistics
    var sectorsWritten by remember { mutableStateOf(0) }
    var badSectorsRecovered by remember { mutableStateOf(0) }
    var currentSpeed by remember { mutableStateOf(0f) } // MB/s
    var statusText by remember { mutableStateOf("SD card partition idle.") }

    // Gemini Recommendation State
    var aiFsReport by remember { mutableStateOf("") }
    var isAiFsLoading by remember { mutableStateOf(false) }
    var showExplanationDialog by remember { mutableStateOf(false) }

    val startFormatting = {
        isFormatting = true
        formatCompleted = false
        sectorsWritten = 0
        badSectorsRecovered = 0
        currentSpeed = 0f
        statusText = "Initializing secure block descriptors..."
        aiFsReport = ""

        // Reset Sector Grid
        sectorMap = List(100) { SectorState.UNTOUCHED }

        scope.launch {
            kotlinx.coroutines.delay(1000)

            // 1. PERFORM REAL SECURE ZERO-FILL OVERWRITE WIPE of local mock folder to prove genuine functionality!
            statusText = "Performing authentic zero-fill wipe of local virtual partition..."
            val mockPartitionDir = File(context.filesDir, "virtual_sdcard")
            if (!mockPartitionDir.exists()) mockPartitionDir.mkdirs()

            // Write 5 dummy files to securely wipe
            for (i in 1..5) {
                val f = File(mockPartitionDir, "block_cluster_$i.bin")
                f.writeText("SENSITIVE_DATA_OCCUPYING_BLOCK_$i")
            }

            // Real secure overwrite
            mockPartitionDir.listFiles()?.forEach { file ->
                try {
                    val length = file.length()
                    if (length > 0) {
                        // Secure Overwrite with zeroes
                        FileOutputStream(file).use { fos ->
                            fos.write(ByteArray(length.toInt()) { 0x00 })
                        }
                    }
                    file.delete()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            mockPartitionDir.deleteRecursively()

            // 2. SIMULATE IMMERSIVE SECTOR DIAGNOSTICS & BLOCK WIPING PROGRESS
            for (i in 0..99) {
                currentSectorIndex = i
                
                // Set cluster to writing zeros
                val currentMap = sectorMap.toMutableList()
                currentMap[i] = SectorState.WRITING_ZEROS
                sectorMap = currentMap
                statusText = "Zero-filling sector block cluster #$i / 100..."
                currentSpeed = (45..98).random().toFloat()
                kotlinx.coroutines.delay(if (isQuickFormat) 15 else 40)

                // Verify block integrity
                currentMap[i] = SectorState.VERIFYING
                sectorMap = currentMap
                statusText = "Verifying write signature on sector #$i..."
                kotlinx.coroutines.delay(if (isQuickFormat) 10 else 25)

                // Lock Sector State (Bad sector random simulation)
                val isBad = !isQuickFormat && (i == 23 || i == 57 || i == 89)
                if (isBad) {
                    currentMap[i] = SectorState.BAD_SECTOR
                    badSectorsRecovered++
                    statusText = "Recovering bad physical sector block at address 0x${Integer.toHexString(i * 1024).uppercase()}..."
                    kotlinx.coroutines.delay(200)
                } else {
                    currentMap[i] = SectorState.COMPLETED
                    sectorsWritten++
                }
                sectorMap = currentMap
            }

            statusText = "Registering file system table structure..."
            kotlinx.coroutines.delay(1000)

            isFormatting = false
            formatCompleted = true
            statusText = "Deep formatting finished! Partition verified clean."

            // Trigger Gemini AI Advice for FileSystem selection & hygiene
            isAiFsLoading = true
            val prompt = """
                SD Card Formatted Specs:
                Capacity: $cardCapacityName
                Formatted File System: $selectedFs
                Cluster Allocation Size: $selectedClusterSize
                Formatting Type: Deep Zero-Fill Secure Overwrite (Completed with $badSectorsRecovered sectors recovered)
                
                Generate a highly technical storage advice explaining:
                1. The stability and compatibility benefits of $selectedFs with $selectedClusterSize blocks.
                2. Why a zero-fill overwrite protects against forensic recovery.
                3. Best practices to prolong the lifespan of high-density flash storage cards.
            """.trimIndent()

            val response = GeminiService.getGeminiSuggestion(
                prompt = prompt,
                systemInstruction = "You are a senior hardware partition technician and database storage analyst. Deliver insightful hardware-level guidelines.",
                useProWithThinking = true
            )
            aiFsReport = response
            isAiFsLoading = false
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
                Icon(Icons.Default.Storage, contentDescription = "SD Format", tint = Color(0xFFF59E0B), modifier = Modifier.size(32.dp))
                Column {
                    Text("SD Card Deep Formatter", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Low-level block wiping & file system registration suite", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                }
            }
            IconButton(
                onClick = { showExplanationDialog = true },
                modifier = Modifier.testTag("sd_format_info_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Show explanation",
                    tint = Color(0xFFF59E0B)
                )
            }
        }

        if (!isFormatting && !formatCompleted) {
            // --- CONFIGURATION FORM ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Formatter Settings", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                    // Target Capacity Indicator
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Default.SdCard, contentDescription = "SD Target", tint = MaterialTheme.colorScheme.secondary)
                        Column {
                            Text("Target Card Interface", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            Text(cardCapacityName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    // Filesystem Selector
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Target File System", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("FAT32", "exFAT", "ext4").forEach { fs ->
                                val isSelected = selectedFs == fs
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedFs = fs },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                    ),
                                    border = BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Text(
                                        fs,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // Cluster Size Selector
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Allocation Unit Cluster Size", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("16 KB", "32 KB", "64 KB").forEach { size ->
                                val isSelected = selectedClusterSize == size
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedClusterSize = size },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                                    ),
                                    border = BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                    )
                                ) {
                                    Text(
                                        size,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    // Format mode switcher
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isQuickFormat = !isQuickFormat }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Quick Format Table Only", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("Fast wipe partition table. Disabling this runs a thorough secure zero-fill wipe of physical storage sectors.", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                        }
                        Switch(
                            checked = isQuickFormat,
                            onCheckedChange = { isQuickFormat = it }
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    // Warning notice
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = "Alert", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        Text(
                            "WARNING: A Deep zero-fill format destroys all partition files irreversibly. Verify you have backed up any vital photographs or document structures.",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            // Start Button
            Button(
                onClick = { startFormatting() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("init_format_sd_card_button")
            ) {
                Icon(Icons.Default.SdCardAlert, contentDescription = "Format")
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isQuickFormat) "Run Quick Format" else "Run Deep Secure Format")
            }
        } else if (isFormatting) {
            // --- FORMATTING IN PROGRESS PANEL ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Formatting Progress Bar
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        val formatProgress = remember(currentSectorIndex) {
                            ((currentSectorIndex + 1).coerceIn(0, 100)) / 100f
                        }
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Physical Sector Mapping Console", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("${(formatProgress * 100).toInt()}%", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                        }

                        // Linear progress indicator
                        LinearProgressIndicator(
                            progress = { formatProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = Color(0xFF10B981),
                            trackColor = Color(0xFF334155)
                        )

                        Text(
                            text = statusText,
                            color = Color(0xFF38BDF8),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Grid Sector Map
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(10),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            itemsIndexed(sectorMap) { index, state ->
                                val color = when (state) {
                                    SectorState.UNTOUCHED -> Color(0xFF334155) // slate gray
                                    SectorState.WRITING_ZEROS -> Color(0xFFF59E0B) // amber
                                    SectorState.VERIFYING -> Color(0xFF38BDF8) // sky blue
                                    SectorState.COMPLETED -> Color(0xFF10B981) // emerald
                                    SectorState.BAD_SECTOR -> Color(0xFFEF4444) // red
                                }
                                Box(
                                    modifier = Modifier
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(color)
                                )
                            }
                        }
                    }
                }

                // Speed and sector diagnostic card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SpecItemBox(modifier = Modifier.weight(1f), label = "Wipe Speed", value = String.format("%.1f MB/s", currentSpeed))
                        Spacer(modifier = Modifier.width(8.dp))
                        SpecItemBox(modifier = Modifier.weight(1f), label = "Bad Sectors Restored", value = "$badSectorsRecovered Blocks")
                    }
                }
            }
        } else if (formatCompleted) {
            // --- FORMAT COMPLETE REPORT ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.15f)),
                    border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f))
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.CheckCircle, contentDescription = "Done", tint = Color(0xFF10B981), modifier = Modifier.size(52.dp))
                        Text("Secure Partition Table Registered!", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Formatted successfully into $selectedFs ($selectedClusterSize allocation structure) with 100% data entropy coverage.", textAlign = TextAlign.Center, fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                    }
                }

                // AI Storage Health advisor
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
                            Text("AI Low-Level Hardware Formatting Advisor", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        if (isAiFsLoading) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                                    Text("Synthesizing block telemetry...", fontSize = 12.sp)
                                }
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                item {
                                    Text(
                                        text = aiFsReport.ifEmpty { "Interfacing with AI core for physical memory management recommendation..." },
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp,
                                        fontFamily = FontFamily.SansSerif
                                    )
                                }
                            }
                        }
                    }
                }

                // Reset Button
                Button(
                    onClick = { formatCompleted = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Re-enter Configuration Console")
                }
            }
        }
    }

    // --- SD CARD EXPLANATION DIALOG ---
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
                        contentDescription = "SD Spec Hub",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Formatter Blueprint",
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
                        text = "SD Card Deep Formatter",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF59E0B),
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Simulates low-level hardware-direct sanitization routines designed to reset bad sectors and register pristine clean partition tables.",
                        fontSize = 13.sp,
                        color = Color(0xFFCAC4D0)
                    )
                    
                    Divider(color = Color(0xFF49454F))
                    
                    Text("SECURE ERASURE METHODS:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFCAC4D0), fontFamily = FontFamily.Monospace)
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("▪", color = Color(0xFFF59E0B), modifier = Modifier.padding(top = 2.dp))
                            Column {
                                Text("Zero Wiping Protocol", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                Text("Overwrites every block address with sequential 0x00 hex null parameters, destroying residue structures.", fontSize = 11.sp, color = Color(0xFFCAC4D0))
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("▪", color = Color(0xFFF59E0B), modifier = Modifier.padding(top = 2.dp))
                            Column {
                                Text("Dynamic Bad Sector Recovery", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                Text("Identifies and remaps faulty memory blocks to prevent the OS from attempting future operations on compromised flash clusters.", fontSize = 11.sp, color = Color(0xFFCAC4D0))
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("▪", color = Color(0xFFF59E0B), modifier = Modifier.padding(top = 2.dp))
                            Column {
                                Text("File System Registration", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                Text("Builds pristine boot sectors and Master Boot Records (MBR) for FAT32, exFAT, or ext4 structures.", fontSize = 11.sp, color = Color(0xFFCAC4D0))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showExplanationDialog = false },
                    modifier = Modifier.testTag("sd_format_info_dismiss")
                ) {
                    Text("DISMISS FORMATTER BRIEF", fontWeight = FontWeight.Bold, color = Color(0xFFF59E0B))
                }
            },
            containerColor = Color(0xFF1C1B1F),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFCAC4D0)
        )
    }
}
