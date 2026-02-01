package com.example.smartspend.ui.analytics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
    aiAnalysis: String?,
    isAnalyzing: Boolean,
    onNavigateBack: () -> Unit,
    onAnalyzeClick: () -> Unit
) {
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceBackground
                )
            )
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

            // 2. AI Analysis Section
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
    }
}

@Composable
fun EmptyChartState() {
    Card(
        modifier = Modifier.fillMaxWidth().height(250.dp),
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
            modifier = Modifier.fillMaxSize().padding(16.dp),
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
            val labelY = center.y + (labelRadius * sin(angle)).toFloat() + 10f // Simple vertical adjustment
            
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
