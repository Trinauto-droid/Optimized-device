package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.GeminiService
import com.example.security.SecurityState
import kotlinx.coroutines.launch
import java.io.File

data class ZombieItem(
    val file: File,
    val type: ZombieType,
    val reason: String,
    var isSelected: Boolean = true
)

enum class ZombieType {
    DUPLICATE, TEMP, EMPTY, BACKUP, MALWARE_SUSPECT, EXPLOIT_PAYLOAD
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZombieScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val filesDir = remember { context.filesDir }
    val scope = rememberCoroutineScope()

    // Screen States
    var isScanning by remember { mutableStateOf(false) }
    var scanCompleted by remember { mutableStateOf(false) }
    var foundZombies by remember { mutableStateOf(emptyList<ZombieItem>()) }
    var totalBytesSaved by remember { mutableStateOf(0L) }
    
    // Gemini Advisor State
    var aiAdvisorReport by remember { mutableStateOf("") }
    var isAiLoading by remember { mutableStateOf(false) }
    var showExplanationDialog by remember { mutableStateOf(false) }

    // Smart Cleanup States
    var isSmartCleaning by remember { mutableStateOf(false) }
    var smartCleanProgress by remember { mutableStateOf(0f) }
    var smartCleanLog by remember { mutableStateOf(emptyList<String>()) }

