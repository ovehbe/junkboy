package com.ovehbe.junkboy.classifier

import android.content.Context
import android.util.Log
import com.ovehbe.junkboy.database.FilterType
import com.ovehbe.junkboy.database.MessageCategory
import com.ovehbe.junkboy.filters.FilterResult
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.io.IOException
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.*

class SmsClassifier private constructor() {
    
    private var interpreter: Interpreter? = null
    private var labels: List<String> = emptyList()
    private var vocabulary: Map<String, Int> = emptyMap()
    private var maxSequenceLength = 100
    private var isInitialized = false
    
    companion object {
        private const val TAG = "SmsClassifier"
        private const val MODEL_FILE = "sms_model.tflite"
        private const val LABELS_FILE = "labels.txt"
        private const val VOCAB_FILE = "vocabulary.txt"
        
        @Volatile
        private var INSTANCE: SmsClassifier? = null
        
        fun getInstance(): SmsClassifier {
            return INSTANCE ?: synchronized(this) {
                val instance = SmsClassifier()
                INSTANCE = instance
                instance
            }
        }
    }
    
    fun initialize(context: Context): Boolean {
        return try {
            // Load TensorFlow Lite model
            val modelBuffer = loadModelFile(context, MODEL_FILE)
            interpreter = Interpreter(modelBuffer)
            
            // Load labels
            labels = loadLabels(context, LABELS_FILE)
            
            // Load vocabulary (if available)
            vocabulary = loadVocabulary(context, VOCAB_FILE)
            
            isInitialized = true
            Log.d(TAG, "SmsClassifier initialized successfully")
            Log.d(TAG, "Labels: $labels")
            Log.d(TAG, "Vocabulary size: ${vocabulary.size}")
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize SmsClassifier", e)
            isInitialized = false
            false
        }
    }
    
