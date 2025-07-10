package com.ovehbe.junkboy.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.ovehbe.junkboy.database.FilteredMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

class CsvExporter(private val context: Context) {
    
    companion object {
        private const val TAG = "CsvExporter"
        private const val CSV_HEADER = "ID,Sender,Message,Received Date,Category,Confidence,Filter Type,Is Blocked,Is User Override,Is Read"
    }
    
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    /**
     * Export filtered messages to CSV and open share dialog
     */
    suspend fun exportAndShare(messages: List<FilteredMessage>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Create CSV file
            val csvFile = createCsvFile(messages)
            
            // Open share dialog
            withContext(Dispatchers.Main) {
                openShareDialog(csvFile)
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error exporting CSV", e)
            Result.failure(e)
        }
    }
    
    /**
     * Create CSV file from filtered messages
     */
    private fun createCsvFile(messages: List<FilteredMessage>): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "junkboy_messages_$timestamp.csv"
        
        // Create file in app's cache directory
        val file = File(context.cacheDir, fileName)
        
        FileWriter(file).use { writer ->
            // Write header
            writer.append(CSV_HEADER)
            writer.append('\n')
            
            // Write data rows
            messages.forEach { message ->
                writer.append(formatCsvRow(message))
                writer.append('\n')
            }
        }
        
        Log.d(TAG, "CSV file created: ${file.absolutePath}, size: ${file.length()} bytes")
        return file
    }
    
    /**
     * Format a message as CSV row
     */
    private fun formatCsvRow(message: FilteredMessage): String {
        return listOf(
            message.id.toString(),
            escapeCsvField(message.sender),
            escapeCsvField(message.messageBody),
            dateFormat.format(message.receivedAt),
            message.category.name,
            String.format("%.2f", message.confidence),
            message.filterType.name,
            message.isBlocked.toString(),
            message.isUserOverride.toString(),
            message.isRead.toString()
        ).joinToString(",")
    }
    
    /**
     * Escape CSV field (handle quotes and commas)
     */
    private fun escapeCsvField(field: String): String {
        val escaped = field.replace("\"", "\"\"") // Escape quotes
        return if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            "\"$escaped\"" // Wrap in quotes if contains special characters
        } else {
            escaped
        }
    }
    
    /**
     * Open Android share dialog for the CSV file
     */
    private fun openShareDialog(csvFile: File) {
        try {
            // Create content URI using FileProvider
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                csvFile
            )
            
            // Create share intent
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, contentUri)
                putExtra(Intent.EXTRA_SUBJECT, "Junkboy Filtered Messages Export")
                putExtra(Intent.EXTRA_TEXT, "Export of SMS messages filtered by Junkboy")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            
            // Create chooser
            val chooserIntent = Intent.createChooser(shareIntent, "Share CSV Export")
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            context.startActivity(chooserIntent)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error opening share dialog", e)
            
            // Fallback: try to open file manager to the file location
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(Uri.fromFile(csvFile.parentFile), "resource/folder")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e2: Exception) {
                Log.e(TAG, "Error opening file manager", e2)
            }
        }
    }
    
    /**
     * Get export statistics
     */
    fun getExportStats(messages: List<FilteredMessage>): ExportStats {
        val totalMessages = messages.size
        val blockedCount = messages.count { it.isBlocked }
        val categoryBreakdown = messages.groupingBy { it.category }.eachCount()
        val dateRange = if (messages.isNotEmpty()) {
            val dates = messages.map { it.receivedAt }.sorted()
            dates.first() to dates.last()
        } else {
            null
        }
        
        return ExportStats(
            totalMessages = totalMessages,
            blockedCount = blockedCount,
            categoryBreakdown = categoryBreakdown,
            dateRange = dateRange
        )
    }
    
    data class ExportStats(
        val totalMessages: Int,
        val blockedCount: Int,
        val categoryBreakdown: Map<com.ovehbe.junkboy.database.MessageCategory, Int>,
        val dateRange: Pair<Date, Date>?
    )
} 