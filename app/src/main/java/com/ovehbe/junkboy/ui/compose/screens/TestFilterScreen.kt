package com.ovehbe.junkboy.ui.compose.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ovehbe.junkboy.R
import com.ovehbe.junkboy.database.MessageCategory
import com.ovehbe.junkboy.database.FilterType
import com.ovehbe.junkboy.filters.CustomFilter
import com.ovehbe.junkboy.classifier.SmsClassifier
import com.ovehbe.junkboy.ui.theme.*
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
            .padding(DesignLayout.ContainerPadding),
        verticalArrangement = Arrangement.spacedBy(DesignSpacing.MD)
    ) {
        // Header
        Text(
            text = stringResource(R.string.test_filter_title),
            style = MaterialTheme.typography.headlineMedium,
            color = DesignColors.Primary
        )
        
        // Input Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = DesignColors.Surface
            ),
            shape = RoundedCornerShape(DesignBorderRadius.MD)
        ) {
            Column(
                modifier = Modifier.padding(DesignSpacing.MD),
                verticalArrangement = Arrangement.spacedBy(DesignSpacing.SM)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DesignSpacing.SM)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        tint = DesignColors.Primary,
                        modifier = Modifier.size(DesignLayout.IconSize)
                    )
                    Text(
                        text = stringResource(R.string.test_filter_message_details),
                        style = MaterialTheme.typography.titleMedium,
                        color = DesignColors.Primary
                    )
                }
                
                // Sender input
                OutlinedTextField(
                    value = senderText,
                    onValueChange = { senderText = it },
                    label = { Text(stringResource(R.string.test_filter_sender_label)) },
                    placeholder = { Text(stringResource(R.string.test_filter_sender_hint)) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Person, 
                            contentDescription = null,
                            modifier = Modifier.size(DesignLayout.IconSize)
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(DesignComponents.Input.BorderRadius),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DesignColors.Primary,
                        unfocusedBorderColor = DesignColors.InputBorder,
                        focusedContainerColor = DesignColors.InputBackground,
                        unfocusedContainerColor = DesignColors.InputBackground
                    )
                )
                
                // Message input
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    label = { Text(stringResource(R.string.test_filter_message_label)) },
                    placeholder = { Text(stringResource(R.string.test_filter_message_hint)) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Message, 
                            contentDescription = null,
                            modifier = Modifier.size(DesignLayout.IconSize)
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6,
                    shape = RoundedCornerShape(DesignComponents.Input.BorderRadius),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DesignColors.Primary,
                        unfocusedBorderColor = DesignColors.InputBorder,
                        focusedContainerColor = DesignColors.InputBackground,
                        unfocusedContainerColor = DesignColors.InputBackground
                    )
                )
                
                // Test button
                Button(
                    onClick = {
                        coroutineScope.launch {
                            isProcessing = true
                            result = testFilterMessage(
                                context,
                                senderText.ifBlank { context.getString(R.string.test_filter_unknown) },
                                messageText,
                                false, // isUnderAttackMode
                                emptyList(), // customKeywords
                                emptyList(), // customRegexPatterns
                                smsClassifier
                            )
                            isProcessing = false
                        }
                    },
                    enabled = !isProcessing && messageText.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(DesignComponents.Button.Height),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DesignColors.ButtonBackground,
                        contentColor = DesignColors.ButtonText
                    ),
                    shape = RoundedCornerShape(DesignComponents.Button.BorderRadius)
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(DesignLayout.IconSize),
                            color = DesignColors.ButtonText,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(DesignSpacing.SM))
                        Text(
                            text = stringResource(R.string.test_filter_processing),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = DesignComponents.Button.FontWeight
                        )
                    } else {
                        Icon(
                            Icons.Default.FilterList, 
                            contentDescription = null,
                            modifier = Modifier.size(DesignLayout.IconSize)
                        )
                        Spacer(modifier = Modifier.width(DesignSpacing.SM))
                        Text(
                            text = stringResource(R.string.test_filter_button),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = DesignComponents.Button.FontWeight
                        )
                    }
                }
            }
        }
        
        // Result display
        result?.let { filterResult ->
            TestFilterResultCard(filterResult, context)
        }
        
        // Sample Messages Section
        SampleMessagesCard(
            onSampleSelected = { sender, message ->
                senderText = sender
                messageText = message
            }
        )
    }
}

