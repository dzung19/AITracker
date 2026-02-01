package com.example.smartspend.ui.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.smartspend.data.ai.AiTier
import com.example.smartspend.ui.MainViewModel
import com.example.smartspend.ui.add.AddExpenseScreen
import com.example.smartspend.ui.camera.CameraScreen
import com.example.smartspend.ui.home.HomeScreen
import com.example.smartspend.ui.detail.ExpenseDetailScreen
import com.example.smartspend.ui.analytics.AnalyticsScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument

/**
 * Navigation routes for the app
 */
sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object AddExpense : Screen("add_expense")
    data object Camera : Screen("camera")
    data object ExpenseDetail : Screen("expense_detail/{expenseId}") {
        fun createRoute(expenseId: Long) = "expense_detail/$expenseId"
    }
    data object Analytics : Screen("analytics")
}

/**
 * Main navigation host for the app
 */
@Composable
fun SmartSpendNavHost(
    navController: NavHostController = rememberNavController(),
    viewModel: MainViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    
    // Collect state from ViewModel
    val expenses by viewModel.expenses.collectAsState()
    val totalSpending by viewModel.totalSpending.collectAsState()
    val currentDate by viewModel.currentDate.collectAsState()
    val selectedPeriod by viewModel.selectedPeriod.collectAsState()
    
    val isScanning by viewModel.isScanning.collectAsState()
    val scannedTitle by viewModel.scannedTitle.collectAsState()
    val scannedAmount by viewModel.scannedAmount.collectAsState()
    val scannedCategory by viewModel.scannedCategory.collectAsState()
    val scannedNote by viewModel.scannedNote.collectAsState()
    val scanError by viewModel.scanError.collectAsState()
    
    // Selected Expense state
    val selectedExpense by viewModel.selectedExpense.collectAsState()

    // Analytics State
    val aiAnalysis by viewModel.aiAnalysis.collectAsState()
    val isAnalyzing by viewModel.isAnalyzing.collectAsState()
    
    // AI Tier state
    val currentAiTier by viewModel.currentAiTier.collectAsState()
    val unlockedTiers by viewModel.unlockedTiers.collectAsState()
    
    // Track pending tier for camera permission flow
    var pendingTier by remember { mutableStateOf(AiTier.BASIC) }
    
    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.selectAiTier(pendingTier)
            navController.navigate(Screen.Camera.route)
        } else {
            Toast.makeText(context, "Camera permission is required to scan receipts", Toast.LENGTH_LONG).show()
        }
    }
    
    // Show toast for scan errors
    LaunchedEffect(scanError) {
        scanError?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
        }
    }
    
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        // Home Screen
        composable(Screen.Home.route) {
            HomeScreen(
                expenses = expenses,
                totalSpending = totalSpending,
                currentDate = currentDate,
                selectedPeriod = selectedPeriod,
                onPeriodSelected = { viewModel.setTimePeriod(it) },
                onPreviousPeriod = { viewModel.previousPeriod() },
                onNextPeriod = { viewModel.nextPeriod() },
                onAddClick = { navController.navigate(Screen.AddExpense.route) },
                onDeleteClick = { expense -> viewModel.deleteExpense(expense) },
                onExpenseClick = { expenseId ->
                    navController.navigate(Screen.ExpenseDetail.createRoute(expenseId))
                },
                onTotalClick = { navController.navigate(Screen.Analytics.route) }
            )
        }
        
        // Analytics Screen
        composable(Screen.Analytics.route) {
            AnalyticsScreen(
                expenses = expenses,
                aiAnalysis = aiAnalysis,
                isAnalyzing = isAnalyzing,
                onNavigateBack = { navController.popBackStack() },
                onAnalyzeClick = { viewModel.loadAiAnalysis(forceRefresh = true) }
            )
        }

        // Expense Detail Screen
        composable(
            route = Screen.ExpenseDetail.route,
            arguments = listOf(navArgument("expenseId") { type = NavType.LongType })
        ) { backStackEntry ->
            val expenseId = backStackEntry.arguments?.getLong("expenseId")
            
            // Load expense when entering screen
            LaunchedEffect(expenseId) {
                if (expenseId != null) {
                    viewModel.getExpense(expenseId)
                }
            }
            
            ExpenseDetailScreen(
                expense = selectedExpense,
                onNavigateBack = {
                    viewModel.clearSelectedExpense()
                    navController.popBackStack()
                }
            )
        }
        
        // Add Expense Screen
        composable(Screen.AddExpense.route) {
            AddExpenseScreen(
                onNavigateBack = { 
                    viewModel.clearScannedData()
                    navController.popBackStack()
                },
                onSaveExpense = { title, amount, category, notes ->
                    viewModel.addExpense(title, amount, category, notes)
                    navController.popBackStack(Screen.Home.route, inclusive = false)
                },
                onScanReceipt = { tier ->
                    pendingTier = tier
                    // Check camera permission
                    when {
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED -> {
                            viewModel.selectAiTier(tier)
                            navController.navigate(Screen.Camera.route)
                        }
                        else -> {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                },
                isScanning = isScanning,
                scannedTitle = scannedTitle,
                scannedAmount = scannedAmount,
                scannedCategory = scannedCategory,
                scannedNote = scannedNote,
                currentAiTier = currentAiTier,
                unlockedTiers = unlockedTiers,
                onTierSelected = { tier ->
                    viewModel.selectAiTier(tier)
                }
            )
        }
        
        // Camera Screen
        composable(Screen.Camera.route) {
            CameraScreen(
                onImageCaptured = { imageProxy ->
                    viewModel.processReceiptImage(imageProxy)
                    // Navigate back to AddExpense after capturing
                    navController.popBackStack()
                },
                onClose = { 
                    navController.popBackStack()
                }
            )
        }
    }
}

