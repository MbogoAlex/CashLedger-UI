package com.records.pesa.workers

import android.content.Context
import android.net.Uri
import android.provider.Telephony
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.records.pesa.CashLedger
import com.records.pesa.db.models.Transaction
import com.records.pesa.mapper.toTransactionCategory
import com.records.pesa.models.MessageData
import com.records.pesa.reusables.SmsProviders
import kotlinx.coroutines.flow.first
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Migration Worker for Safaricom Data Minimization Update (March 24, 2026)
 * 
 * Purpose: Re-classify transactions that may have been misclassified before the app
 * was updated with masked phone number support.
 * 
 * What it does:
 * 1. Reads M-PESA SMS messages from device starting March 24, 2026
 * 2. For each SMS, re-parses it with the new phone masking logic
 * 3. Finds existing transaction by transaction code
 * 4. If transaction was misclassified, UPDATES it with correct values
 * 5. If transaction doesn't exist (new message), INSERTS it
 * 
 * SAFETY FEATURES:
 * ✅ NEVER deletes transactions - only updates or inserts
 * ✅ Only touches transactions where we have SMS evidence
 * ✅ Safe for users who deleted SMS messages (existing transactions preserved)
 * ✅ Safe for late updates (only fixes what's available in SMS)
 * ✅ Safe for partial SMS history (only processes what exists)
 * 
 * This worker runs once on first app launch after update to version 153+
 */
class TransactionMigrationWorker(
    private val context: Context,
    private val params: WorkerParameters,
) : CoroutineWorker(context, params) {
    
    companion object {
        private const val TAG = "TransactionMigration"
        private const val SAFARICOM_ROLLOUT_DATE = "24/03/2026" // March 24, 2026
    }
    
    override suspend fun doWork(): Result {
        try {
            Log.d(TAG, "Starting transaction migration for Safaricom data minimization update")
            
            val appContext = context.applicationContext as? CashLedger
                ?: return Result.failure()
            
            val transactionService = appContext.container.transactionService
            val categoryService = appContext.container.categoryService
            val userAccountService = appContext.container.userAccountService
            val dbRepository = appContext.container.dbRepository

            val user = dbRepository.getUsers().first().firstOrNull()
                ?: return Result.failure()
            val userAccount = userAccountService.getUserAccount(userId = user.userId).first()
                ?: return Result.failure()
            
            // Get all categories
            val categories = categoryService.getAllCategories().first()
            
            // Read SMS messages from device starting March 24, 2026
            val messagesToProcess = fetchMessagesFromDate(
                context = context,
                startDate = SAFARICOM_ROLLOUT_DATE
            )
            
            if (messagesToProcess.isEmpty()) {
                Log.d(TAG, "No messages found to process. Migration complete.")
                return Result.success()
            }
            
            Log.d(TAG, "Found ${messagesToProcess.size} messages to process")
            
            var updatedCount = 0
            var insertedCount = 0
            var skippedCount = 0
            var errorCount = 0
            
            // Process each message
            for (messageData in messagesToProcess) {
                try {
                    // Extract transaction code from SMS
                    val transactionCode = extractTransactionCode(messageData.body)
                    
                    if (transactionCode == null) {
                        skippedCount++
                        continue
                    }
                    
                    // Check if transaction already exists
                    val existingTransaction = transactionService.getTransactionByCode(transactionCode).first()
                    
                    if (existingTransaction != null) {
                        // Transaction exists - check if it needs correction
                        val classification = extractClassificationFields(messageData.body)
                        
                        if (classification != null) {
                            // Check if transaction was misclassified
                            val typeChanged = existingTransaction.transactionType != classification.transactionType
                            val entityChanged = existingTransaction.entity != classification.entity
                            
                            if (typeChanged || entityChanged) {
                                // Update transaction with correct classification
                                val corrected = existingTransaction.copy(
                                    transactionType = classification.transactionType,
                                    entity = classification.entity
                                )
                                
                                transactionService.updateTransaction(corrected)
                                
                                updatedCount++
                                Log.d(TAG, "Corrected transaction $transactionCode: " +
                                    "\"${existingTransaction.transactionType}\" → \"${classification.transactionType}\"")
                            } else {
                                // No changes needed
                                skippedCount++
                            }
                        } else {
                            skippedCount++
                        }
                    } else {
                        // Transaction doesn't exist in database
                        // This happens if user updated late - insert it now
                        transactionService.extractTransactionDetails(
                            messageDto = messageData,
                            userAccount = userAccount,
                            categories = categories.map { it.toTransactionCategory() }
                        )
                        insertedCount++
                        Log.d(TAG, "Inserted missing transaction $transactionCode")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing message: ${e.message}", e)
                    errorCount++
                }
            }
            
            Log.d(TAG, "Migration complete. Updated: $updatedCount, Inserted: $insertedCount, " +
                "Skipped: $skippedCount, Errors: $errorCount")
            return Result.success()
            
        } catch (e: Exception) {
            Log.e(TAG, "Migration failed: ${e.message}", e)
            return Result.failure()
        }
    }
    
    /**
     * Data class to hold transaction classification fields
     */
    private data class TransactionClassification(
        val transactionType: String,
        val entity: String
    )
    
    /**
     * Lightweight parser that extracts ONLY classification fields from SMS
     * 
     * This mirrors the core logic from TransactionsExtraction.java but without
     * creating a full Transaction object or inserting into database.
     * 
     * Focuses on the key decision point: "Send Money" vs "Pochi la Biashara"
     */
    private fun extractClassificationFields(message: String): TransactionClassification? {
        try {
            // Check if this is a "sent to" transaction (the problematic case)
            if (!message.contains("sent to")) {
                // Not a "Send Money" type - no classification issue
                return null
            }
            
            // Extract recipient
            val recipientPattern = Regex("sent to (.+?)(?: on )")
            val recipientMatch = recipientPattern.find(message) ?: return null
            val recipient = recipientMatch.groupValues[1].replace("\u00A0", " ")
            
            // Apply the SAME logic as TransactionsExtraction.java
            // Check if recipient contains phone-like pattern
            val hasPhone = hasPhoneLikePattern(recipient)
            
            val transactionType: String
            val entity: String
            
            if (hasPhone) {
                // Phone detected → "Send Money"
                transactionType = "Send Money"
                // Normalize entity with masked phone support
                entity = normalizeEntity(recipient)
            } else {
                // No phone → "Pochi la Biashara"
                transactionType = "Pochi la Biashara"
                entity = recipient
            }
            
            return TransactionClassification(transactionType, entity)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error extracting classification: ${e.message}")
            return null
        }
    }
    
    /**
     * Check if text contains phone-like pattern
     * Mirrors logic from TransactionsExtraction.java
     */
    private fun hasPhoneLikePattern(text: String): Boolean {
        if (text.trim().isEmpty()) return false
        
        val trimmed = text.trim()
        val length = trimmed.length
        val digitCount = trimmed.count { it.isDigit() }
        val letterCount = trimmed.count { it.isLetter() }
        val symbolCount = trimmed.count { !it.isLetterOrDigit() && !it.isWhitespace() }
        val digitRatio = if (length > 0) digitCount.toDouble() / length else 0.0
        
        // Heuristics for phone detection
        val lengthOk = length in 7..13
        val hasDigits = digitCount >= 4
        val digitRatioOk = digitRatio > 0.3
        val notAllLetters = letterCount < length * 0.5
        val hasMasking = symbolCount > 0
        
        // Calculate confidence score
        var confidence = 0.0
        if (lengthOk) confidence += 0.2
        if (hasDigits) confidence += 0.3
        if (digitRatioOk) confidence += 0.3
        if (notAllLetters) confidence += 0.1
        if (hasMasking) confidence += 0.1
        
        return confidence >= 0.6
    }
    
    /**
     * Normalize entity with masked phone number support
     * Mirrors logic from TransactionsExtraction.java
     */
    private fun normalizeEntity(entity: String): String {
        val original = entity.trim()
        
        // Strategy 1: Full phone (10 consecutive digits at end)
        val fullPhoneRegex = Regex("\\s+(\\d{10})$")
        val fullPhoneMatch = fullPhoneRegex.find(original)
        if (fullPhoneMatch != null) {
            val name = original.substring(0, fullPhoneMatch.range.first).trim()
            val phone = fullPhoneMatch.groupValues[1]
            return "$name $phone"
        }
        
        // Strategy 2: Masked phone (mix of digits and symbols)
        for (length in 13 downTo 7) {
            val patternRegex = Regex("\\s+([\\S\\s]{$length})$")
            val match = patternRegex.find(original)
            
            if (match != null) {
                val potentialPhone = match.groupValues[1].trim()
                
                if (hasPhoneLikePattern(potentialPhone)) {
                    val name = original.substring(0, match.range.first).trim()
                    val phonePattern = extractPhonePattern(potentialPhone)
                    if (phonePattern.isNotEmpty()) {
                        return "$name $phonePattern"
                    }
                }
            }
        }
        
        return original
    }
    
    /**
     * Extract phone pattern preserving visible digits
     * Mirrors logic from TransactionsExtraction.java
     */
    private fun extractPhonePattern(phone: String): String {
        val pattern = StringBuilder()
        var digitCount = 0
        
        for (c in phone) {
            if (digitCount >= 10) break
            
            when {
                c.isDigit() -> {
                    pattern.append(c)
                    digitCount++
                }
                !c.isWhitespace() && !c.isLetter() -> {
                    pattern.append('_')
                    digitCount++
                }
            }
        }
        
        return pattern.toString()
    }
    
    /**
     * Fetch M-PESA SMS messages from device starting from a specific date
     * 
     * IMPORTANT: Only reads messages that still exist on device.
     * If user deleted SMS, we won't touch those transactions (safe).
     */
    private fun fetchMessagesFromDate(context: Context, startDate: String): List<MessageData> {
        val messages = mutableListOf<MessageData>()
        val uri = Uri.parse("content://sms/inbox")
        val projection = arrayOf(Telephony.Sms.BODY, Telephony.Sms.DATE, Telephony.Sms.ADDRESS)
        
        // Parse start date
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val startDateParsed = dateFormat.parse(startDate)
        val startDateMillis = startDateParsed?.time ?: return messages
        
        // Create filter: M-PESA providers AND date >= March 24, 2026
        val (providerWhere, providerArgs) = SmsProviders.createSmsFilterQuery()
        val dateWhere = "${Telephony.Sms.DATE} >= ?"
        val whereClause = "($providerWhere) AND ($dateWhere)"
        val whereArgs = providerArgs + startDateMillis.toString()
        
        val sortOrder = "${Telephony.Sms.DATE} ASC" // Oldest first
        
        val cursor = context.contentResolver.query(uri, projection, whereClause, whereArgs, sortOrder)
        
        cursor?.use {
            val dateOutputFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            
            while (it.moveToNext()) {
                val body = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.BODY))
                val dateMillis = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.DATE))
                val senderAddress = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS))
                
                val date = Date(dateMillis)
                val formattedDate = dateOutputFormat.format(date)
                val formattedTime = timeFormat.format(date)
                
                if (SmsProviders.isFinancialProvider(senderAddress)) {
                    messages.add(MessageData(body, formattedDate, formattedTime))
                }
            }
        }
        
        return messages
    }
    
    /**
     * Extract transaction code from SMS body
     */
    private fun extractTransactionCode(messageBody: String): String? {
        val matcher = Regex("\\b\\w{10}\\b").find(messageBody)
        return matcher?.value?.trim()?.lowercase()
    }
}
