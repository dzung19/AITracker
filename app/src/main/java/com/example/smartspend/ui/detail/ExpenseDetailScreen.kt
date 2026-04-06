package com.example.smartspend.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartspend.data.local.Expense
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.example.smartspend.data.local.Category

// Reuse colors from HomeScreen for consistency

private val SurfaceBackground = Color(0xFF121218)
private val CardBackground = Color(0xFF1E1E2E)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFFB0B0C0)
private val AccentGreen = Color(0xFF00D9A5)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDetailScreen(
    expense: Expense?,
    currencyFormatter: NumberFormat,
    onNavigateBack: () -> Unit
) {
    Scaffold(
        containerColor = SurfaceBackground,
        topBar = {
            TopAppBar(
                title = { Text("Expense Details", color = TextPrimary) },
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
        if (expense == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentGreen)
            }
        } else {
            ExpenseContent(expense = expense, currencyFormatter = currencyFormatter, modifier = Modifier.padding(padding))
        }
    }
}

@Composable
private fun ExpenseContent(expense: Expense, currencyFormatter: NumberFormat, modifier: Modifier = Modifier) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMMM dd, yyyy • h:mm a") }
    val formattedDate = try {
        LocalDateTime.parse(expense.date).format(dateFormatter)
    } catch (e: Exception) {
        expense.date
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Amount and Title Header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = expense.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = currencyFormatter.format(expense.amount),
                        style = MaterialTheme.typography.displayMedium,
                        color = AccentGreen,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    CategoryChip(category = expense.category)
                }
            }
        }

        // Details Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardBackground)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    // Date
                    DetailRow(
                        icon = Icons.Default.DateRange,
                        label = "Date",
                        value = formattedDate
                    )
                    
                    if (!expense.notes.isNullOrBlank()) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 16.dp),
                            color = TextSecondary.copy(alpha = 0.2f)
                        )
                        // Notes
                        DetailRow(
                            icon = Icons.Default.Info,
                            label = "Notes",
                            value = expense.notes
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = TextPrimary,
                lineHeight = 24.sp
            )
        }
    }
}

@Composable
private fun CategoryChip(category: String) {
    val chipColor = com.example.smartspend.data.local.Category.fromString(category).color

    Surface(
        color = chipColor.copy(alpha = 0.2f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = category,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
            color = chipColor,
            fontWeight = FontWeight.Bold
        )
    }
}


