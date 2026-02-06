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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.DateRange
import kotlinx.coroutines.launch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.animation.core.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartspend.data.local.Expense
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.time.LocalDate
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.example.smartspend.data.local.Category.Companion.fromString
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
    installDate: LocalDate,
    selectedPeriod: TimePeriod,
    monthlyBudget: Double?,
    streakCount: Int,
    showStreakCelebration: Boolean,
    onDismissStreak: () -> Unit,
    onSetBudget: (Double?) -> Unit,
    onPeriodSelected: (TimePeriod) -> Unit,
    onPreviousPeriod: () -> Unit,
    onNextPeriod: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onAddClick: () -> Unit,
    onDeleteClick: (Expense) -> Unit,
    onExpenseClick: (Long) -> Unit,
    onTotalClick: () -> Unit,
    onTierManagementClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.getDefault()) }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    // Budget dialog state
    var showBudgetDialog by remember { mutableStateOf(false) }
    
    // Date picker dialog state
    var showDatePicker by remember { mutableStateOf(false) }
    
    // Streak celebration dialog
    if (showStreakCelebration) {
        StreakCelebrationDialog(
            streakCount = streakCount,
            onDismiss = onDismissStreak
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = CardBackground,
                drawerContentColor = TextPrimary,
                modifier = Modifier.width(300.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                // Drawer Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SmartSpend",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = AccentGreen
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(modifier = Modifier.padding(horizontal = 24.dp), color = TextSecondary.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(24.dp))

                // Navigation Items
                NavigationDrawerItem(
                    label = { Text("Home") },
                    selected = true,
                    onClick = { scope.launch { drawerState.close() } },
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    modifier = Modifier.padding(horizontal = 12.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = AccentGreen.copy(alpha = 0.2f),
                        selectedIconColor = AccentGreen,
                        selectedTextColor = AccentGreen,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                )

                NavigationDrawerItem(
                    label = { Text("Tier Management") },
                    selected = false,
                    onClick = { 
                        scope.launch { 
                            drawerState.close() 
                            onTierManagementClick() 
                        } 
                    },
                    icon = { Icon(Icons.Default.Star, contentDescription = null) }, // Magic/Star icon
                    modifier = Modifier.padding(horizontal = 12.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    )
                )
            }
        }
    ) {
        Scaffold(
            containerColor = SurfaceBackground,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "SmartSpend",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = Icons.Default.Menu,
                                contentDescription = "Menu",
                                tint = TextPrimary
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Pick Date",
                                tint = TextPrimary
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = SurfaceBackground
                    )
                )
            },
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
                    Spacer(modifier = Modifier.height(8.dp))
                    
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
                        
                        // Check if we can go back (not before install date)
                        val isPreviousEnabled = when (selectedPeriod) {
                            TimePeriod.MONTH -> {
                                val prevMonth = currentDate.minusMonths(1)
                                !prevMonth.withDayOfMonth(1).isBefore(installDate.withDayOfMonth(1))
                            }
                            TimePeriod.YEAR -> {
                                val prevYear = currentDate.minusYears(1)
                                prevYear.year >= installDate.year
                            }
                            else -> true
                        }

                        DateNavigator(
                            currentDate = currentDate,
                            period = selectedPeriod,
                            onPrevious = onPreviousPeriod,
                            onNext = onNextPeriod,
                            isPreviousEnabled = isPreviousEnabled,
                            isNextEnabled = isNextEnabled
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    TotalSpendingCard(
                        total = totalSpending, 
                        currencyFormatter = currencyFormatter,
                        period = selectedPeriod,
                        date = currentDate,
                        budget = monthlyBudget,
                        onClick = onTotalClick,
                        onEditBudget = { showBudgetDialog = true }
                    )
                    
                    // Streak Badge
                    if (streakCount > 0) {
                        Spacer(modifier = Modifier.height(12.dp))
                        StreakBadge(streakCount = streakCount)
                    }
                    
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
    }
    
    // Budget Dialog
    if (showBudgetDialog) {
        BudgetDialog(
            currentBudget = monthlyBudget,
            onDismiss = { showBudgetDialog = false },
            onConfirm = { newBudget ->
                onSetBudget(newBudget)
                showBudgetDialog = false
            }
        )
    }
    
    // Month/Year Picker Dialog
    if (showDatePicker) {
        MonthYearPickerDialog(
            currentDate = currentDate,
            installDate = installDate,
            onDismiss = { showDatePicker = false },
            onConfirm = { selectedDate ->
                onDateSelected(selectedDate)
                showDatePicker = false
            }
        )
    }
}

