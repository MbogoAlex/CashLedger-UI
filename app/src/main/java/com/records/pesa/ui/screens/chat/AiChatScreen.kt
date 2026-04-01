package com.records.pesa.ui.screens.chat

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.ai.client.generativeai.type.content
import com.records.pesa.AppViewModelFactory
import com.records.pesa.BuildConfig
import com.records.pesa.db.DBRepository
import com.records.pesa.db.models.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStreamReader

// ─── Navigation destination ──────────────────────────────────────────────────
object AiChatScreenDestination {
    val route = "ai_chat"
}

// ─── UI State ─────────────────────────────────────────────────────────────────
data class AiChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val consentGiven: Boolean = false,
    val showConsentDialog: Boolean = false,
    val isPremium: Boolean = false,
    val freeResponsesUsed: Int = 0,
    val showUpgradeDialog: Boolean = false,
    val pendingAttachmentName: String? = null,
    val pendingAttachmentContent: String? = null,
    val pendingAttachmentType: String? = null,
    val error: String? = null,
    val userId: Int = 0,
    val isOnline: Boolean = true
)

// ─── ViewModel ────────────────────────────────────────────────────────────────
class AiChatScreenViewModel(
    private val dbRepository: DBRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiChatUiState())
    val uiState: StateFlow<AiChatUiState> = _uiState.asStateFlow()

    private var chatSession: com.google.ai.client.generativeai.Chat? = null
    private val model = GenerativeModel(
        modelName = "gemini-1.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY,
        safetySettings = listOf(
            SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.MEDIUM_AND_ABOVE),
            SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.MEDIUM_AND_ABOVE)
        )
    )

    private var connectivityCallback: ConnectivityManager.NetworkCallback? = null

    fun initConnectivity(context: Context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        // Set initial state
        val active = cm.activeNetwork
        val caps = cm.getNetworkCapabilities(active)
        _uiState.value = _uiState.value.copy(
            isOnline = caps != null &&
                (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                 caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                 caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
        )
        // Register callback for real-time updates
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                _uiState.value = _uiState.value.copy(isOnline = true)
            }
            override fun onLost(network: Network) {
                _uiState.value = _uiState.value.copy(isOnline = false)
            }
        }
        connectivityCallback = callback
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        cm.registerNetworkCallback(request, callback)
    }

    override fun onCleared() {
        super.onCleared()
        // Connectivity callbacks are unregistered via DisposableEffect in the composable
    }

    fun unregisterConnectivity(context: Context) {
        connectivityCallback?.let { cb ->
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            try { cm.unregisterNetworkCallback(cb) } catch (_: Exception) {}
            connectivityCallback = null
        }
    }

    fun init() {
        viewModelScope.launch {
            val prefs = dbRepository.getUserPreferences()?.first()
            val user = dbRepository.getUser()?.first()
            val userId = user?.userId ?: 0
            val isPremium = prefs?.paid == true || prefs?.permanent == true
            val consentGiven = prefs?.chatConsentGiven == true

            _uiState.value = _uiState.value.copy(
                userId = userId,
                isPremium = isPremium,
                consentGiven = consentGiven,
                showConsentDialog = !consentGiven
            )

            if (consentGiven) {
                loadHistoryAndInitSession(userId)
            }
        }
    }

    private suspend fun loadHistoryAndInitSession(userId: Int) {
        val storedMessages = dbRepository.getMessagesForUserOnce(userId)
        _uiState.value = _uiState.value.copy(messages = storedMessages)

        val contextPrompt = buildContextPrompt()
        val historyContent = mutableListOf(
            content("user") { text(contextPrompt) },
            content("model") { text("I have reviewed your complete financial data. I'm your AI Financial Advisor. Ask me anything about your transactions, spending patterns, budgets, or categories.") }
        )

        storedMessages.forEach { msg ->
            historyContent.add(content(msg.role) { text(msg.content) })
        }

        chatSession = model.startChat(history = historyContent)
    }

    private suspend fun buildContextPrompt(): String = withContext(Dispatchers.IO) {
        val sb = StringBuilder()
        sb.appendLine("You are a knowledgeable personal financial advisor. The user has granted you access to their complete financial data from the mLedger app. Use this data to give accurate, specific answers. Always refer to amounts in KES.")
        sb.appendLine()

        sb.appendLine("## SOURCE 1: M-PESA Transactions (from SMS)")
        try {
            val transactions = dbRepository.getAllTransactionsOnce()
            sb.appendLine("Total: ${transactions.size} transactions")
            transactions.forEach { tx ->
                sb.appendLine("{\"date\":\"${tx.date}\",\"time\":\"${tx.time}\",\"type\":\"${tx.transactionType}\",\"amount\":${tx.transactionAmount},\"cost\":${tx.transactionCost},\"entity\":\"${tx.entity}\",\"balance\":${tx.balance},\"code\":\"${tx.transactionCode}\"}")
            }
        } catch (e: Exception) {
            sb.appendLine("(Could not load M-PESA transactions: ${e.message})")
        }
        sb.appendLine()

        sb.appendLine("## SOURCE 2: Manual Transactions (user-entered)")
        try {
            val manualTxs = dbRepository.getAllManualTransactionsOnce()
            sb.appendLine("Total: ${manualTxs.size} manual transactions")
            manualTxs.forEach { tx ->
                sb.appendLine("{\"date\":\"${tx.date}\",\"type\":\"${tx.transactionTypeName}\",\"isOutflow\":${tx.isOutflow},\"amount\":${tx.amount},\"description\":\"${tx.description}\",\"member\":\"${tx.memberName}\"}")
            }
        } catch (e: Exception) {
            sb.appendLine("(Could not load manual transactions)")
        }
        sb.appendLine()

        sb.appendLine("## SOURCE 3: Categories & Keywords")
        try {
            val categories = dbRepository.getAllCategoriesOnce()
            categories.forEach { cat ->
                val keywords = dbRepository.getKeywordsForCategoryOnce(cat.id)
                sb.appendLine("Category: \"${cat.name}\" | Keywords: ${keywords.joinToString(", ") { it.keyword }}")
            }
        } catch (e: Exception) {
            sb.appendLine("(Could not load categories)")
        }
        sb.appendLine()

        sb.appendLine("## SOURCE 4: Budgets")
        try {
            val budgets = dbRepository.getAllBudgetsOnce()
            budgets.forEach { b ->
                sb.appendLine("Budget: \"${b.name}\" | Limit: KES ${b.budgetLimit} | Spent: KES ${b.expenditure} | Active: ${b.deletedAt == null}")
            }
        } catch (e: Exception) {
            sb.appendLine("(Could not load budgets)")
        }

        sb.toString()
    }

    fun onConsentAccepted() {
        viewModelScope.launch {
            dbRepository.updateChatConsentGiven(true)
            val userId = _uiState.value.userId
            _uiState.value = _uiState.value.copy(
                consentGiven = true,
                showConsentDialog = false
            )
            loadHistoryAndInitSession(userId)
        }
    }

    fun onConsentDeclined() {
        _uiState.value = _uiState.value.copy(showConsentDialog = false)
    }

    fun onInputChanged(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text)
    }

    fun clearPendingAttachment() {
        _uiState.value = _uiState.value.copy(
            pendingAttachmentName = null,
            pendingAttachmentContent = null,
            pendingAttachmentType = null
        )
    }

    fun onFileAttached(name: String, content: String, type: String) {
        _uiState.value = _uiState.value.copy(
            pendingAttachmentName = name,
            pendingAttachmentContent = content,
            pendingAttachmentType = type
        )
    }

    fun dismissUpgradeDialog() {
        _uiState.value = _uiState.value.copy(showUpgradeDialog = false)
    }

    fun sendMessage(navigateToSubscription: () -> Unit) {
        val state = _uiState.value
        val text = state.inputText.trim()
        if (text.isEmpty() && state.pendingAttachmentContent == null) return
        if (!state.consentGiven) return
        if (!state.isOnline) {
            _uiState.value = state.copy(error = "You're offline. Please check your connection and try again.")
            return
        }

        if (!state.isPremium && state.freeResponsesUsed >= 1) {
            _uiState.value = state.copy(showUpgradeDialog = true)
            return
        }

        viewModelScope.launch {
            val userId = state.userId
            val displayText = buildString {
                if (state.pendingAttachmentName != null) append("[Attached: ${state.pendingAttachmentName}] ")
                append(text)
            }

            val userMsg = ChatMessage(
                userId = userId,
                role = "user",
                content = displayText,
                attachmentName = state.pendingAttachmentName,
                attachmentType = state.pendingAttachmentType
            )
            val insertedId = dbRepository.insertChatMessage(userMsg)
            val userMsgWithId = userMsg.copy(id = insertedId.toInt())

            val currentMessages = _uiState.value.messages.toMutableList().also { it.add(userMsgWithId) }
            _uiState.value = _uiState.value.copy(
                messages = currentMessages,
                inputText = "",
                isLoading = true,
                pendingAttachmentName = null,
                pendingAttachmentContent = null,
                pendingAttachmentType = null,
                error = null
            )

            try {
                val session = chatSession ?: run {
                    loadHistoryAndInitSession(userId)
                    chatSession!!
                }

                val messageToSend = buildString {
                    state.pendingAttachmentContent?.let { fileContent ->
                        append("## SOURCE: Uploaded File (${state.pendingAttachmentName})\n")
                        append(fileContent)
                        append("\n\n")
                    }
                    append(text)
                }

                val response = session.sendMessage(messageToSend)
                val aiText = response.text ?: "I couldn't generate a response. Please try again."

                val aiMsg = ChatMessage(userId = userId, role = "model", content = aiText)
                dbRepository.insertChatMessage(aiMsg)

                val updatedMessages = _uiState.value.messages.toMutableList().also { it.add(aiMsg) }
                _uiState.value = _uiState.value.copy(
                    messages = updatedMessages,
                    isLoading = false,
                    freeResponsesUsed = _uiState.value.freeResponsesUsed + 1
                )
            } catch (e: Exception) {
                // Roll back: remove the user message from DB and list since send failed
                dbRepository.deleteChatMessage(insertedId.toInt())
                val rolledBack = _uiState.value.messages.toMutableList().also {
                    it.removeAll { msg -> msg.id == insertedId.toInt() }
                }
                val isOfflineError = e.message?.contains("Unable to resolve host", ignoreCase = true) == true ||
                    e.message?.contains("No address associated", ignoreCase = true) == true ||
                    e.message?.contains("Network is unreachable", ignoreCase = true) == true
                _uiState.value = _uiState.value.copy(
                    messages = rolledBack,
                    inputText = displayText.removePrefix("[Attached: ${state.pendingAttachmentName}] "),
                    isLoading = false,
                    error = if (isOfflineError) "No internet connection. Message not sent." else "Failed to get response: ${e.message}"
                )
            }
        }
    }

    fun clearChat() {
        viewModelScope.launch {
            val userId = _uiState.value.userId
            dbRepository.clearChatForUser(userId)
            _uiState.value = _uiState.value.copy(messages = emptyList())
            val contextPrompt = buildContextPrompt()
            chatSession = model.startChat(history = listOf(
                content("user") { text(contextPrompt) },
                content("model") { text("I have reviewed your complete financial data. I'm your AI Financial Advisor. Ask me anything about your transactions, spending patterns, budgets, or categories.") }
            ))
        }
    }
}