@Composable
private fun TestFilterResultCard(result: TestFilterResult, context: android.content.Context) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (result.isBlocked) {
                DesignColors.Accent.copy(alpha = 0.1f)
            } else {
                DesignColors.Surface
            }
        ),
        shape = RoundedCornerShape(DesignBorderRadius.MD)
    ) {
        Column(
            modifier = Modifier.padding(DesignSpacing.MD),
            verticalArrangement = Arrangement.spacedBy(DesignSpacing.SM)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(DesignSpacing.SM)
                ) {
                    Icon(
                        Icons.Default.Assessment,
                        contentDescription = null,
                        tint = DesignColors.Primary,
                        modifier = Modifier.size(DesignLayout.IconSize)
                    )
                    Text(
                        text = stringResource(R.string.test_filter_result),
                        style = MaterialTheme.typography.titleMedium,
                        color = DesignColors.Primary
                    )
                }
                
                ResultBadge(
                    category = result.category,
                    isBlocked = result.isBlocked
                )
            }
            
            // Category
            ResultDetailRow(
                icon = getCategoryIcon(result.category),
                label = stringResource(R.string.test_filter_category_label),
                                        value = result.category.name,
                color = getCategoryColor(result.category)
            )
            
            // Confidence
            ResultDetailRow(
                icon = Icons.Default.TrendingUp,
                label = stringResource(R.string.test_filter_confidence_label),
                value = "${(result.confidence * 100).toInt()}%",
                color = DesignColors.Secondary
            )
            
            // Filter Type
            ResultDetailRow(
                icon = getFilterTypeIcon(result.filterType),
                label = stringResource(R.string.test_filter_type_label),
                value = result.filterType.getDisplayName(context),
                color = DesignColors.Secondary
            )
            
            // Blocked Status
            ResultDetailRow(
                icon = if (result.isBlocked) Icons.Default.Block else Icons.Default.CheckCircle,
                label = stringResource(R.string.test_filter_status_label),
                value = if (result.isBlocked) stringResource(R.string.test_filter_blocked) else stringResource(R.string.test_filter_allowed),
                color = if (result.isBlocked) DesignColors.Accent else DesignColors.Primary
            )
            
            // Details
            if (result.details.isNotEmpty()) {
                Divider(
                    color = DesignColors.Divider,
                    thickness = DesignComponents.Divider.Thickness
                )
                
                Text(
                    text = stringResource(R.string.test_filter_details),
                    style = MaterialTheme.typography.titleSmall,
                    color = DesignColors.Primary
                )
                Text(
                    text = result.details,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DesignColors.Secondary
                )
            }
        }
    }
}

@Composable
private fun ResultDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(DesignSpacing.SM)
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(DesignLayout.IconSize),
                tint = color
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = DesignColors.Primary
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ResultBadge(
    category: MessageCategory,
    isBlocked: Boolean
) {
    val (text, color) = if (isBlocked) {
        "BLOCKED" to DesignColors.Accent
    } else {
                                    category.name.uppercase() to getCategoryColor(category)
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(DesignComponents.Badge.BorderRadius),
        modifier = Modifier.clip(RoundedCornerShape(DesignComponents.Badge.BorderRadius))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(
                horizontal = DesignComponents.Badge.PaddingHorizontal,
                vertical = DesignComponents.Badge.PaddingVertical
            ),
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun SampleMessagesCard(
    onSampleSelected: (String, String) -> Unit
) {
    val sampleMessages = remember {
        listOf(
            "BANK123" to "Your OTP is 456789. Do not share with anyone.",
            "Amazon" to "Your order has been shipped. Track: ABC123",
            "SPAM123" to "CONGRATULATIONS! You won $1,000,000! Click here to claim now!!!",
            "+1234567890" to "Hey, are we still meeting for lunch today?",
            "PROMO" to "Get 50% off on all items! Limited time offer. Shop now!"
        )
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = DesignColors.Surface
        ),
        shape = RoundedCornerShape(DesignBorderRadius.MD)
    ) {
        Column(
            modifier = Modifier.padding(DesignSpacing.MD),
            verticalArrangement = Arrangement.spacedBy(DesignSpacing.SM)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(DesignSpacing.SM)
            ) {
                Icon(
                    Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = DesignColors.Primary,
                    modifier = Modifier.size(DesignLayout.IconSize)
                )
                Text(
                    text = stringResource(R.string.test_filter_sample_messages),
                    style = MaterialTheme.typography.titleMedium,
                    color = DesignColors.Primary
                )
            }
            
            Text(
                text = stringResource(R.string.test_filter_sample_description),
                style = MaterialTheme.typography.bodyMedium,
                color = DesignColors.Secondary
            )
            
            sampleMessages.forEach { (sender, message) ->
                SampleMessageItem(
                    sender = sender,
                    message = message,
                    onClick = { onSampleSelected(sender, message) }
                )
            }
        }
    }
}

