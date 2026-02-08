package com.example.smartspend.ui.analytics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartspend.data.local.Expense
import androidx.compose.ui.graphics.Paint
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.ui.geometry.Size
import androidx.compose.material.icons.rounded.Star
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import com.example.smartspend.data.chat.ChatMessage
import com.example.smartspend.data.chat.ChatService
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.core.graphics.toColorInt

// Reusing colors
private val SurfaceBackground = Color(0xFF121218)
private val CardBackground = Color(0xFF1E1E2E)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFFB0B0C0)
private val AccentGreen = Color(0xFF00D9A5)
private val AccentPurple = Color(0xFF764BA2)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    expenses: List<Expense>,
    monthlyBudget: Double,
    aiAnalysis: String?,
    isAnalyzing: Boolean,
    chatService: ChatService,
    onNavigateBack: () -> Unit,
    onAnalyzeClick: () -> Unit,
    downloadStatus: com.example.smartspend.data.ai.ModelDownloadManager.DownloadStatus,
    onDownloadClick: () -> Unit
) {
    val context = LocalContext.current
    var showBottomSheet by remember { mutableStateOf(false) }
    // Allow partial expansion - not full screen
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = SurfaceBackground,
        topBar = {
            TopAppBar(
                title = { Text("Analytics", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    // COMMENTED OUT: Download BERT Model UI
                    // Temporarily hidden as the offline model feature is being redesigned
                    /*
                    when (downloadStatus) {
                        is com.example.smartspend.data.ai.ModelDownloadManager.DownloadStatus.Idle -> {
                            IconButton(onClick = onDownloadClick) {
                                Icon(
                                    imageVector = Icons.Filled.Info, // Or Download if available
                                    contentDescription = "Download Offline Model",
                                    tint = TextSecondary
                                )
                            }
                        }
                        is com.example.smartspend.data.ai.ModelDownloadManager.DownloadStatus.Downloading -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp).padding(4.dp),
                                color = AccentGreen,
                                strokeWidth = 2.dp
                            )
                        }
                        is com.example.smartspend.data.ai.ModelDownloadManager.DownloadStatus.Completed -> {
                            // Optionally hide or show a confirmation
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Offline Ready",
                                tint = AccentGreen,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                        }
                        is com.example.smartspend.data.ai.ModelDownloadManager.DownloadStatus.Error -> {
                            IconButton(onClick = onDownloadClick) {
                                Icon(
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = "Retry Download",
                                    tint = Color(0xFFEF5350)
                                )
                            }
                        }
                    }
                    */
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showBottomSheet = true },
                containerColor = AccentGreen,
                contentColor = Color.White
            ) {
                Icon(
                    imageVector = Icons.Default.Star, // Using Star as Chat icon placeholder
                    contentDescription = "Chat"
                )
            }
        }
    ) { padding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            // 1. Spider Chart Section
            Text(
                text = "Category Distribution",
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary
            )

            if (expenses.isNotEmpty()) {
                SpiderChartCard(expenses = expenses)
            } else {
                EmptyChartState()
            }

            // 2. Monthly Spending Trend (NEW)
            Text(
                text = "6-Month Spending Trend",
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary
            )

            MonthlySpendingBarChart(expenses = expenses)

            // 3. AI Analysis Section
            Text(
                text = "AI Financial Advisor",
                style = MaterialTheme.typography.titleMedium,
                color = TextSecondary
            )

            AiAnalysisCard(
                analysis = aiAnalysis,
                isLoading = isAnalyzing,
                onAnalyze = onAnalyzeClick
            )
        }

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState,
                containerColor = CardBackground,
                contentColor = TextPrimary
            ) {
                AnalyticsChatBottomSheetContent(
                    expenses = expenses,
                    monthlyBudget = monthlyBudget,
                    chatService = chatService
                )
            }
        }
    }
}

@Composable
fun EmptyChartState() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Not enough data for chart", color = TextSecondary)
        }
    }
}