// ─── Composable ───────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreenComposable(
    navigateToPreviousScreen: () -> Unit,
    navigateToSubscriptionScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: AiChatScreenViewModel = viewModel(factory = AppViewModelFactory.Factory)
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.init()
        viewModel.initConnectivity(context)
    }

    // Unregister connectivity callback when composable leaves composition
    DisposableEffect(Unit) {
        onDispose { viewModel.unregisterConnectivity(context) }
    }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val name = getFileName(context, uri) ?: "uploaded_file"
            val type = when {
                name.endsWith(".csv", ignoreCase = true) -> "csv"
                name.endsWith(".pdf", ignoreCase = true) -> "pdf"
                else -> "text"
            }
            val fileContent = try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    InputStreamReader(stream).readText()
                } ?: ""
            } catch (e: Exception) { "Could not read file: ${e.message}" }
            viewModel.onFileAttached(name, fileContent, type)
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val name = getFileName(context, uri) ?: "image"
            viewModel.onFileAttached(name, "[IMAGE: $name - image content sent to AI]", "image")
        }
    }

    if (uiState.showConsentDialog) {
        ConsentDialog(
            onAccept = { viewModel.onConsentAccepted() },
            onDecline = {
                viewModel.onConsentDeclined()
                navigateToPreviousScreen()
            }
        )
    }

    if (uiState.showUpgradeDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissUpgradeDialog() },
            icon = { Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Upgrade to Premium") },
            text = { Text("Free users can send 1 message per session. Upgrade to Premium for unlimited AI conversations.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.dismissUpgradeDialog()
                    navigateToSubscriptionScreen()
                }) { Text("Upgrade") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissUpgradeDialog() }) { Text("Maybe later") }
            }
        )
    }

    val listState = rememberLazyListState()
    val messages = uiState.messages

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column {
                            Text("AI Financial Advisor", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                            Text("Powered by Gemini", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = navigateToPreviousScreen) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = com.records.pesa.R.drawable.arrow_back),
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (messages.isNotEmpty()) {
                        IconButton(onClick = { viewModel.clearChat() }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear chat")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Offline banner
            if (!uiState.isOnline) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("📶", fontSize = 14.sp)
                    Text(
                        "You're offline — AI unavailable",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            if (messages.isEmpty() && !uiState.isLoading) {
                QuickStartChips(
                    onChipClick = { viewModel.onInputChanged(it) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages, key = { it.id }) { msg ->
                    ChatBubble(message = msg)
                }
                if (uiState.isLoading) {
                    item { TypingIndicator() }
                }
            }

            uiState.error?.let { err ->
                Text(
                    text = err,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            uiState.pendingAttachmentName?.let { name ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SuggestionChip(
                        onClick = { viewModel.clearPendingAttachment() },
                        label = { Text("📎 $name  ✕", fontSize = 12.sp) }
                    )
                }
            }

            InputRow(
                inputText = uiState.inputText,
                isLoading = uiState.isLoading,
                isOnline = uiState.isOnline,
                onInputChanged = { viewModel.onInputChanged(it) },
                onSend = { viewModel.sendMessage(navigateToSubscriptionScreen) },
                onAttachFile = { filePickerLauncher.launch("*/*") },
                onAttachImage = { imagePickerLauncher.launch("image/*") }
            )
        }
    }
}

// ─── Quick-start chips ────────────────────────────────────────────────────────
@Composable
private fun QuickStartChips(onChipClick: (String) -> Unit, modifier: Modifier = Modifier) {
    val suggestions = listOf(
        "What's my total spend this year?",
        "Who do I send money to most?",
        "How much did I spend last month?",
        "Am I over budget on any category?",
        "Give me a savings tip"
    )
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Ask me anything about your finances",
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
        )
        suggestions.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { suggestion ->
                    SuggestionChip(
                        onClick = { onChipClick(suggestion) },
                        label = { Text(suggestion, fontSize = 12.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

// ─── Chat bubble ──────────────────────────────────────────────────────────────
@Composable
private fun ChatBubble(message: ChatMessage) {
    val isUser = message.role == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            }
            Spacer(Modifier.width(6.dp))
        }
        Column(horizontalAlignment = if (isUser) Alignment.End else Alignment.Start) {
            message.attachmentName?.let { name ->
                Text(
                    "📎 $name",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
            Surface(
                shape = RoundedCornerShape(
                    topStart = if (isUser) 16.dp else 4.dp,
                    topEnd = if (isUser) 4.dp else 16.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 16.dp
                ),
                color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = if (isUser) 0.dp else 1.dp,
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                SelectionContainer {
                    Text(
                        text = message.content,
                        color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }
        if (isUser) Spacer(Modifier.width(6.dp))
    }
}

// ─── Typing indicator ─────────────────────────────────────────────────────────
@Composable
private fun TypingIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")
    Row(
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(6.dp))
        Surface(
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    val alpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f, targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, delayMillis = index * 200),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dot_$index"
                    )
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha), CircleShape)
                    )
                }
            }
        }
    }
}

