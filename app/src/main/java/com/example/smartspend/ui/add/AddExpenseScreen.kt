package com.example.smartspend.ui.add

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.DateRange
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import android.net.Uri
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartspend.data.ai.AiTier
import com.example.smartspend.data.local.Category

// Premium Color Palette (same as HomeScreen)
private val GradientStart = Color(0xFF667EEA)
private val GradientEnd = Color(0xFF764BA2)
private val CardBackground = Color(0xFF1E1E2E)
private val SurfaceBackground = Color(0xFF121218)
private val AccentGreen = Color(0xFF00D9A5)
private val AccentGold = Color(0xFFFFD700)
private val AccentPurple = Color(0xFFA78BFA)
private val AccentBlue = Color(0xFF60A5FA)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFFB0B0C0)

// val categories = listOf("Food", "Transport", "Shopping", "Entertainment", "Bills", "Investment", "Other") // Replaced by Category Enum

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit,
    onSaveExpense: (title: String, amount: Double, category: String, notes: String?) -> Unit,
    onScanReceipt: (AiTier) -> Unit,
    isScanning: Boolean = false,
    scannedTitle: String? = null,
    scannedAmount: Double? = null,
    scannedCategory: String? = null,
    scannedNote: String? = null,
    currentAiTier: AiTier = AiTier.BASIC,
    unlockedTiers: Set<AiTier> = setOf(AiTier.BASIC), // Tiers user has purchased
    onTierSelected: (AiTier) -> Unit = {},
    onScanGallery: (Uri) -> Unit = {}
) {
    // Gallery Launcher
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onScanGallery(uri)
        }
    }
    var title by remember(scannedTitle) { mutableStateOf(scannedTitle ?: "") }
    var amount by remember(scannedAmount) { mutableStateOf(scannedAmount?.toString() ?: "") }
    var selectedCategory by remember { mutableStateOf(Category.FOOD) }
    
    // Update category when AI scan returns a valid category
    LaunchedEffect(scannedCategory) {
        if (!scannedCategory.isNullOrBlank()) {
            selectedCategory = Category.fromString(scannedCategory)
        }
    }

    var notes by remember(scannedNote) { mutableStateOf(scannedNote ?: "") }
    var showCategoryDropdown by remember { mutableStateOf(false) }
    var selectedTier by remember { mutableStateOf(currentAiTier) }

    Scaffold(
        containerColor = SurfaceBackground,
        topBar = {
            TopAppBar(
                title = { Text("Add Expense", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceBackground
                )
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            // AI Tier Selector
            AiTierSelector(
                selectedTier = selectedTier,
                unlockedTiers = unlockedTiers,
                onTierSelected = { tier ->
                    selectedTier = tier
                    onTierSelected(tier)
                }
            )

            // Scan Receipt Button - The STAR feature!
            ScanReceiptButton(
                onClick = { onScanReceipt(selectedTier) },
                isScanning = isScanning,
                selectedTier = selectedTier
            )
            
            // Gallery Button (Small, under Scan)
            TextButton(
                onClick = { 
                    galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp),
                enabled = !isScanning,
                colors = ButtonDefaults.textButtonColors(contentColor = AccentGreen)
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Or pick from Gallery",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Divider with "OR"
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = TextSecondary.copy(alpha = 0.3f))
                Text(
                    text = "  OR ENTER MANUALLY  ",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = TextSecondary.copy(alpha = 0.3f))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Title Input
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Expense Title") },
                placeholder = { Text("e.g., Coffee at Starbucks") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentGreen,
                    unfocusedBorderColor = TextSecondary.copy(alpha = 0.5f),
                    focusedLabelColor = AccentGreen,
                    unfocusedLabelColor = TextSecondary,
                    cursorColor = AccentGreen,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedPlaceholderColor = TextSecondary,
                    unfocusedPlaceholderColor = TextSecondary
                ),
                singleLine = true
            )

            // Amount Input
            OutlinedTextField(
                value = amount,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                        amount = newValue
                    }
                },
                label = { Text("Amount ($)") },
                placeholder = { Text("0.00") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentGreen,
                    unfocusedBorderColor = TextSecondary.copy(alpha = 0.5f),
                    focusedLabelColor = AccentGreen,
                    unfocusedLabelColor = TextSecondary,
                    cursorColor = AccentGreen,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedPlaceholderColor = TextSecondary,
                    unfocusedPlaceholderColor = TextSecondary
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )

            // Category Dropdown
            ExposedDropdownMenuBox(
                expanded = showCategoryDropdown,
                onExpandedChange = { showCategoryDropdown = it }
            ) {
                OutlinedTextField(
                    value = "${selectedCategory.icon} ${selectedCategory.displayName}",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showCategoryDropdown) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentGreen,
                        unfocusedBorderColor = TextSecondary.copy(alpha = 0.5f),
                        focusedLabelColor = AccentGreen,
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
                ExposedDropdownMenu(
                    expanded = showCategoryDropdown,
                    onDismissRequest = { showCategoryDropdown = false },
                    modifier = Modifier.background(CardBackground)
                ) {
                    Category.entries.forEach { category ->
                        DropdownMenuItem(
                            text = { 
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(category.color)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("${category.icon} ${category.displayName}", color = TextPrimary)
                                }
                            },
                            onClick = {
                                selectedCategory = category
                                showCategoryDropdown = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }

            // Notes Input (Optional)
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (Optional)") },
                placeholder = { Text("Add any extra details...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentGreen,
                    unfocusedBorderColor = TextSecondary.copy(alpha = 0.5f),
                    focusedLabelColor = AccentGreen,
                    unfocusedLabelColor = TextSecondary,
                    cursorColor = AccentGreen,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    focusedPlaceholderColor = TextSecondary,
                    unfocusedPlaceholderColor = TextSecondary
                ),
                maxLines = 4
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Save Button
            Button(
                onClick = {
                    val parsedAmount = amount.toDoubleOrNull() ?: 0.0
                    if (title.isNotBlank() && parsedAmount > 0) {
                    if (title.isNotBlank()) {
                        onSaveExpense(title, parsedAmount, selectedCategory.displayName, notes.ifBlank { null })
                    }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentGreen,
                    contentColor = Color.Black
                ),
                enabled = title.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text(
                    text = "Save Expense",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * AI Tier Selector - Horizontal scrolling cards for tier selection
 */
@Composable
private fun AiTierSelector(
    selectedTier: AiTier,
    unlockedTiers: Set<AiTier>,
    onTierSelected: (AiTier) -> Unit
) {
    val listState = rememberLazyListState()

    // Auto-scroll to selected tier when it changes
    LaunchedEffect(selectedTier) {
        val index = AiTier.entries.indexOf(selectedTier)
        if (index >= 0) {
            listState.animateScrollToItem(index)
        }
    }

    Column {
        Text(
            text = "AI Model",
            style = MaterialTheme.typography.titleSmall,
            color = TextSecondary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        LazyRow(
            state = listState,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(AiTier.entries) { tier ->
                AiTierCard(
                    tier = tier,
                    isSelected = tier == selectedTier,
                    isUnlocked = tier in unlockedTiers,
                    onClick = {
                        if (tier in unlockedTiers) {
                            onTierSelected(tier)
                        }
                    }
                )
            }
        }
    }
}

/**
 * Individual AI Tier Card
 */
@Composable
private fun AiTierCard(
    tier: AiTier,
    isSelected: Boolean,
    isUnlocked: Boolean,
    onClick: () -> Unit
) {
    val tierColor = when (tier) {
        AiTier.BASIC -> AccentGreen
        AiTier.STANDARD -> AccentBlue
        AiTier.ADVANCED -> AccentPurple
        AiTier.ELITE -> AccentGold
    }
    
    val tierIcon = when (tier) {
        AiTier.BASIC -> "⚡"
        AiTier.STANDARD -> "🚀"
        AiTier.ADVANCED -> "🧠"
        AiTier.ELITE -> "✨"
    }
    
    val animatedBorderColor by animateColorAsState(
        targetValue = if (isSelected) tierColor else Color.Transparent,
        label = "borderColor"
    )
    
    val animatedBackgroundColor by animateColorAsState(
        targetValue = if (isSelected) tierColor.copy(alpha = 0.15f) else CardBackground,
        label = "backgroundColor"
    )
    
    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable(enabled = isUnlocked) { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = animatedBackgroundColor),
        border = BorderStroke(2.dp, animatedBorderColor)
    ) {
        Box {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Tier Icon
                Text(
                    text = tierIcon,
                    fontSize = 24.sp
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Tier Name
                Text(
                    text = tier.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isUnlocked) tierColor else TextSecondary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // RPD Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(tierColor.copy(alpha = if (isUnlocked) 0.2f else 0.1f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${tier.requestsPerDay}/day",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isUnlocked) tierColor else TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Description (truncated)
                Text(
                    text = tier.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary.copy(alpha = if (isUnlocked) 1f else 0.5f),
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    lineHeight = 14.sp
                )
            }
            
            // Lock overlay for locked tiers
            if (!isUnlocked) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(SurfaceBackground.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = TextSecondary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            // Checkmark for selected tier
            if (isSelected && isUnlocked) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(20.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(tierColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.Black,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ScanReceiptButton(
    onClick: () -> Unit,
    isScanning: Boolean,
    selectedTier: AiTier
) {
    val tierColor = when (selectedTier) {
        AiTier.BASIC -> AccentGreen
        AiTier.STANDARD -> AccentBlue
        AiTier.ADVANCED -> AccentPurple
        AiTier.ELITE -> AccentGold
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isScanning) { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(GradientStart, GradientEnd)
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isScanning) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = TextPrimary,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Scanning Receipt...",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Using ${selectedTier.displayName}",
                            color = TextPrimary.copy(alpha = 0.8f),
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "📷 Scan Receipt",
                            color = TextPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Using ",
                                color = TextPrimary.copy(alpha = 0.8f),
                                fontSize = 12.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(tierColor.copy(alpha = 0.3f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = selectedTier.displayName,
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
