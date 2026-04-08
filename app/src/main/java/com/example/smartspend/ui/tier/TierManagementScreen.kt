package com.example.smartspend.ui.tier

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartspend.data.ai.AiTier

// Premium Color Palette (Consistent with App)
private val SurfaceBackground = Color(0xFF121218)
private val CardBackground = Color(0xFF1E1E2E)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFFB0B0C0)
private val AccentGreen = Color(0xFF00D9A5)
private val AccentBlue = Color(0xFF60A5FA)
private val AccentPurple = Color(0xFFA78BFA)
private val AccentGold = Color(0xFFFFD700)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TierManagementScreen(
    currentTier: AiTier,
    unlockedTiers: Set<AiTier>,
    onTierSelected: (AiTier) -> Unit,
    onPurchaseClick: (AiTier) -> Unit,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        containerColor = SurfaceBackground,
        topBar = {
            TopAppBar(
                title = { Text("AI Management", color = TextPrimary) },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Unlock the power of AI",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(AiTier.entries) { tier ->
                DetailedAiTierCard(
                    tier = tier,
                    isSelected = tier == currentTier,
                    isUnlocked = tier in unlockedTiers,
                    onSelect = { onTierSelected(tier) },
                    onPurchaseClick = { onPurchaseClick(tier) }
                )
            }
            

            
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun DetailedAiTierCard(
    tier: AiTier,
    isSelected: Boolean,
    isUnlocked: Boolean,
    onSelect: () -> Unit,
    onPurchaseClick: () -> Unit
) {
    val tierColor = when (tier) {
        AiTier.BASIC -> AccentGreen
        AiTier.STANDARD -> AccentBlue
        AiTier.ADVANCED -> AccentPurple
        AiTier.ELITE -> AccentGold
    }

    val animatedBorderColor by animateColorAsState(
        targetValue = if (isSelected) tierColor else Color.Transparent,
        label = "borderColor"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { 
                if (isUnlocked) onSelect() else onPurchaseClick()
            },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        border = BorderStroke(2.dp, animatedBorderColor)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val icon = when (tier) {
                        AiTier.BASIC -> "⚡"
                        AiTier.STANDARD -> "🚀"
                        AiTier.ADVANCED -> "🧠"
                        AiTier.ELITE -> "✨"
                    }
                    Text(text = icon, fontSize = 32.sp)
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = tier.displayName,
                            style = MaterialTheme.typography.titleLarge,
                            color = if (isUnlocked) tierColor else TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = tier.modelName,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
                
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = tierColor,
                        modifier = Modifier.size(32.dp)
                    )
                } else if (!isUnlocked) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = TextSecondary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = tier.description,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary.copy(alpha = 0.9f),
                lineHeight = 20.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Badge(text = "${tier.requestsPerDay} scans/day", color = tierColor, isUnlocked = isUnlocked)
                Badge(text = "${tier.requestsPerMinute} RPM", color = tierColor, isUnlocked = isUnlocked)
            }
        }
    }
}



@Composable
fun Badge(text: String, color: Color, isUnlocked: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = if (isUnlocked) 0.15f else 0.05f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = if (isUnlocked) color else TextSecondary,
            fontWeight = FontWeight.Bold
        )
    }
}
