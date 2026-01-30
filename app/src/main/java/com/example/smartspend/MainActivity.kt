package com.example.smartspend

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.smartspend.ui.MainViewModel
import com.example.smartspend.ui.add.AddExpenseScreen
import com.example.smartspend.ui.camera.CameraScreen
import com.example.smartspend.ui.home.HomeScreen
import com.example.smartspend.ui.theme.SmartSpendTheme

class MainActivity : ComponentActivity() {
    
    private val viewModel: MainViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SmartSpendTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SmartSpendApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun SmartSpendApp(viewModel: MainViewModel) {
    val context = LocalContext.current
    var currentScreen by remember { mutableStateOf("home") }
    
    val expenses by viewModel.expenses.collectAsState()
    val totalSpending by viewModel.totalSpending.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val scannedTitle by viewModel.scannedTitle.collectAsState()
    val scannedAmount by viewModel.scannedAmount.collectAsState()
    val scannedCategory by viewModel.scannedCategory.collectAsState()
    val scanError by viewModel.scanError.collectAsState()
    val showCamera by viewModel.showCamera.collectAsState()
    
    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.openCamera()
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
    
    // Render current screen
    when {
        showCamera -> {
            CameraScreen(
                onImageCaptured = { imageProxy ->
                    viewModel.processReceiptImage(imageProxy)
                },
                onClose = { viewModel.closeCamera() }
            )
        }
        currentScreen == "home" -> {
            HomeScreen(
                expenses = expenses,
                totalSpending = totalSpending ?: 0.0,
                onAddClick = { currentScreen = "add" },
                onDeleteClick = { expense -> viewModel.deleteExpense(expense) }
            )
        }
        currentScreen == "add" -> {
            AddExpenseScreen(
                onNavigateBack = { 
                    viewModel.clearScannedData()
                    currentScreen = "home" 
                },
                onSaveExpense = { title, amount, category, notes ->
                    viewModel.addExpense(title, amount, category, notes)
                    currentScreen = "home"
                },
                onScanReceipt = {
                    // Check camera permission
                    when {
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA
                        ) == PackageManager.PERMISSION_GRANTED -> {
                            viewModel.openCamera()
                        }
                        else -> {
                            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                        }
                    }
                },
                isScanning = isScanning,
                scannedTitle = scannedTitle,
                scannedAmount = scannedAmount,
                scannedCategory = scannedCategory
            )
        }
    }
}