@Composable
fun SpiderChartCard(expenses: List<Expense>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(350.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val categories = remember(expenses) {
                expenses.groupBy { it.category }
                    .mapValues { entry -> entry.value.sumOf { it.amount } }
            }

            if (categories.isEmpty()) {
                Text("No data", color = TextSecondary)
            } else {
                SpiderChart(
                    data = categories,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            }
        }
    }
}

@Composable
fun SpiderChart(
    data: Map<String, Double>,
    modifier: Modifier = Modifier
) {
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(data) {
        animationProgress.animateTo(1f, animationSpec = tween(1000))
    }

    val paint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = 32f // Pixels
        }
    }

    Canvas(modifier = modifier) {
        val center = center
        val radius = size.minDimension / 2 * 0.8f
        val maxVal = data.values.maxOrNull() ?: 1.0
        val keys = data.keys.toList()
        val stepAngle = (2 * PI / keys.size)

        // Draw Web (Grid)
        val steps = 4
        for (i in 1..steps) {
            val r = radius * (i / steps.toFloat())
            drawPath(
                path = createPolygonPath(keys.size, r, center, stepAngle),
                color = TextSecondary.copy(alpha = 0.2f),
                style = Stroke(width = 2f)
            )
        }

        // Draw Axes
        keys.forEachIndexed { index, label ->
            val angle = index * stepAngle - (PI / 2) // Start from top
            val endX = center.x + (radius * cos(angle)).toFloat()
            val endY = center.y + (radius * sin(angle)).toFloat()

            drawLine(
                color = TextSecondary.copy(alpha = 0.2f),
                start = center,
                end = Offset(endX, endY),
                strokeWidth = 2f
            )

            // Draw Label
            val labelRadius = radius + 40f
            val labelX = center.x + (labelRadius * cos(angle)).toFloat()
            val labelY =
                center.y + (labelRadius * sin(angle)).toFloat() + 10f // Simple vertical adjustment

            drawContext.canvas.nativeCanvas.drawText(
                label,
                labelX,
                labelY,
                paint
            )
        }

        // Draw Data Polygon
        if (data.isNotEmpty()) {
            val path = Path()
            keys.forEachIndexed { index, key ->
                val value = (data[key] ?: 0.0).toFloat()
                val normalizedValue = (value / maxVal.toFloat()) * animationProgress.value
                val r = radius * normalizedValue

                val angle = index * stepAngle - (PI / 2)
                val x = center.x + (r * cos(angle)).toFloat()
                val y = center.y + (r * sin(angle)).toFloat()

                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)

                // Draw data points
                drawCircle(
                    color = AccentGreen,
                    radius = 8f,
                    center = Offset(x, y)
                )
            }
            path.close()

            // Fill
            drawPath(
                path = path,
                color = AccentGreen.copy(alpha = 0.3f)
            )
            // Stroke
            drawPath(
                path = path,
                color = AccentGreen,
                style = Stroke(width = 4f, cap = StrokeCap.Round)
            )
        }
    }
}

