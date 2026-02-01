package com.example.smartspend.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartspend.data.local.Expense
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.time.LocalDate
import java.time.YearMonth
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.foundation.clickable
import com.example.smartspend.ui.TimePeriod

// Premium Color Palette
private val GradientStart = Color(0xFF667EEA)
private val GradientEnd = Color(0xFF764BA2)
private val CardBackground = Color(0xFF1E1E2E)
private val SurfaceBackground = Color(0xFF121218)
private val AccentGreen = Color(0xFF00D9A5)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFFB0B0C0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    expenses: List<Expense>,
    totalSpending: Double,
    currentDate: LocalDate,
    selectedPeriod: TimePeriod,
    onPeriodSelected: (TimePeriod) -> Unit,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    onAddClick: () -> Unit,
    onDeleteClick: (Expense) -> Unit,
    onExpenseClick: (Long) -> Unit,
    onTotalClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.getDefault()) }

    Scaffold(
        containerColor = SurfaceBackground,
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = AccentGreen,
                contentColor = Color.Black,
                shape = CircleShape,
                modifier = Modifier.size(64.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Expense", modifier = Modifier.size(28.dp))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header with total spending
            item {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Time Period Selector
                PeriodSelector(
                    selectedPeriod = selectedPeriod,
                    onPeriodSelected = onPeriodSelected
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Date Navigator
                if (selectedPeriod != TimePeriod.ALL) {
                    val isNextEnabled = when (selectedPeriod) {
                        TimePeriod.MONTH -> {
                            val nextMonth = currentDate.plusMonths(1)
                            val now = LocalDate.now()
                            !nextMonth.withDayOfMonth(1).isAfter(now.withDayOfMonth(1))
                        }
                        TimePeriod.YEAR -> {
                            val nextYear = currentDate.plusYears(1)
                            val now = LocalDate.now()
                            nextYear.year <= now.year
                        }
                        else -> true
                    }

                    DateNavigator(
                        currentDate = currentDate,
                        period = selectedPeriod,
                        onPrevious = onPreviousPeriod,
                        onNext = onNextPeriod,
                        isNextEnabled = isNextEnabled
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                TotalSpendingCard(
                    total = totalSpending, 
                    currencyFormatter = currencyFormatter,
                    period = selectedPeriod,
                    date = currentDate,
                    onClick = onTotalClick
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Section title
            item {
                Text(
                    text = "Recent Expenses",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextSecondary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Expense Items
            if (expenses.isEmpty()) {
                item {
                    EmptyStateCard()
                }
            } else {
                items(expenses, key = { it.id }) { expense ->
                    ExpenseCard(
                        expense = expense,
                        currencyFormatter = currencyFormatter,
                        onDelete = { onDeleteClick(expense) },
                        onClick = { onExpenseClick(expense.id) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp)) // Space for FAB
            }
        }
    }
}

@Composable
private fun TotalSpendingCard(
    total: Double, 
    currencyFormatter: NumberFormat,
    period: TimePeriod,
    date: LocalDate,
    onClick: () -> Unit
) {
    val periodLabel = when (period) {
        TimePeriod.ALL -> "All Time"
        TimePeriod.MONTH -> date.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
        TimePeriod.YEAR -> date.format(DateTimeFormatter.ofPattern("yyyy"))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(GradientStart, GradientEnd)
                    ),
                    shape = RoundedCornerShape(24.dp)
                )
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = "Total Spending ($periodLabel)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = currencyFormatter.format(total),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
        }
    }
}

@Composable
private fun PeriodSelector(
    selectedPeriod: TimePeriod,
    onPeriodSelected: (TimePeriod) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        TimePeriod.entries.forEach { period ->
            val isSelected = period == selectedPeriod
            val backgroundColor = if (isSelected) AccentGreen else CardBackground
            val textColor = if (isSelected) Color.Black else TextSecondary

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(backgroundColor)
                    .clickable { onPeriodSelected(period) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = period.name.lowercase().capitalize(Locale.ROOT),
                    style = MaterialTheme.typography.labelMedium,
                    color = textColor,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}

@Composable
private fun DateNavigator(
    currentDate: LocalDate,
    period: TimePeriod,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    isNextEnabled: Boolean = true
) {
    val displayText = when (period) {
        TimePeriod.MONTH -> currentDate.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
        TimePeriod.YEAR -> currentDate.format(DateTimeFormatter.ofPattern("yyyy"))
        else -> ""
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Previous",
                tint = TextPrimary
            )
        }
        
        Text(
            text = displayText,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
        
        IconButton(
            onClick = onNext,
            enabled = isNextEnabled
        ) {
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Next",
                tint = if (isNextEnabled) TextPrimary else TextSecondary.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun ExpenseCard(
    expense: Expense,
    currencyFormatter: NumberFormat,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM dd, yyyy") }
    val formattedDate = try {
        LocalDateTime.parse(expense.date).format(dateFormatter)
    } catch (e: Exception) {
        expense.date
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryChip(category = expense.category)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = currencyFormatter.format(expense.amount),
                    style = MaterialTheme.typography.titleMedium,
                    color = AccentGreen,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(category: String) {
    val chipColor = when (category.lowercase()) {
        "food" -> Color(0xFFFF6B6B)
        "transport" -> Color(0xFF4ECDC4)
        "shopping" -> Color(0xFFFFE66D)
        "entertainment" -> Color(0xFFA78BFA)
        "bills" -> Color(0xFFF472B6)
        "investment" -> Color(0xFF22C55E)
        else -> Color(0xFF94A3B8)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(chipColor.copy(alpha = 0.2f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = category,
            style = MaterialTheme.typography.labelSmall,
            color = chipColor,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun EmptyStateCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "📷",
                fontSize = 48.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No expenses yet",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tap the + button to add your first expense",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}