@Composable
private fun SampleMessageItem(
    sender: String,
    message: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(DesignBorderRadius.SM)),
        color = DesignColors.Background,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(DesignSpacing.SM),
            verticalArrangement = Arrangement.spacedBy(DesignSpacing.XS)
        ) {
            Text(
                text = stringResource(R.string.test_filter_from, sender),
                style = MaterialTheme.typography.labelMedium,
                color = DesignColors.Primary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = DesignColors.Secondary
            )
        }
    }
}

// Helper function to test message (implementation would be similar to original)
private suspend fun testFilterMessage(
    context: android.content.Context,
    sender: String,
    message: String,
    isUnderAttackMode: Boolean,
    customKeywords: List<String>,
    customRegexPatterns: List<String>,
    smsClassifier: com.ovehbe.junkboy.classifier.SmsClassifier
): TestFilterResult {
    // Check if sender is in allowed list FIRST with flexible matching
    try {
        val database = com.ovehbe.junkboy.database.AppDatabase.getDatabase(context)
        val isAllowed = isAllowedSenderFlexible(database, sender)
        if (isAllowed) {
            return TestFilterResult(
                category = com.ovehbe.junkboy.database.MessageCategory.ALLOWED,
                filterType = com.ovehbe.junkboy.database.FilterType.ALLOWED_SENDER,
                confidence = 1.0f,
                isBlocked = false,
                details = context.getString(R.string.test_filter_reason_allowed, sender)
            )
        }
    } catch (e: Exception) {
        // Database error, continue with other checks
    }
    
    // Try keyword/regex filtering
    if (isUnderAttackMode || customKeywords.any { message.contains(it, ignoreCase = true) }) {
        return TestFilterResult(
            category = com.ovehbe.junkboy.database.MessageCategory.JUNK,
            filterType = com.ovehbe.junkboy.database.FilterType.KEYWORD_FILTER,
            confidence = 0.9f,
            isBlocked = true,
            details = context.getString(R.string.test_filter_reason_keyword)
        )
    }
    
    // Try regex patterns
    customRegexPatterns.forEach { pattern ->
        try {
            if (Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(message)) {
                return TestFilterResult(
                    category = com.ovehbe.junkboy.database.MessageCategory.JUNK,
                    filterType = com.ovehbe.junkboy.database.FilterType.REGEX_FILTER,
                    confidence = 0.95f,
                    isBlocked = true,
                    details = context.getString(R.string.test_filter_reason_regex, pattern)
                )
            }
        } catch (e: Exception) {
            // Invalid regex pattern, skip
        }
    }
    
    // Try ML classification if available
    try {
        val mlResult = smsClassifier.classify(message)
        if (mlResult != null) {
            return TestFilterResult(
                category = mlResult.category,
                filterType = com.ovehbe.junkboy.database.FilterType.ML_CLASSIFICATION,
                confidence = mlResult.confidence,
                isBlocked = mlResult.category == com.ovehbe.junkboy.database.MessageCategory.JUNK,
                details = context.getString(R.string.test_filter_reason_ml, mlResult.category.name)
            )
        }
    } catch (e: Exception) {
        // ML classifier not available or error occurred
    }
    
    // Default classification
    return TestFilterResult(
        category = com.ovehbe.junkboy.database.MessageCategory.GENERAL,
        filterType = com.ovehbe.junkboy.database.FilterType.ML_CLASSIFICATION,
        confidence = 0.5f,
        isBlocked = false,
        details = context.getString(R.string.test_filter_reason_default)
    )
}