private fun createPolygonPath(sides: Int, radius: Float, center: Offset, stepAngle: Double): Path {
    val path = Path()
    for (i in 0 until sides) {
        val angle = i * stepAngle - (PI / 2)
        val x = center.x + (radius * cos(angle)).toFloat()
        val y = center.y + (radius * sin(angle)).toFloat()
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}

/**
 * Bar Chart showing spending for the last 6 months with average line
 */
@Composable
fun MonthlySpendingBarChart(expenses: List<Expense>) {
    val currencyFormatter = remember { java.text.NumberFormat.getCurrencyInstance() }
    
    // Calculate monthly totals for the last 6 months
    val monthlyData = remember(expenses) {
        val now = java.time.LocalDate.now()
        val formatter = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
        
        // Create list of last 6 months (including current)
        val months = (5 downTo 0).map { monthsAgo ->
            now.minusMonths(monthsAgo.toLong())
        }
        
        months.map { month ->
            val monthStart = month.withDayOfMonth(1)
            val monthEnd = month.withDayOfMonth(month.lengthOfMonth())
            
            val total = expenses.filter { expense ->
                try {
                    val expenseDate = java.time.LocalDateTime.parse(expense.date, formatter).toLocalDate()
                    expenseDate >= monthStart && expenseDate <= monthEnd
                } catch (e: Exception) {
                    false
                }
            }.sumOf { it.amount }
            
            val monthLabel = month.format(java.time.format.DateTimeFormatter.ofPattern("MMM"))
            monthLabel to total
        }
    }
    
    val maxValue = remember(monthlyData) { 
        monthlyData.maxOfOrNull { it.second }?.coerceAtLeast(1.0) ?: 1.0 
    }
    val average = remember(monthlyData) { 
        val nonZeroMonths = monthlyData.filter { it.second > 0 }
        if (nonZeroMonths.isNotEmpty()) nonZeroMonths.sumOf { it.second } / nonZeroMonths.size 
        else 0.0 
    }
    
    // Animation
    val animatedProgress = remember { Animatable(0f) }
    LaunchedEffect(monthlyData) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(1f, animationSpec = tween(800))
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 20.dp)
        ) {
            // Average indicator
            if (average > 0) {
                Text(
                    text = "Avg: ${currencyFormatter.format(average)}/month",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Chart area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val barWidth = (size.width - 16.dp.toPx()) / 6f
                    val chartHeight = size.height - 28.dp.toPx()
                    val startX = 8.dp.toPx()
                    
                    // Draw average line
                    if (average > 0) {
                        val avgY = chartHeight - (average / maxValue * chartHeight).toFloat()
                        drawLine(
                            color = AccentPurple,
                            start = Offset(startX, avgY),
                            end = Offset(size.width - 10.dp.toPx(), avgY),
                            strokeWidth = 2.dp.toPx(),
                            pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                floatArrayOf(10f, 10f)
                            )
                        )
                    }
                    
                    // Draw bars and labels
                    monthlyData.forEachIndexed { index, (month, value) ->
                        val barHeight = if (maxValue > 0) {
                            (value / maxValue * chartHeight * animatedProgress.value).toFloat()
                        } else 0f
                        
                        // Calculate bar center position
                        val barCenterX = startX + index * barWidth + barWidth / 2
                        val barActualWidth = barWidth * 0.6f
                        val x = barCenterX - barActualWidth / 2
                        
                        // Bar gradient effect
                        val barColor = if (value >= average && average > 0) {
                            Color(0xFFEF5350) // Red if above average
                        } else {
                            AccentGreen // Green if below average
                        }
                        
                        // Draw bar
                        if (barHeight > 0) {
                            drawRoundRect(
                                color = barColor,
                                topLeft = Offset(x, chartHeight - barHeight),
                                size = Size(barActualWidth, barHeight),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f, 8f)
                            )
                        }
                        
                        // Draw month label centered under each bar
                        drawContext.canvas.nativeCanvas.apply {
                            val textPaint = android.graphics.Paint().apply {
                                color = "#B0B0C0".toColorInt()
                                textSize = 11.dp.toPx()
                                textAlign = android.graphics.Paint.Align.CENTER
                                isAntiAlias = true
                            }
                            drawText(
                                month,
                                barCenterX,
                                chartHeight + 18.dp.toPx(),
                                textPaint
                            )
                        }
                    }
                }
            }
            
            // Legend
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(AccentGreen, RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Below Avg", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(Color(0xFFEF5350), RoundedCornerShape(2.dp))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Above Avg", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Box(
                    modifier = Modifier
                        .width(16.dp)
                        .height(2.dp)
                        .background(AccentPurple)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Average", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }
        }
    }
}

