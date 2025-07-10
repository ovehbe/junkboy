package com.ovehbe.junkboy.ui.compose.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ovehbe.junkboy.database.MessageCategory
import com.ovehbe.junkboy.database.FilterType
import com.ovehbe.junkboy.filters.CustomFilter
import com.ovehbe.junkboy.classifier.SmsClassifier
import kotlinx.coroutines.launch

data class TestFilterResult(
    val category: MessageCategory,
    val confidence: Float,
    val isBlocked: Boolean,
    val filterType: FilterType,
    val details: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestFilterScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    var senderText by remember { mutableStateOf("") }
    var messageText by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<TestFilterResult?>(null) }
    
    val smsClassifier = remember { 
        val classifier = SmsClassifier.getInstance()
        classifier.initialize(context)
        classifier
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Test SMS Filter",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        // Sender input
        OutlinedTextField(
            value = senderText,
            onValueChange = { senderText = it },
            label = { Text("Sender (Phone Number or Name)") },
            placeholder = { Text("e.g., +1234567890 or BANK") },
            leadingIcon = {
                Icon(Icons.Default.Person, contentDescription = null)
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Message input
        OutlinedTextField(
            value = messageText,
            onValueChange = { messageText = it },
            label = { Text("Message Content") },
            placeholder = { Text("Enter the SMS message text to test...") },
            leadingIcon = {
                Icon(Icons.Default.Message, contentDescription = null)
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done
            ),
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 6
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Test button
        Button(
            onClick = {
                coroutineScope.launch {
                    isProcessing = true
                    result = testMessage(senderText, messageText, smsClassifier)
                    isProcessing = false
                }
            },
            enabled = !isProcessing && senderText.isNotBlank() && messageText.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Processing...")
            } else {
                Icon(Icons.Default.FilterList, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Test Filter")
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Result display
        result?.let { filterResult ->
            TestFilterResultCard(filterResult)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TestFilterResultCard(result: TestFilterResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (result.isBlocked) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filter Result",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                ResultBadge(
                    category = result.category,
                    isBlocked = result.isBlocked
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Category
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    getCategoryIcon(result.category),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Category: ${result.category.name.lowercase().replaceFirstChar { it.uppercase() }}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Confidence
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.TrendingUp,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Confidence: ${(result.confidence * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.width(8.dp))
                LinearProgressIndicator(
                    progress = result.confidence,
                    modifier = Modifier.width(100.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Filter type
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.FilterList,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Filter Type: ${result.filterType.name.lowercase().replace('_', ' ')}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Status
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (result.isBlocked) Icons.Default.Block else Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = if (result.isBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Status: ${if (result.isBlocked) "BLOCKED" else "ALLOWED"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (result.isBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            
            if (result.details.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Details: ${result.details}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResultBadge(
    category: MessageCategory,
    isBlocked: Boolean
) {
    val (color, text, icon) = when {
        isBlocked -> Triple(
            MaterialTheme.colorScheme.error,
            "BLOCKED",
            Icons.Default.Block
        )
        category == MessageCategory.JUNK -> Triple(
            MaterialTheme.colorScheme.error,
            "JUNK",
            Icons.Default.Delete
        )
        category == MessageCategory.PROMOTION -> Triple(
            MaterialTheme.colorScheme.tertiary,
            "PROMOTION",
            Icons.Default.LocalOffer
        )
        category == MessageCategory.TRANSACTION -> Triple(
            MaterialTheme.colorScheme.primary,
            "TRANSACTION",
            Icons.Default.AccountBalance
        )
        category == MessageCategory.NOTIFICATION -> Triple(
            MaterialTheme.colorScheme.secondary,
            "NOTIFICATION",
            Icons.Default.Notifications
        )
        else -> Triple(
            MaterialTheme.colorScheme.outline,
            "GENERAL",
            Icons.Default.Message
        )
    }
    
    Badge(
        containerColor = color.copy(alpha = 0.1f),
        contentColor = color
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun getCategoryIcon(category: MessageCategory) = when (category) {
    MessageCategory.GENERAL -> Icons.Default.Message
    MessageCategory.PROMOTION -> Icons.Default.LocalOffer
    MessageCategory.NOTIFICATION -> Icons.Default.Notifications
    MessageCategory.TRANSACTION -> Icons.Default.AccountBalance
    MessageCategory.JUNK -> Icons.Default.Delete
}

private suspend fun testMessage(
    sender: String,
    message: String,
    smsClassifier: SmsClassifier
): TestFilterResult {
    // First try custom filter (rule-based)
    val customResult = CustomFilter.filterMessage(message, sender)
    if (customResult.isBlocked) {
        return TestFilterResult(
            category = customResult.category,
            confidence = customResult.confidence,
            isBlocked = true,
            filterType = customResult.filterType,
            details = "Blocked by custom filter: ${customResult.matchedRule ?: "unknown rule"}"
        )
    }
    
    // Then try ML classifier
    val mlResult = smsClassifier.classify(message)
    
    return TestFilterResult(
        category = mlResult.category,
        confidence = mlResult.confidence,
        isBlocked = mlResult.isBlocked,
        filterType = mlResult.filterType,
        details = if (mlResult.isBlocked) "Blocked by ML classifier: ${mlResult.matchedRule ?: "unknown"}" else "Allowed by ML classifier"
    )
} 