package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.security.SecurityState

enum class AppTab {
    EXPLORER, ZOMBIE, OPTIMIZER, SD_FORMAT, UPGRADE
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                AppMainLayout()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppMainLayout() {
    var activeTab by rememberSaveable { mutableStateOf(AppTab.EXPLORER) }
    var showThemeCustomizer by rememberSaveable { mutableStateOf(false) }
    var showInfoDialog by rememberSaveable { mutableStateOf(false) }
    var showSecurityShieldDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = "SystemCore X",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            text = "SystemCore X",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                actions = {
                    // Pulsing/Colored Airgap Shield Status Button
                    IconButton(
                        onClick = { showSecurityShieldDialog = true },
                        modifier = Modifier.testTag("airgap_shield_toggle_btn")
                    ) {
                        Icon(
                            imageVector = if (SecurityState.isAirgapShieldEnabled.value) Icons.Default.VerifiedUser else Icons.Default.Shield,
                            contentDescription = "Airgap Defense Hub",
                            tint = if (SecurityState.isAirgapShieldEnabled.value) Color(0xFF10B981) else Color(0xFFF59E0B)
                        )
                    }
                    IconButton(
                        onClick = { showThemeCustomizer = true },
                        modifier = Modifier.testTag("palette_theme_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = "Theme Customizer",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = { showInfoDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "System Info",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        bottomBar = {
            Column {
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), thickness = 1.dp)
                NavigationBar(
                    tonalElevation = 8.dp,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    val navItemColors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.onBackground,
                        unselectedIconColor = MaterialTheme.colorScheme.outline,
                        unselectedTextColor = MaterialTheme.colorScheme.outline,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    )

                    // Explorer Tab
                    NavigationBarItem(
                        selected = activeTab == AppTab.EXPLORER,
                        onClick = { activeTab = AppTab.EXPLORER },
                        colors = navItemColors,
                        icon = {
                            Icon(
                                imageVector = if (activeTab == AppTab.EXPLORER) Icons.Default.Folder else Icons.Outlined.Folder,
                                contentDescription = "Explorer"
                            )
                        },
                        label = { Text("Explorer", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("nav_explorer_tab")
                    )

                    // Zombie Finder Tab
                    NavigationBarItem(
                        selected = activeTab == AppTab.ZOMBIE,
                        onClick = { activeTab = AppTab.ZOMBIE },
                        colors = navItemColors,
                        icon = {
                            Icon(
                                imageVector = if (activeTab == AppTab.ZOMBIE) Icons.Default.DeleteSweep else Icons.Outlined.DeleteSweep,
                                contentDescription = "Zombie Cleanroom"
                            )
                        },
                        label = { Text("Cleanroom", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("nav_zombie_tab")
                    )

                    // Optimizer Tab
                    NavigationBarItem(
                        selected = activeTab == AppTab.OPTIMIZER,
                        onClick = { activeTab = AppTab.OPTIMIZER },
                        colors = navItemColors,
                        icon = {
                            Icon(
                                imageVector = if (activeTab == AppTab.OPTIMIZER) Icons.Default.Bolt else Icons.Outlined.Bolt,
                                contentDescription = "Optimizer"
                            )
                        },
                        label = { Text("Optimizer", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("nav_optimizer_tab")
                    )

                    // SD Card Tab
                    NavigationBarItem(
                        selected = activeTab == AppTab.SD_FORMAT,
                        onClick = { activeTab = AppTab.SD_FORMAT },
                        colors = navItemColors,
                        icon = {
                            Icon(
                                imageVector = if (activeTab == AppTab.SD_FORMAT) Icons.Default.SdCard else Icons.Outlined.SdCard,
                                contentDescription = "Format"
                            )
                        },
                        label = { Text("Format", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("nav_sd_format_tab")
                    )

                    // System Upgrade Tab
                    NavigationBarItem(
                        selected = activeTab == AppTab.UPGRADE,
                        onClick = { activeTab = AppTab.UPGRADE },
                        colors = navItemColors,
                        icon = {
                            Icon(
                                imageVector = if (activeTab == AppTab.UPGRADE) Icons.Default.SystemUpdate else Icons.Outlined.SystemUpdate,
                                contentDescription = "Upgrade Engine"
                            )
                        },
                        label = { Text("Upgrade", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        modifier = Modifier.testTag("nav_upgrade_tab")
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = MaterialTheme.colorScheme.background
        ) {
            AnimatedContent(
                targetState = activeTab,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
                },
                label = "ScreenTransitions"
            ) { targetTab ->
                when (targetTab) {
                    AppTab.EXPLORER -> ExplorerScreen()
                    AppTab.ZOMBIE -> ZombieScreen()
                    AppTab.OPTIMIZER -> OptimizerScreen()
                    AppTab.SD_FORMAT -> SDCardScreen()
                    AppTab.UPGRADE -> SoftwareUpdateScreen()
                }
            }
        }
    }

    // --- THEME CUSTOMIZER DIALOG ---
    if (showThemeCustomizer) {
        AlertDialog(
            onDismissRequest = { showThemeCustomizer = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = "Theme Customizer",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text("Theme Customizer", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Select system theme skin preset to dynamically adjust virtual machine hardware console styling.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AppThemePreset.values().forEach { preset ->
                            val isSelected = ThemeSelector.currentTheme.value == preset
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                        else Color.Transparent
                                    )
                                    .border(
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary 
                                                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        ThemeSelector.currentTheme.value = preset
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Mini Color Circle
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .background(preset.primary, CircleShape)
                                        .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                                )
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = preset.displayName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White
                                    )
                                }
                                
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { ThemeSelector.currentTheme.value = preset },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { showThemeCustomizer = false }
                ) {
                    Text("APPLY AESTHETICS", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = Color(0xFF1C1B1F),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFCAC4D0)
        )
    }

    // --- SYSTEM INFO DIALOG ---
    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text("SystemCore X Specifications", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "An advanced virtual sandbox filesystem workspace, garbage sweep optimizer, block-level SD Card wiper, and custom Treble OS flashing suite.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Divider()
                    Text("App Version: v2.5.1-PRO", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Text("Runtime VM: Android ART (AOT Mode)", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Text("Compiled with: Compose & Kotlin 2.x", fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("DISMISS BRIEF", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = Color(0xFF1C1B1F),
            titleContentColor = Color.White,
            textContentColor = Color(0xFFCAC4D0)
        )
    }

    // --- AIRGAP SECURITY & TAMPER ALERT CONTROL HUB ---
    if (showSecurityShieldDialog) {
        AlertDialog(
            onDismissRequest = { showSecurityShieldDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (SecurityState.isAirgapShieldEnabled.value) Icons.Default.VerifiedUser else Icons.Default.Shield,
                        contentDescription = "Security Shield",
                        tint = if (SecurityState.isAirgapShieldEnabled.value) Color(0xFF10B981) else Color(0xFFF59E0B),
                        modifier = Modifier.size(26.dp)
                    )
                    Text("Airgap Security & Firewall", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                ) {
                    Text(
                        "This tool operates 100% offline. Local sandbox storage and flash interfaces are protected from remote cyber attacks.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))

                    // Airgap Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Airgap Firewall",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                "Completely isolates virtual sandbox filesystem from external internet queries.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = SecurityState.isAirgapShieldEnabled.value,
                            onCheckedChange = { isChecked ->
                                SecurityState.isAirgapShieldEnabled.value = isChecked
                                if (isChecked) {
                                    SecurityState.registerAlert("Airgap Firewall Enabled. Outbound networking restricted.")
                                } else {
                                    SecurityState.registerAlert("Airgap Firewall Disabled. Running in Dynamic Defense mode.")
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF10B981),
                                checkedTrackColor = Color(0xFF10B981).copy(alpha = 0.3f)
                            )
                        )
                    }

                    // Tamper Protection Toggle
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Tamper Detection",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                "Instantly alerts and blocks rogue execution attempts or suspicious extension edits.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = SecurityState.isTamperProtectionActive.value,
                            onCheckedChange = { isChecked ->
                                SecurityState.isTamperProtectionActive.value = isChecked
                                if (isChecked) {
                                    SecurityState.registerAlert("Active Workspace Tamper Guard Enabled.")
                                } else {
                                    SecurityState.registerAlert("Active Workspace Tamper Guard Disabled.")
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF10B981),
                                checkedTrackColor = Color(0xFF10B981).copy(alpha = 0.3f)
                            )
                        )
                    }

                    // Blocked Counter Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Cyber Intrusion Shield",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFEF4444).copy(alpha = 0.2f))
                                    .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${SecurityState.totalThreatsBlocked.value} BLOCKED",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF87171)
                                )
                            }
                        }
                    }

                    // Threat Alerts List
                    Text("Recent Firewall Block logs", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(6.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A))
                    ) {
                        if (SecurityState.recentThreatAlerts.value.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No threat intrusions flagged.", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(SecurityState.recentThreatAlerts.value) { alert ->
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(modifier = Modifier.size(4.dp).background(Color(0xFFEF4444), CircleShape))
                                        Text(
                                            text = alert,
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = Color(0xFFF87171)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSecurityShieldDialog = false }) {
                    Text("SAVE & AIRGAP", fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                }
            },
            containerColor = Color(0xFF1C1B1F)
        )
    }
}