@Composable
fun AiAnalysisCard(
    analysis: String?,
    isLoading: Boolean,
    onAnalyze: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground)
    ) {
        Column(
            modifier = Modifier.padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "AI",
                        tint = AccentPurple
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Smart Insights",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (!isLoading) {
                    IconButton(onClick = onAnalyze) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Generate",
                            tint = AccentGreen
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (isLoading) {
                    CircularProgressIndicator(color = AccentGreen)
                } else if (analysis != null) {
                    Text(
                        text = analysis,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        lineHeight = 24.sp
                    )
                } else {
                    Button(
                        onClick = onAnalyze,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentPurple
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("✨ Generate Analysis")
                    }
                }
            }
        }
    }
}

@Composable
fun AnalyticsChatBottomSheetContent(
    expenses: List<Expense>,
    monthlyBudget: Double,
    chatService: ChatService
) {
    val totalFormat =
        java.text.NumberFormat.getCurrencyInstance().format(expenses.sumOf { it.amount })
    var messages by remember {
        mutableStateOf(
            listOf(
                ChatMessage(
                    text = "Hi! I've analyzed your ${expenses.size} transactions. Total spending: $totalFormat. Ask me anything! ✨",
                    isUser = false
                )
            )
        )
    }
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val commonQuestions = listOf(
        "How do I save more? 📉",
        "Am I spending too much? 🍔",
        "Rate my spending ⭐️",
        "What is my biggest expense? 💰"
    )

    fun sendMessage(text: String) {
        if (text.isNotBlank()) {
            val userMsg = ChatMessage(text = text, isUser = true)
            messages = messages + userMsg
            val query = text
            inputText = ""
            focusManager.clearFocus()

            scope.launch { listState.animateScrollToItem(messages.size - 1) }

            scope.launch {
                isLoading = true
                val response = chatService.generateAnalyticsResponse(query, expenses, monthlyBudget)
                isLoading = false
                messages = messages + response
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .imePadding() // Handle keyboard insets - pushes content above keyboard
            .padding(16.dp)
            .navigationBarsPadding()
    ) {
        // Chat List - scrollable, takes remaining space above input
        // Use weight to allow it to shrink when keyboard appears
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f, fill = false) // Can shrink but not required to fill
                .heightIn(min = 100.dp, max = 300.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                ChatBubble(message = msg)
            }
            
            // Show typing indicator while AI is generating response
            if (isLoading) {
                item {
                    TypingIndicator()
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Suggestion Chips (New Feature)
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(commonQuestions) { question ->
                SuggestionChip(
                    onClick = { sendMessage(question) },
                    label = { Text(question, color = TextPrimary) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = CardBackground,
                        labelColor = TextPrimary
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        AccentGreen.copy(alpha = 0.5f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Input Area - Fixed height, won't shrink
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp), // Fixed height prevents shrinking
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(), // Fill the fixed row height
                placeholder = { Text("Ask for insights...", color = TextSecondary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = AccentGreen,
                    focusedBorderColor = AccentGreen,
                    unfocusedBorderColor = TextSecondary
                ),
                shape = RoundedCornerShape(24.dp),
                singleLine = true, // Prevent multi-line expansion
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { sendMessage(inputText) })
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = { sendMessage(inputText) },
                modifier = Modifier
                    .size(48.dp)
                    .background(AccentGreen, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Send",
                    tint = Color.Black
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val bubbleColor = if (message.isUser) AccentGreen else Color(0xFF2A2A35)
    val textColor = if (message.isUser) Color.Black else TextPrimary
    val alignment = if (message.isUser) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.isUser) 16.dp else 4.dp,
                        bottomEnd = if (message.isUser) 4.dp else 16.dp
                    )
                )
                .background(bubbleColor)
                .padding(12.dp)
        ) {
            Text(
                text = message.text,
                color = textColor,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * Animated typing indicator with bouncing dots
 */
@Composable
fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    
    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )
    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 400),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = 4.dp,
                        bottomEnd = 16.dp
                    )
                )
                .background(Color(0xFF2A2A35))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            AccentGreen.copy(alpha = dot1Alpha),
                            CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            AccentGreen.copy(alpha = dot2Alpha),
                            CircleShape
                        )
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(
                            AccentGreen.copy(alpha = dot3Alpha),
                            CircleShape
                        )
                )
            }
        }
    }
}