    fun classify(text: String): FilterResult {
        if (!isInitialized || interpreter == null) {
            Log.w(TAG, "Classifier not initialized, using fallback classification")
            return fallbackClassification(text)
        }
        
        return try {
            // Preprocess the input text
            val inputArray = preprocessText(text)
            
            // Run inference
            val outputArray = Array(1) { FloatArray(labels.size) }
            interpreter?.run(inputArray, outputArray)
            
            // Get prediction results
            val predictions = outputArray[0]
            val maxIndex = predictions.indices.maxByOrNull { predictions[it] } ?: 0
            val confidence = predictions[maxIndex]
            val predictedLabel = labels.getOrNull(maxIndex) ?: "GENERAL"
            
            // Convert label to MessageCategory
            val category = when (predictedLabel.uppercase()) {
                "JUNK", "SPAM" -> MessageCategory.JUNK
                "PROMOTION", "MARKETING" -> MessageCategory.PROMOTION
                "NOTIFICATION", "ALERT" -> MessageCategory.NOTIFICATION
                "TRANSACTION", "BANKING" -> MessageCategory.TRANSACTION
                else -> MessageCategory.GENERAL
            }
            
            // Determine if message should be blocked (only junk messages)
            val isBlocked = category == MessageCategory.JUNK && confidence > 0.7f
            
            Log.d(TAG, "Classification: $predictedLabel (confidence: $confidence)")
            
            FilterResult(
                isBlocked = isBlocked,
                category = category,
                filterType = FilterType.ML_CLASSIFICATION,
                confidence = confidence,
                matchedRule = "ml_model:$predictedLabel"
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during classification", e)
            fallbackClassification(text)
        }
    }
    
    private fun preprocessText(text: String): Array<FloatArray> {
        // Simple tokenization and padding approach
        // In a real implementation, this should match your training preprocessing
        
        val cleanText = text.lowercase()
            .replace(Regex("[^a-z0-9\\sçğıöşü]"), " ") // Keep Turkish characters
            .replace(Regex("\\s+"), " ")
            .trim()
        
        val tokens = cleanText.split(" ")
        val tokenIds = mutableListOf<Int>()
        
        // Convert tokens to IDs using vocabulary
        for (token in tokens.take(maxSequenceLength)) {
            val tokenId = vocabulary[token] ?: vocabulary["<UNK>"] ?: 0
            tokenIds.add(tokenId)
        }
        
        // Pad or truncate to maxSequenceLength
        while (tokenIds.size < maxSequenceLength) {
            tokenIds.add(0) // Padding token
        }
        
        if (tokenIds.size > maxSequenceLength) {
            tokenIds.subList(maxSequenceLength, tokenIds.size).clear()
        }
        
        // Convert to float array for TensorFlow Lite
        val inputArray = Array(1) { FloatArray(maxSequenceLength) }
        for (i in tokenIds.indices) {
            inputArray[0][i] = tokenIds[i].toFloat()
        }
        
        return inputArray
    }
    
    private fun fallbackClassification(text: String): FilterResult {
        // Simple rule-based fallback when ML model is not available
        val lowerText = text.lowercase()
        
        // Check for transaction patterns FIRST (higher priority than junk)
        val transactionIndicators = listOf(
            // Turkish terms
            "banka", "ödeme", "bakiye", "hesap", "fatura", "tutar", "tutarli", 
            "abonelik", "aboneliğiniz", "son ödeme tarihi", "ödemek için",
            "kredi", "borç", "işlem", "havale", "transfer", "kart",
            // English terms
            "bank", "payment", "balance", "account", "bill", "amount",
            "subscription", "due date", "credit", "debt", "transaction",
            "transfer", "atm", "invoice"
        )
        
        if (transactionIndicators.any { lowerText.contains(it) }) {
            return FilterResult(
                isBlocked = false,
                category = MessageCategory.TRANSACTION,
                filterType = FilterType.ML_CLASSIFICATION,
                confidence = 0.8f,
                matchedRule = "ml_fallback_transaction"
            )
        }
        
        // Check for promotion patterns
        val promotionIndicators = listOf(
            "indirim", "kampanya", "fırsat", "özel", "teklif", "promosyon",
            "discount", "sale", "offer", "deal", "promotion", "special"
        )
        
        if (promotionIndicators.any { lowerText.contains(it) }) {
            return FilterResult(
                isBlocked = false,
                category = MessageCategory.PROMOTION,
                filterType = FilterType.ML_CLASSIFICATION,
                confidence = 0.7f,
                matchedRule = "ml_fallback_promotion"
            )
        }
        
        // Check for notification patterns
        val notificationIndicators = listOf(
            "hatırlatma", "bilgilendirme", "uyarı", "kod", "doğrulama", "aktivasyon",
            "reminder", "notification", "alert", "code", "verification", "activation",
            "otp", "randevu", "appointment", "delivery", "teslimat"
        )
        
        if (notificationIndicators.any { lowerText.contains(it) }) {
            return FilterResult(
                isBlocked = false,
                category = MessageCategory.NOTIFICATION,
                filterType = FilterType.ML_CLASSIFICATION,
                confidence = 0.7f,
                matchedRule = "ml_fallback_notification"
            )
        }
        
        // Check for obvious junk patterns (LAST to avoid false positives)
        val junkIndicators = listOf(
            // High confidence junk terms
            "tıklayınız", "tıkla", "kazan", "hediye", "ödül", "çekiliş", "şanslı",
            "click", "win", "prize", "winner", "congratulations", "selected",
            "bonus", "free money", "cash prize", "ücretsiz para"
        )
        
        if (junkIndicators.any { lowerText.contains(it) }) {
            return FilterResult(
                isBlocked = true,
                category = MessageCategory.JUNK,
                filterType = FilterType.ML_CLASSIFICATION,
                confidence = 0.9f,
                matchedRule = "ml_fallback_junk"
            )
        }
        
        // Default to general
        return FilterResult(
            isBlocked = false,
            category = MessageCategory.GENERAL,
            filterType = FilterType.ML_CLASSIFICATION,
            confidence = 0.5f,
            matchedRule = "ml_fallback_general"
        )
    }
    
    private fun loadModelFile(context: Context, filename: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(filename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }
    
    private fun loadLabels(context: Context, filename: String): List<String> {
        return try {
            context.assets.open(filename).bufferedReader().readLines()
        } catch (e: IOException) {
            Log.w(TAG, "Could not load labels file: $filename, using defaults")
            listOf("GENERAL", "PROMOTION", "NOTIFICATION", "TRANSACTION", "JUNK")
        }
    }
    
    private fun loadVocabulary(context: Context, filename: String): Map<String, Int> {
        return try {
            val vocab = mutableMapOf<String, Int>()
            context.assets.open(filename).bufferedReader().forEachLine { line ->
                val parts = line.split("\t")
                if (parts.size == 2) {
                    vocab[parts[0]] = parts[1].toIntOrNull() ?: 0
                }
            }
            vocab
        } catch (e: IOException) {
            Log.w(TAG, "Could not load vocabulary file: $filename, using simple tokenization")
            emptyMap()
        }
    }
    
    fun cleanup() {
        interpreter?.close()
        interpreter = null
        isInitialized = false
        Log.d(TAG, "SmsClassifier cleaned up")
    }
} 