// ─── Input row ────────────────────────────────────────────────────────────────
@Composable
private fun InputRow(
    inputText: String,
    isLoading: Boolean,
    isOnline: Boolean,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    onAttachFile: () -> Unit,
    onAttachImage: () -> Unit
) {
    var showAttachMenu by remember { mutableStateOf(false) }
    val canSend = !isLoading && isOnline && inputText.isNotBlank()

    Surface(
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp)
                .navigationBarsPadding()
                .imePadding(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box {
                IconButton(
                    onClick = { showAttachMenu = true },
                    enabled = !isLoading && isOnline
                ) {
                    Icon(
                        Icons.Filled.AttachFile,
                        contentDescription = "Attach file",
                        tint = if (isOnline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }
                DropdownMenu(
                    expanded = showAttachMenu,
                    onDismissRequest = { showAttachMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("📄 CSV / PDF / Text") },
                        onClick = { showAttachMenu = false; onAttachFile() }
                    )
                    DropdownMenuItem(
                        text = { Text("🖼️ Image / Screenshot") },
                        onClick = { showAttachMenu = false; onAttachImage() }
                    )
                }
            }

            OutlinedTextField(
                value = inputText,
                onValueChange = onInputChanged,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        if (isOnline) "Ask about your finances…" else "Offline — AI unavailable",
                        fontSize = 14.sp
                    )
                },
                minLines = 1,
                maxLines = 4,
                shape = RoundedCornerShape(24.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { if (canSend) onSend() }),
                enabled = !isLoading && isOnline
            )

            IconButton(
                onClick = onSend,
                enabled = canSend,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (canSend) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant,
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (canSend) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ─── Consent dialog ───────────────────────────────────────────────────────────
@Composable
private fun ConsentDialog(onAccept: () -> Unit, onDecline: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDecline,
        icon = { Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("AI Financial Advisor", fontWeight = FontWeight.Bold) },
        text = {
            Text(
                "To give you personalised financial advice, your transaction history, categories, and budget data will be sent to Google Gemini AI.\n\n" +
                "• Your data is used only for answering your questions\n" +
                "• Google may process this data per their privacy policy\n" +
                "• You can clear the chat history at any time\n\n" +
                "Do you consent to sharing your data with Gemini AI?"
            )
        },
        confirmButton = {
            Button(onClick = onAccept) { Text("I Agree") }
        },
        dismissButton = {
            TextButton(onClick = onDecline) { Text("No Thanks") }
        }
    )
}

// ─── Helpers ──────────────────────────────────────────────────────────────────
private fun getFileName(context: Context, uri: Uri): String? {
    var name: String? = null
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (idx >= 0) name = cursor.getString(idx)
        }
    }
    return name ?: uri.lastPathSegment
}