// Helper functions

/**
 * Flexible allowed sender matching - same logic as SmsFilterService
 */
private suspend fun isAllowedSenderFlexible(
    database: com.ovehbe.junkboy.database.AppDatabase,
    sender: String
): Boolean {
    val dao = database.allowedSenderDao()
    
    // First try exact match
    if (dao.isAllowedSender(sender)) {
        return true
    }
    
    // Try case-insensitive match
    if (dao.isAllowedSenderCaseInsensitive(sender)) {
        return true
    }
    
    // For phone numbers, try normalized matching
    val normalizedSender = normalizePhoneNumber(sender)
    if (normalizedSender != sender && dao.isAllowedSenderCaseInsensitive(normalizedSender)) {
        return true
    }
    
    // Get all allowed senders and check with various normalizations
    val allowedSenders = dao.getAllowedSendersList()
    for (allowed in allowedSenders) {
        val normalizedAllowed = normalizePhoneNumber(allowed.phoneNumber)
        
        // Check if normalized versions match
        if (normalizedSender == normalizedAllowed) {
            return true
        }
        
        // Check if sender contains the allowed number or vice versa (partial match)
        if (normalizedSender.length >= 10 && normalizedAllowed.length >= 10) {
            val senderLast10 = normalizedSender.takeLast(10)
            val allowedLast10 = normalizedAllowed.takeLast(10)
            if (senderLast10 == allowedLast10) {
                return true
            }
        }
        
        // Case-insensitive text match
        if (sender.equals(allowed.phoneNumber, ignoreCase = true)) {
            return true
        }
    }
    
    return false
}

private fun normalizePhoneNumber(phone: String): String {
    return phone
        .replace(Regex("[+\\-()\\s]"), "")
        .replace(Regex("^0+"), "")
}

private fun getCategoryIcon(category: MessageCategory): androidx.compose.ui.graphics.vector.ImageVector {
    return when (category) {
        MessageCategory.GENERAL -> Icons.Default.Person
        MessageCategory.PROMOTION -> Icons.Default.LocalOffer
        MessageCategory.NOTIFICATION -> Icons.Default.Notifications
        MessageCategory.TRANSACTION -> Icons.Default.AccountBalance
        MessageCategory.JUNK -> Icons.Default.Delete
        MessageCategory.ALLOWED -> Icons.Default.Verified
    }
}

private fun getCategoryColor(category: MessageCategory): androidx.compose.ui.graphics.Color {
    return when (category) {
        MessageCategory.GENERAL -> DesignColors.Primary
        MessageCategory.PROMOTION -> DesignColors.Accent
        MessageCategory.NOTIFICATION -> DesignColors.Secondary
        MessageCategory.TRANSACTION -> DesignColors.Primary
        MessageCategory.JUNK -> DesignColors.Accent
        MessageCategory.ALLOWED -> DesignColors.AllowedMessage
    }
}

private fun getFilterTypeIcon(filterType: FilterType): androidx.compose.ui.graphics.vector.ImageVector {
    return when (filterType) {
        FilterType.ML_CLASSIFICATION -> Icons.Default.Psychology
        FilterType.KEYWORD_FILTER -> Icons.Default.TextFields
        FilterType.REGEX_FILTER -> Icons.Default.Code
        FilterType.USER_RULE -> Icons.Default.People
        FilterType.UNDER_ATTACK_MODE -> Icons.Default.Security
        FilterType.ALLOWED_SENDER -> Icons.Default.Verified
    }
}

// Extension function for display names
private fun FilterType.getDisplayName(context: android.content.Context): String = when (this) {
    FilterType.ML_CLASSIFICATION -> context.getString(R.string.test_filter_type_ai)
    FilterType.KEYWORD_FILTER -> context.getString(R.string.test_filter_type_keyword)
    FilterType.REGEX_FILTER -> context.getString(R.string.test_filter_type_regex)
    FilterType.USER_RULE -> context.getString(R.string.test_filter_type_user_rule)
    FilterType.UNDER_ATTACK_MODE -> context.getString(R.string.test_filter_type_under_attack)
    FilterType.ALLOWED_SENDER -> context.getString(R.string.test_filter_type_allowed_sender)
} 