@Composable
private fun BudgetDialog(
    currentBudget: Double?,
    onDismiss: () -> Unit,
    onConfirm: (Double?) -> Unit
) {
    var budgetText by remember { mutableStateOf(currentBudget?.toString() ?: "") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = { Text("Set Monthly Budget") },
        text = {
            Column {
                Text(
                    "Enter your budget for this month:",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = budgetText,
                    onValueChange = { budgetText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Budget Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentGreen,
                        unfocusedBorderColor = TextSecondary,
                        cursorColor = AccentGreen,
                        focusedLabelColor = AccentGreen,
                        unfocusedLabelColor = TextSecondary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amount = budgetText.toDoubleOrNull()
                    onConfirm(amount)
                }
            ) {
                Text("Save", color = AccentGreen)
            }
        },
        dismissButton = {
            Row {
                if (currentBudget != null) {
                    TextButton(onClick = { onConfirm(null) }) {
                        Text("Clear", color = Color(0xFFEF5350))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        }
    )
}

@Composable
private fun MonthYearPickerDialog(
    currentDate: LocalDate,
    installDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit
) {
    var selectedYear by remember { mutableStateOf(currentDate.year) }
    var selectedMonth by remember { mutableStateOf(currentDate.monthValue) }
    
    val months = listOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )
    
    val currentYear = LocalDate.now().year
    val currentMonth = LocalDate.now().monthValue
    val years = (installDate.year..currentYear).toList()
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        title = { Text("Go to Date") },
        text = {
            Column {
                // Year selector
                Text("Year", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    years.takeLast(5).forEach { year ->
                        val isSelected = year == selectedYear
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) AccentGreen else CardBackground)
                                .clickable { selectedYear = year }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = year.toString(),
                                color = if (isSelected) Color.Black else TextPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Month selector
                Text("Month", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                
                // Grid of months (3 columns)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (row in 0..3) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (col in 0..2) {
                                val monthIndex = row * 3 + col
                                if (monthIndex < 12) {
                                    val monthNum = monthIndex + 1
                                    val isSelected = monthNum == selectedMonth
                                    // Disable future months in current year
                                    val isDisabled = selectedYear == currentYear && monthNum > currentMonth
                                    // Disable months before install date
                                    val isBeforeInstall = selectedYear == installDate.year && monthNum < installDate.monthValue
                                    
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                when {
                                                    isSelected -> AccentGreen
                                                    isDisabled || isBeforeInstall -> CardBackground.copy(alpha = 0.5f)
                                                    else -> CardBackground
                                                }
                                            )
                                            .clickable(enabled = !isDisabled && !isBeforeInstall) { 
                                                selectedMonth = monthNum 
                                            }
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = months[monthIndex],
                                            color = when {
                                                isSelected -> Color.Black
                                                isDisabled || isBeforeInstall -> TextSecondary.copy(alpha = 0.3f)
                                                else -> TextPrimary
                                            },
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(LocalDate.of(selectedYear, selectedMonth, 1))
                }
            ) {
                Text("Go", color = AccentGreen)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun TotalSpendingCard(
    total: Double, 
    currencyFormatter: NumberFormat,
    period: TimePeriod,
    date: LocalDate,
    budget: Double?,
    onClick: () -> Unit,
    onEditBudget: () -> Unit
) {
    val periodLabel = when (period) {
        TimePeriod.ALL -> "All Time"
        TimePeriod.MONTH -> date.format(DateTimeFormatter.ofPattern("MMMM yyyy"))
        TimePeriod.YEAR -> date.format(DateTimeFormatter.ofPattern("yyyy"))
    }
    
    // Calculate progress and colors
    val progress = if (budget != null && budget > 0) (total / budget).coerceIn(0.0, 1.5) else 0.0
    val progressColor = when {
        budget == null -> AccentGreen
        progress <= 0.75 -> AccentGreen  // Green: under 75%
        progress <= 1.0 -> Color(0xFFFFB74D)  // Yellow/Orange: 75-100%
        else -> Color(0xFFEF5350)  // Red: over budget
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
                
                // Spending amount with budget
                Row(
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = currencyFormatter.format(total),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    if (budget != null) {
                        Text(
                            text = " / ${currencyFormatter.format(budget)}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Normal,
                            color = TextPrimary.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    } else {
                        Text(
                            text = " / —",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Normal,
                            color = TextPrimary.copy(alpha = 0.5f),
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
                
                // Only show progress bar for month period when budget is set
                if (period == TimePeriod.MONTH && budget != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(TextPrimary.copy(alpha = 0.2f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth((progress.toFloat()).coerceAtMost(1f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(4.dp))
                                .background(progressColor)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Remaining or over budget text
                    val remaining = budget - total
                    Text(
                        text = if (remaining >= 0) {
                            "${currencyFormatter.format(remaining)} remaining"
                        } else {
                            "${currencyFormatter.format(-remaining)} over budget"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = progressColor,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Tap to set budget hint (only for MONTH when no budget)
                if (period == TimePeriod.MONTH && budget == null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap to set monthly budget",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary.copy(alpha = 0.6f),
                        modifier = Modifier.clickable(onClick = onEditBudget)
                    )
                }
                
                // Edit budget button (for month period when budget exists)
                if (period == TimePeriod.MONTH && budget != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Edit budget",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextPrimary.copy(alpha = 0.5f),
                        modifier = Modifier.clickable(onClick = onEditBudget)
                    )
                }
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
    isPreviousEnabled: Boolean = true,
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
        IconButton(
            onClick = onPrevious,
            enabled = isPreviousEnabled
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Previous",
                tint = if (isPreviousEnabled) TextPrimary else TextSecondary.copy(alpha = 0.3f)
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
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
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
    } catch (_: Exception) {
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
    val chipColor = fromString(category).color

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

@Composable
private fun StreakBadge(streakCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFFFF6B35),
                        Color(0xFFFF9500)
                    )
                )
            )
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = "🔥",
            fontSize = 24.sp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$streakCount month${if (streakCount > 1) "s" else ""} under budget!",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "🔥",
            fontSize = 24.sp
        )
    }
}

@Composable
private fun StreakCelebrationDialog(
    streakCount: Int,
    onDismiss: () -> Unit
) {
    // Animation for the fire emoji
    val infiniteTransition = rememberInfiniteTransition(label = "streak")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(300, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation"
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardBackground,
        title = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "🔥",
                    fontSize = 64.sp,
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            rotationZ = rotation
                        }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Streak!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF6B35)
                )
            }
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "You stayed under budget last month!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "$streakCount month${if (streakCount > 1) "s" else ""} streak",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentGreen
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Keep it up! 💪",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Awesome!", color = AccentGreen, fontWeight = FontWeight.Bold)
            }
        }
    )
}
