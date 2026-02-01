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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.smartspend.data.local.Expense
import java.text.NumberFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import androidx.compose.material.icons.rounded.Star
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import com.example.smartspend.data.chat.ChatMessage
import com.example.smartspend.data.chat.ChatService
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.draw.clip

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
    onNavigateBack: () -> Unit
) {
    val chatService = remember { ChatService() } // In a real app, use Hilt injection
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

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
        },
        floatingActionButton = {
            if (expense != null) {
                FloatingActionButton(
                    onClick = { showBottomSheet = true },
                    containerColor = AccentGreen,
                    contentColor = Color.Black,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Star, // AI Icon
                        contentDescription = "Chat with AI",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
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
            ExpenseContent(expense = expense, modifier = Modifier.padding(padding))
            
            if (showBottomSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showBottomSheet = false },
                    sheetState = sheetState,
                    containerColor = CardBackground,
                    contentColor = TextPrimary 
                ) {
                   ChatBottomSheetContent(
                       expense = expense, 
                       chatService = chatService
                   )
                }
            }
        }
    }
}

@Composable
private fun ExpenseContent(expense: Expense, modifier: Modifier = Modifier) {
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.getDefault()) }
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

@Composable
fun ChatBottomSheetContent(
    expense: Expense,
    chatService: ChatService
) {
    var messages by remember { mutableStateOf(listOf(
        ChatMessage(
            text = "Hi! I'm your AI Budget Assistant. I see you spent ${NumberFormat.getCurrencyInstance().format(expense.amount)} on ${expense.title}. How can I help? 🤖", 
            isUser = false
        )
    )) }
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(500.dp) // Fixed height for sheet
            .padding(16.dp)
    ) {
        // Chat List
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                ChatBubble(message = msg)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Input Area
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask about this expense...", color = TextSecondary) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = AccentGreen,
                    focusedBorderColor = AccentGreen,
                    unfocusedBorderColor = TextSecondary
                ),
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onDone = {
                    if (inputText.isNotBlank()) {
                        val userMsg = ChatMessage(text = inputText, isUser = true)
                        messages = messages + userMsg
                        val query = inputText
                        inputText = ""
                        focusManager.clearFocus()
                        
                        // Scroll to bottom
                        scope.launch { 
                            listState.animateScrollToItem(messages.size - 1)
                        }

                        // Get AI Response
                        scope.launch {
                            val response = chatService.generateResponse(query, expense)
                            messages = messages + response
                            listState.animateScrollToItem(messages.size - 1)
                        }
                    }
                })
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        val userMsg = ChatMessage(text = inputText, isUser = true)
                        messages = messages + userMsg
                        val query = inputText
                        inputText = ""
                        focusManager.clearFocus()
                        
                        scope.launch { 
                            listState.animateScrollToItem(messages.size - 1)
                        }

                        scope.launch {
                            val response = chatService.generateResponse(query, expense)
                            messages = messages + response
                            listState.animateScrollToItem(messages.size - 1)
                        }
                    }
                },
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
        Spacer(modifier = Modifier.height(16.dp)) // Keyboard spacer
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