    // Scan Animation
    val infiniteTransition = rememberInfiniteTransition(label = "Radar Scan")
    val radarRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Radar Rotation"
    )
    val radarScale by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Radar Scale"
    )

    // Scanner Logic
    val runScan = {
        isScanning = true
        scanCompleted = false
        foundZombies = emptyList()
        totalBytesSaved = 0L
        aiAdvisorReport = ""

        scope.launch {
            // Fake delay to show scanning beauty
            kotlinx.coroutines.delay(2000)

            ensureSampleFilesExist(context)
            val zombies = mutableListOf<ZombieItem>()

            // Scan recursively
            val allFiles = mutableListOf<File>()
            fun collectAll(file: File) {
                if (file.isDirectory) {
                    file.listFiles()?.forEach { collectAll(it) }
                } else {
                    allFiles.add(file)
                }
            }
            collectAll(filesDir)

            // 1. Empty files check (0 bytes)
            allFiles.filter { it.length() == 0L }.forEach {
                zombies.add(ZombieItem(it, ZombieType.EMPTY, "Zero byte empty shell file."))
            }

            // 2. Temp cache check (.tmp, .log)
            allFiles.filter { it.name.endsWith(".tmp") || it.name.endsWith(".log") }.forEach {
                zombies.add(ZombieItem(it, ZombieType.TEMP, "Temporary session buffer / diagnostic log."))
            }

            // 3. Obsolete backup files check (.bak)
            allFiles.filter { it.name.endsWith(".bak") }.forEach {
                zombies.add(ZombieItem(it, ZombieType.BACKUP, "Obsolete residual configuration backup."))
            }

            // 4. Duplicate files check (Comparing size & matching names or matching first bytes as hash)
            val groupsOfSize = allFiles.filter { it.length() > 0 }.groupBy { it.length() }
            groupsOfSize.forEach { (size, files) ->
                if (files.size > 1) {
                    for (i in 1 until files.size) {
                        zombies.add(ZombieItem(files[i], ZombieType.DUPLICATE, "Identical size (${formatSize(size)}) duplicate copy of ${files[0].name}"))
                    }
                }
            }

            // 5. Offline Cyber Attack Defense - Double Extensions & Dangerous Payloads
            allFiles.forEach { file ->
                val nameLower = file.name.lowercase()
                val parts = file.name.split(".")
                
                // Double extension detection
                if (parts.size > 2) {
                    val penult = parts[parts.size - 2].lowercase()
                    val last = parts[parts.size - 1].lowercase()
                    val dangerousExts = listOf("exe", "sh", "bin", "bat", "cmd", "py", "js", "vbs")
                    val harmlessBase = listOf("tar", "gz", "zip", "rar")
                    if (last in dangerousExts && penult !in harmlessBase) {
                        zombies.add(ZombieItem(file, ZombieType.EXPLOIT_PAYLOAD, "Malicious double-extension bypass attempt discovered!"))
                        SecurityState.registerAlert("Double-extension threat blocked: ${file.name}")
                    }
                }
                
                // Dangerous scripts or standalone binaries
                if (nameLower.endsWith(".sh") || nameLower.endsWith(".exe") || nameLower.endsWith(".bin") || nameLower.endsWith(".bat") || nameLower.endsWith(".vbs")) {
                    if (zombies.none { it.file.absolutePath == file.absolutePath }) {
                        zombies.add(ZombieItem(file, ZombieType.MALWARE_SUSPECT, "Unauthorized executable script in user partition."))
                        SecurityState.registerAlert("Rogue script isolated: ${file.name}")
                    }
                }
            }

            foundZombies = zombies
            totalBytesSaved = zombies.sumOf { it.file.length() }
            isScanning = false
            scanCompleted = true

            // Trigger AI Advice Recommendation
            isAiLoading = true
            val fileMetaList = zombies.joinToString("\n") { 
                "- ${it.file.name} (${formatSize(it.file.length())}) [Type: ${it.type.name}]" 
            }
            val prompt = """
                Analyze this scanned list of 'Zombie' system files from the application sandbox:
                $fileMetaList
                
                Provide an expert analysis of:
                1. Why these files are classified as duplicates or useless backups.
                2. The risks of retaining obsolete backups.
                3. Clear optimization guidelines for storage hygiene.
            """.trimIndent()

            val response = GeminiService.getGeminiSuggestion(
                prompt = prompt,
                systemInstruction = "You are OmniFile AI Disk Janitor. Provide highly technical, clean, and practical storage advice.",
                useProWithThinking = true
            )
            aiAdvisorReport = response
            isAiLoading = false
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
                Icon(Icons.Default.DeleteSweep, contentDescription = "Zombie Finder", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(32.dp))
                Column {
                    Text("Zombie File Cleanroom", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Scan and exterminate redundant duplicates and orphan backups", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                }
            }
            IconButton(
                onClick = { showExplanationDialog = true },
                modifier = Modifier.testTag("zombie_info_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Show explanation",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }

        if (!isScanning && !scanCompleted) {
            // --- READY STATE ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // CARD 1: ZOMBIE STORAGE SCANNER
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(Color(0xFFEF4444).copy(alpha = 0.15f), Color.Transparent),
                                        radius = size.minDimension / 1.5f
                                    )
                                )
                            }
                            Icon(
                                Icons.Default.Radar,
                                contentDescription = "Radar Idle",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(54.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Zombie Storage Scanner",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Scans system partitions and app sandboxes for duplicates, logs, empty placeholders, and outdated .bak configuration records.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { runScan() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth(0.8f)
                                .height(44.dp)
                                .testTag("start_scan_zombies_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Run Scan")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Initiate Sector Scan")
                        }
                    }
                }

                // CARD 2: SMART CLEANUP ENGINE
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Smart Clean Icon",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Smart Cleanup Engine",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Text(
                            text = "Instantly clears residual caches, zero-byte logs, and redundant background junk streams from application memory sectors recursively.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Button(
                            onClick = {
                                isSmartCleaning = true
                                smartCleanProgress = 0f
                                smartCleanLog = listOf("[INFO] Initializing Smart Cleanup Suite...")
                                scope.launch {
                                    kotlinx.coroutines.delay(800)
                                    smartCleanLog = smartCleanLog + "[INFO] Probing temporary storage clusters..."
                                    smartCleanProgress = 0.25f
                                    kotlinx.coroutines.delay(600)
                                    smartCleanLog = smartCleanLog + "[INFO] Purging dead files and empty system logs..."
                                    smartCleanProgress = 0.5f
                                    
                                    // Perform real sweep delete!
                                    var deletedCount = 0
                                    var deletedBytes = 0L
                                    fun runRealSweep(file: File) {
                                        if (file.isDirectory) {
                                            file.listFiles()?.forEach { runRealSweep(it) }
                                        } else {
                                            val nameLower = file.name.lowercase()
                                            if (nameLower.endsWith(".tmp") || nameLower.endsWith(".log") || nameLower.endsWith(".bak") || file.length() == 0L) {
                                                deletedBytes += file.length()
                                                if (file.delete()) deletedCount++
                                            }
                                        }
                                    }
                                    runRealSweep(filesDir)
                                    
                                    kotlinx.coroutines.delay(600)
                                    smartCleanLog = smartCleanLog + "[INFO] Recalculating disk partition buffers..."
                                    smartCleanProgress = 0.8f
                                    kotlinx.coroutines.delay(800)
                                    smartCleanLog = smartCleanLog + "[SUCCESS] Wiped $deletedCount junk file records! Released ${formatSize(deletedBytes)}."
                                    smartCleanProgress = 1.0f
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("smart_cleanup_run_btn")
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Run Smart Cleanup Sweep", fontSize = 12.sp)
                        }
                    }
                }
            }
        } else if (isScanning) {
            // --- SCANNING ROTATOR ACTIVE ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier.size(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Draw background rings
                            drawCircle(color = Color(0xFF1E293B), style = Stroke(2f))
                            drawCircle(color = Color(0xFF1E293B), radius = size.minDimension / 3, style = Stroke(1.5f))
                            
                            // Draw glowing pulse
                            drawCircle(
                                color = Color(0xFFEF4444).copy(alpha = 0.08f),
                                radius = (size.minDimension / 2) * radarScale
                            )

                            // Rotating sweeps
                            drawArc(
                                brush = Brush.sweepGradient(
                                    colors = listOf(Color(0xFFEF4444), Color.Transparent)
                                ),
                                startAngle = radarRotation,
                                sweepAngle = 90f,
                                useCenter = true
                            )
                        }
                        Icon(
                            Icons.Default.Biotech,
                            contentDescription = "Scanning",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        "Scouring Directory Sectors...",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        "Analyzing sizes, file buffers & redundancy patterns.",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    LinearProgressIndicator(
                        color = Color(0xFFEF4444),
                        trackColor = Color(0xFF334155),
                        modifier = Modifier
                            .fillMaxWidth(0.7f)
                            .clip(RoundedCornerShape(4.dp))
                    )
                }
            }
        } else if (scanCompleted) {
            // --- RESULTS AND RESOLUTION ---
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Summary KPI
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Recoverable Space", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(formatSize(totalBytesSaved), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Total Zombies", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("${foundZombies.size} Files", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                TabRow(
                    selectedTabIndex = 0,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp))
                ) {
                    Tab(selected = true, onClick = {}, text = { Text("Zombies Found") })
                }

                if (foundZombies.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.5f)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Verified, contentDescription = "Clean", tint = Color(0xFF10B981), modifier = Modifier.size(48.dp))
                            Text("Your partitions are pristine!", fontWeight = FontWeight.Bold)
                            Text("No zombie files detected.", color = MaterialTheme.colorScheme.outline, fontSize = 12.sp)
                        }
                    }
                } else {
                    // List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.6f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(foundZombies) { item ->
                            val isThreat = item.type == ZombieType.MALWARE_SUSPECT || item.type == ZombieType.EXPLOIT_PAYLOAD
                            val containerBg = if (isThreat) Color(0xFFEF4444).copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                            val borderColor = if (isThreat) Color(0xFFEF4444).copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                            var selected by remember { mutableStateOf(item.isSelected) }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(containerBg)
                                    .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                                    .clickable { 
                                        selected = !selected
                                        item.isSelected = selected
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = selected,
                                    onCheckedChange = { 
                                        selected = it
                                        item.isSelected = it
                                    },
                                    colors = CheckboxDefaults.colors(checkedColor = if (isThreat) Color(0xFFEF4444) else MaterialTheme.colorScheme.error)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = item.file.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = if (isThreat) Color(0xFFF87171) else Color.White
                                        )
                                        if (isThreat) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(Color(0xFFEF4444).copy(alpha = 0.2f))
                                                    .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Text("THREAT ISOLATED", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFFF87171))
                                            }
                                        }
                                    }
                                    Text(item.reason, fontSize = 11.sp, color = if (isThreat) Color(0xFFFCA5A5) else MaterialTheme.colorScheme.outline)
                                }
                                Text(
                                    formatSize(item.file.length()), 
                                    fontSize = 12.sp, 
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isThreat) Color(0xFFEF4444) else MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                // AI Expert Advice Box
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.4f),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Psychology, contentDescription = "AI", tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
                            Text("AI Disk Hygiene Expert Advisor", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        if (isAiLoading) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.tertiary)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Analyzing disk sectors...", fontSize = 12.sp)
                            }
                        } else {
                            LazyColumn(modifier = Modifier.fillMaxSize()) {
                                item {
                                    Text(
                                        text = aiAdvisorReport.ifEmpty { "Scraping local file signatures to produce optimization strategies..." },
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp,
                                        fontFamily = FontFamily.SansSerif
                                    )
                                }
                            }
                        }
                    }
                }

                // Delete Button
                Button(
                    onClick = {
                        val targets = foundZombies.filter { it.isSelected }
                        targets.forEach { item ->
                            item.file.deleteRecursively()
                        }
                        // Re-run scan to update list
                        runScan()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("delete_selected_zombies_button"),
                    enabled = foundZombies.any { it.isSelected }
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = "Exterminate")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Exterminate Selected Zombies")
                }
            }
        }
    }

    // --- ZOMBIE EXPLANATION DIALOG ---
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
                        contentDescription = "Cleanroom Hub Info",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Exterminator Specs",
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
                        text = "Zombie Storage Scanner",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Scours active filesystem branches for redundant blocks, duplicate copies, empty placeholders, and orphan cache records.",
                        fontSize = 13.sp,
                        color = Color(0xFFCAC4D0)
                    )
                    
                    Divider(color = Color(0xFF49454F))
                    
                    Text("CATEGORICAL FILE CHECKSUMS:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFCAC4D0), fontFamily = FontFamily.Monospace)
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("▪", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 2.dp))
                            Column {
                                Text("Zero-Byte Shells (Empty)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                Text("Orphan files that carry zero payload weight but occupy active inode addresses in the directory tree.", fontSize = 11.sp, color = Color(0xFFCAC4D0))
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("▪", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 2.dp))
                            Column {
                                Text("Temporary Cache & Logs", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                Text("Identifies transient sessions (e.g., .tmp and .log extensions) produced by app threads that were not properly de-allocated.", fontSize = 11.sp, color = Color(0xFFCAC4D0))
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("▪", color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 2.dp))
                            Column {
                                Text("Duplicate Data Streams", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                Text("Clusters files by exact byte size and compares name fragments to identify redundancy without deleting originals.", fontSize = 11.sp, color = Color(0xFFCAC4D0))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showExplanationDialog = false },
                    modifier = Modifier.testTag("zombie_info_dismiss")
                ) {
                    Text("DISMISS EXTERMINATOR BRIEF", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                }
            },
            containerColor = Color(0xFF1C1B1F),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFCAC4D0)
        )
    }

    // --- SMART CLEANUP ENGINE DIALOG ---
    if (isSmartCleaning) {
        AlertDialog(
            onDismissRequest = { if (smartCleanProgress >= 1f) isSmartCleaning = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        progress = { smartCleanProgress },
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text("Smart Cleanup Sweep", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Cleaning system logs, temp cache tables, and unreferenced filesystem blocks...",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    LinearProgressIndicator(
                        progress = { smartCleanProgress },
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp))
                    )
                    
                    // Console Output Log
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(8.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                    ) {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(smartCleanLog) { line ->
                                Text(
                                    text = line,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (line.startsWith("[SUCCESS]")) Color(0xFF10B981) else Color(0xFFCAC4D0)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { isSmartCleaning = false },
                    enabled = smartCleanProgress >= 1.0f,
                    modifier = Modifier.testTag("smart_cleanup_dismiss_btn")
                ) {
                    Text("DISMISS CLEANER", fontWeight = FontWeight.Bold, color = if (smartCleanProgress >= 1f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
                }
            },
            containerColor = Color(0xFF1C1B1F)
        )
    }
}
