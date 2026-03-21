package com.records.pesa.workers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Telephony
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.accounts.AccountManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.ActivityCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.records.pesa.CashLedger
import com.records.pesa.container.AppContainerImpl
import com.records.pesa.models.FinancialMessagePayload
import com.records.pesa.models.SmsMessage
import com.records.pesa.reusables.SmsProviders
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

/**
 * Background worker for processing and submitting SMS messages to the backend API.
 * Handles batch processing with maximum 1000 messages per batch.
 */
class SmsSubmissionWorker(
    private val context: Context,
    private val params: WorkerParameters,
) : CoroutineWorker(context, params) {

    companion object {
        const val KEY_USER_ID = "userId"
        const val KEY_USER_PHONE = "userPhone"
        const val BATCH_SIZE = 1000
        const val TAG = "SmsSubmissionWorker"
        const val TAG2 = "FinancialMessageSIMInfo"
    }

    data class SimCardInfo(
        val subscriptionId: Int,
        val phoneNumber: String?,
        val carrierName: String?,
        val displayName: String?,
        val simSlotIndex: Int
    )

    override suspend fun doWork(): Result {
        try {
            val appContext = context.applicationContext as? CashLedger
                ?: return Result.failure(workDataOf("error" to "App context not found"))

            // Ensure AppContainer is initialized
            if (appContext.container == null) {
                appContext.container = AppContainerImpl(appContext)
            }

            val apiRepository = appContext.container.apiRepository
            val dbRepository = appContext.container.dbRepository
            val authenticationManager = appContext.container.authenticationManager

            // Get input parameters
            val userId = inputData.getLong(KEY_USER_ID, -1L)
            val userPhone = inputData.getString(KEY_USER_PHONE)

            if (userId == -1L || userPhone.isNullOrEmpty()) {
                Log.e(TAG, "Invalid input parameters: userId=$userId, userPhone=$userPhone")
                return Result.failure(workDataOf("error" to "Invalid input parameters"))
            }

            Log.d(TAG, "Starting SMS submission for user $userId")

            // Fetch SMS messages from all financial providers
            val allMessages = fetchFinancialSmsMessages(context, userPhone)
            Log.d(TAG, "Found ${allMessages.size} financial SMS messages")

            if (allMessages.isEmpty()) {
                Log.d(TAG, "No financial messages to submit")
                return Result.success(workDataOf("messagesSent" to 0))
            }

            // Submit messages in batches with authentication handling
            val totalMessagesSent = submitMessagesInBatches(
                apiRepository = apiRepository,
                authenticationManager = authenticationManager,
                messages = allMessages,
                userId = userId,
                userPhone = userPhone
            )

            Log.d(TAG, "Successfully submitted $totalMessagesSent messages")

            return Result.success(workDataOf(
                "messagesSent" to totalMessagesSent,
                "totalMessages" to allMessages.size
            ))

        } catch (e: Exception) {
            Log.e(TAG, "Error in SMS submission worker", e)
            return Result.retry()
        }
    }

    /**
     * Fetch SMS messages from all known financial providers
     */
    private fun fetchFinancialSmsMessages(context: Context, userPhone: String): List<SmsMessage> {
        val messages = mutableListOf<SmsMessage>()
        val uri = Uri.parse("content://sms/inbox")
        val projection = arrayOf(
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.SUBSCRIPTION_ID
        )

        // Use comprehensive provider patterns for filtering
        val (whereClause, whereArgs) = SmsProviders.createSmsFilterQuery()
        val sortOrder = "${Telephony.Sms.DATE} DESC"

        // Get SIM card information for proper carrier names and phone numbers
        val simInfoMap = getSimCardInfo(context)
        Log.d(TAG, "Retrieved SIM info for ${simInfoMap.size} subscriptions: ${simInfoMap.keys}")

        Log.d(TAG, "Querying SMS with ${whereArgs.size} provider patterns")

        val cursor = context.contentResolver.query(
            uri, projection, whereClause, whereArgs, sortOrder
        )

        cursor?.use {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

            while (it.moveToNext()) {
                val body = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.BODY))
                val dateMillis = it.getLong(it.getColumnIndexOrThrow(Telephony.Sms.DATE))
                val senderAddress = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS))
                val subscriptionId = it.getInt(it.getColumnIndexOrThrow(Telephony.Sms.SUBSCRIPTION_ID))

                val date = Date(dateMillis)
                val formattedDate = dateFormat.format(date)
                val formattedTime = timeFormat.format(date)

                // Verify this is a financial provider
                if (SmsProviders.isFinancialProvider(senderAddress)) {
                    // Get SIM info for this subscription ID
                    var simInfo = simInfoMap[subscriptionId]

                    // Smart SIM mapping fallback strategies
                    if (simInfo == null && simInfoMap.isNotEmpty()) {
                        simInfo = when {
                            // Invalid subscription ID (-1 is common on some devices)
                            subscriptionId == -1 -> {
                                Log.w(TAG, "Invalid subscriptionId=-1, using primary SIM")
                                // Use the SIM with slot 0 (primary) or first available
                                simInfoMap.values.find { it.simSlotIndex == 0 } ?: simInfoMap.values.first()
                            }
                            // Try to match by sender patterns (Safaricom SMS should use Safaricom SIM)
                            else -> {
                                val smartMappedSim = getSmartSimMapping(senderAddress, simInfoMap, subscriptionId)
                                if (smartMappedSim != null) {
                                    Log.d(TAG, "Smart mapped $senderAddress to ${smartMappedSim.carrierName} SIM")
                                    smartMappedSim
                                } else {
                                    Log.w(TAG, "No SIM info found for subscriptionId=$subscriptionId (available: ${simInfoMap.keys}), using primary SIM")
                                    simInfoMap.values.find { it.simSlotIndex == 0 } ?: simInfoMap.values.first()
                                }
                            }
                        }
                    }

//                    Log.d(TAG, "SMS from $senderAddress: subscriptionId=$subscriptionId, simInfo=$simInfo, availableSims=${simInfoMap.keys}")

                    messages.add(
                        SmsMessage(
                            body = body,
                            date = formattedDate,
                            time = formattedTime,
                            receivingPhoneNumber = getEffectivePhoneNumber(simInfo, userPhone),
                            carrierName = simInfo?.carrierName ?: "Unknown",
                            simSlotIndex = simInfo?.simSlotIndex ?: -1,
                            subscriptionId = subscriptionId,
                            senderAddress = senderAddress
                        )
                    )
                }
            }
        }

        Log.d(TAG, "Fetched ${messages.size} financial SMS messages")
        return messages
    }

    /**
     * Get SIM card information for proper carrier names and phone numbers
     */
    private fun getSimCardInfo(context: Context): Map<Int, SimCardInfo> {
        val simInfoMap = mutableMapOf<Int, SimCardInfo>()

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_NUMBERS) != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "Required permissions not granted - READ_PHONE_STATE: ${ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED}, READ_PHONE_NUMBERS: ${ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_NUMBERS) == PackageManager.PERMISSION_GRANTED}")
            return simInfoMap
        }

        try {
            val subscriptionManager = SubscriptionManager.from(context)
            val subscriptions = subscriptionManager.activeSubscriptionInfoList

            if (subscriptions == null) {
                Log.w(TAG, "No active subscriptions found")
                return simInfoMap
            }

            Log.d(TAG, "Found ${subscriptions.size} active subscriptions")

            subscriptions.forEach { subInfo ->
                // Try multiple methods to get phone number
                val phoneNumber = getPhoneNumberForSubscription(context, subInfo.subscriptionId, subInfo.number)

                val simCardInfo = SimCardInfo(
                    subscriptionId = subInfo.subscriptionId,
                    phoneNumber = phoneNumber,
                    carrierName = subInfo.carrierName?.toString(),
                    displayName = subInfo.displayName?.toString(),
                    simSlotIndex = subInfo.simSlotIndex
                )

                simInfoMap[subInfo.subscriptionId] = simCardInfo

                Log.d(TAG2, "SIM ${subInfo.simSlotIndex + 1}: ID=${subInfo.subscriptionId}, Carrier=${subInfo.carrierName}, Number=${phoneNumber}, Display=${subInfo.displayName}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting SIM info: ${e.message}", e)
        }

        Log.d(TAG, "Final simInfoMap: ${simInfoMap}")
        return simInfoMap
    }

    /**
     * Try multiple methods to get phone number for a specific subscription
     */
    @RequiresPermission(allOf = [Manifest.permission.READ_SMS, Manifest.permission.READ_PHONE_NUMBERS, Manifest.permission.READ_PHONE_STATE])
    private fun getPhoneNumberForSubscription(context: Context, subscriptionId: Int, subscriptionNumber: String?): String? {
        try {
            // Method 1: Use subscription number if available
            if (!subscriptionNumber.isNullOrBlank()) {
                Log.d(TAG, "Got phone number from subscription: $subscriptionNumber")
                return subscriptionNumber
            }

            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

            // Method 2: Try to get phone number using TelephonyManager with subscription ID
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                val telephonyManagerForSub = telephonyManager.createForSubscriptionId(subscriptionId)

                // Method 2a: getLine1Number
                val line1Number = telephonyManagerForSub.line1Number
                if (!line1Number.isNullOrBlank()) {
                    Log.d(TAG, "Got phone number from line1Number: $line1Number")
                    return line1Number
                }

                // Method 2b: getVoiceMailNumber (sometimes contains the phone number)
                val voiceMailNumber = telephonyManagerForSub.voiceMailNumber
                if (!voiceMailNumber.isNullOrBlank()) {
                    Log.d(TAG, "Got phone number from voiceMailNumber: $voiceMailNumber")
                    return voiceMailNumber
                }
            }

            // Method 3: Try default TelephonyManager (for primary SIM)
            val defaultLine1Number = telephonyManager.line1Number
            if (!defaultLine1Number.isNullOrBlank()) {
                Log.d(TAG, "Got phone number from default line1Number: $defaultLine1Number")
                return defaultLine1Number
            }


            Log.w(TAG, "Could not retrieve phone number for subscription $subscriptionId after trying all methods")
            return null

        } catch (e: Exception) {
            Log.e(TAG, "Error getting phone number for subscription $subscriptionId: ${e.message}")
            return null
        }
    }

    /**
     * Smart SIM mapping based on SMS sender patterns
     * Matches SMS senders to likely SIM carriers (e.g., MPESA -> Safaricom SIM)
     */
    private fun getSmartSimMapping(senderAddress: String?, simInfoMap: Map<Int, SimCardInfo>, subscriptionId: Int): SimCardInfo? {
        if (senderAddress.isNullOrEmpty()) return null

        val sender = senderAddress.uppercase()

        // Create carrier pattern mappings
        val carrierPatterns = mapOf(
            "SAFARICOM" to listOf("MPESA", "M-PESA", "SAFARICOM", "MSHWARI", "FULIZA"),
            "AIRTEL" to listOf("AIRTEL", "AIRTELMONEY", "AIRTEL-MONEY", "23283"),
            "TELKOM" to listOf("TKASH", "T-KASH", "TELKOM"),
            "EQUITEL" to listOf("EQUITEL", "EQUITY")
        )

        // Try to match sender to carrier patterns
        for ((carrier, patterns) in carrierPatterns) {
            if (patterns.any { pattern -> sender.contains(pattern) }) {
                // Find SIM with matching carrier
                val matchingSim = simInfoMap.values.find { sim ->
                    sim.carrierName?.uppercase()?.contains(carrier) == true
                }
                if (matchingSim != null) {
                    Log.d(TAG, "Matched $sender to $carrier carrier (SIM slot ${matchingSim.simSlotIndex})")
                    return matchingSim
                }
            }
        }

        Log.d(TAG, "No smart mapping found for $sender")
        return null
    }

    /**
     * Get the actual phone number for this SIM only if available
     * Returns null if the actual SIM phone number cannot be determined
     */
    private fun getEffectivePhoneNumber(simInfo: SimCardInfo?, userPhone: String): String? {
        // Only use SIM phone number if it's actually available
        if (!simInfo?.phoneNumber.isNullOrBlank()) {
            Log.d(TAG, "Using actual SIM phone number: ${simInfo!!.phoneNumber}")
            return simInfo!!.phoneNumber!!
        }

        // Don't guess or use defaults - return null if we don't know
        Log.d(TAG, "SIM phone number not available, returning null")
        return null
    }

    /**
     * Submit messages to API in batches of 1000 with automatic authentication handling
     */
    private suspend fun submitMessagesInBatches(
        apiRepository: com.records.pesa.network.ApiRepository,
        authenticationManager: com.records.pesa.service.auth.AuthenticationManager,
        messages: List<SmsMessage>,
        userId: Long,
        userPhone: String
    ): Int {
        var totalMessagesSent = 0
        val totalBatches = (messages.size + BATCH_SIZE - 1) / BATCH_SIZE

        Log.d(TAG, "Submitting ${messages.size} messages in $totalBatches batches")

        for (batchIndex in 0 until totalBatches) {
            val fromIndex = batchIndex * BATCH_SIZE
            val toIndex = minOf(fromIndex + BATCH_SIZE, messages.size)
            val batch = messages.subList(fromIndex, toIndex)

            Log.d(TAG, "Processing batch ${batchIndex + 1}/$totalBatches with ${batch.size} messages")

            try {
                // Convert SMS messages to API payload format
                val payloadBatch = batch.map { smsMessage ->
//                    Log.d("SmsSubmissionWorker", "carrierName: ${smsMessage.carrierName}, receivingPhoneNumber: ${smsMessage.receivingPhoneNumber}")
                    FinancialMessagePayload(
                        message = smsMessage.body,
                        sender = smsMessage.senderAddress ?: "Unknown",
                        receiver = smsMessage.receivingPhoneNumber ?: "Unknown",
                        timestamp = formatTimestampForBackend(smsMessage.date, smsMessage.time),
                        messageType = SmsProviders.getProviderType(smsMessage.senderAddress ?: ""),
                        bank = determineBankName(smsMessage.senderAddress),
                        carrierName = smsMessage.carrierName ?: "Unknown",
                        receivingPhoneNumber = smsMessage.receivingPhoneNumber ?: "Unknown",
                        userId = userId
                    )


                }

                // Submit batch to API with authentication handling
                val response = authenticationManager.executeWithAuth { token ->
                    apiRepository.submitMessages(
                        token = token,
                        messages = payloadBatch
                    )
                }

                if (response != null && response.isSuccessful) {
                    Log.d(TAG, "Batch ${batchIndex + 1} submitted successfully")
                    totalMessagesSent += batch.size
                } else if (response != null) {
                    Log.e(TAG, "Batch ${batchIndex + 1} failed with code: ${response.code()}")
                } else {
                    Log.e(TAG, "Batch ${batchIndex + 1} failed - authentication issue")
                }

                // Add small delay between batches to avoid overwhelming the server
                delay(500)

            } catch (e: Exception) {
                Log.e(TAG, "Failed to submit batch ${batchIndex + 1}", e)
                // Continue with next batch instead of failing entirely
            }
        }

        return totalMessagesSent
    }

    /**
     * Format timestamp from SMS date and time to ISO 8601 format for backend
     * Converts from "dd/MM/yyyy HH:mm:ss" to "yyyy-MM-ddTHH:mm:ss.SSS"
     */
    private fun formatTimestampForBackend(date: String, time: String): String {
        return try {
            // Parse the SMS date and time format: "27/09/2025" and "20:36:18"
            val inputFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            val parsedDate = inputFormatter.parse("$date $time")

            if (parsedDate != null) {
                // Convert to LocalDateTime and format as ISO 8601
                val localDateTime = LocalDateTime.ofInstant(parsedDate.toInstant(), java.time.ZoneId.systemDefault())
                localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"))
            } else {
                // Fallback to current time if parsing fails
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error formatting timestamp: $date $time", e)
            // Fallback to current time with proper format
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"))
        }
    }

    /**
     * Determine bank name from sender address for better categorization
     * Uses comprehensive pattern matching to handle various sender address formats
     */
    private fun determineBankName(senderAddress: String?): String? {
        if (senderAddress.isNullOrEmpty()) return null

        val upperSender = senderAddress.uppercase()

        return when {
            // M-PESA and Safaricom patterns
            upperSender.contains("MPESA") || upperSender.contains("M-PESA") ||
            upperSender.contains("M PESA") || upperSender.contains("SAFARICOM") ||
            upperSender.contains("MSHWARI") || upperSender.contains("M-SHWARI") ||
            upperSender.contains("M SHWARI") || upperSender.contains("FULIZA") -> "M-PESA"

            // Airtel Money patterns
            upperSender.contains("AIRTEL") || upperSender.contains("AIRTELMONEY") ||
            upperSender.contains("AIRTEL-MONEY") || upperSender.contains("AIRTEL_MONEY") ||
            upperSender.contains("AIRTEL MONEY") || upperSender.contains("23283") -> "Airtel Money"

            // T-Kash patterns
            upperSender.contains("TKASH") || upperSender.contains("T-KASH") ||
            upperSender.contains("T KASH") || upperSender.contains("TELKOM") ||
            upperSender.contains("TKASH-KE") || upperSender.contains("T_KASH") -> "T-Kash"

            // KCB Bank patterns
            upperSender.contains("KCB") || upperSender.contains("KCB-BANK") ||
            upperSender.contains("KCB_BANK") || upperSender.contains("KCBBANK") ||
            upperSender.contains("KCB BANK") || upperSender.contains("KCB-INFO") ||
            upperSender.contains("KCBINFO") || upperSender.contains("KCB INFO") ||
            upperSender.contains("KCB-MPESA") || upperSender.contains("KCBMPESA") ||
            upperSender.contains("KCB MPESA") || upperSender.contains("KCB_MPESA") -> "KCB Bank"

            // Equity Bank patterns
            upperSender.contains("EQUITY") || upperSender.contains("EQUITYBANK") ||
            upperSender.contains("EQUITY-BANK") || upperSender.contains("EQUITY_BANK") ||
            upperSender.contains("EQUITY BANK") || upperSender.contains("EAZZYBANKING") ||
            upperSender.contains("EAZZY-BANKING") || upperSender.contains("EAZZY BANKING") ||
            upperSender.contains("EQUITEL") || upperSender.contains("EAZZY") -> "Equity Bank"

            // Cooperative Bank patterns
            upperSender.contains("COOPERATIVE") || upperSender.contains("COOP") ||
            upperSender.contains("COOP-BANK") || upperSender.contains("COOPBANK") ||
            upperSender.contains("COOP BANK") || upperSender.contains("COOP_BANK") ||
            upperSender.contains("CO-OP") || upperSender.contains("CO OP") ||
            upperSender.contains("COOPERATIVE BANK") || upperSender.contains("MCOOP") -> "Cooperative Bank"

            // DTB (Diamond Trust Bank) patterns
            upperSender.contains("DTB") || upperSender.contains("DTB-BANK") ||
            upperSender.contains("DTB_BANK") || upperSender.contains("DTBBANK") ||
            upperSender.contains("DTB BANK") || upperSender.contains("DIAMOND") ||
            upperSender.contains("DIAMOND-TRUST") || upperSender.contains("DIAMOND_TRUST") ||
            upperSender.contains("DIAMOND TRUST") || upperSender.contains("DIAMONDTRUST") -> "Diamond Trust Bank"

            // Family Bank patterns
            upperSender.contains("FAMILY") || upperSender.contains("FAMILY-BANK") ||
            upperSender.contains("FAMILY_BANK") || upperSender.contains("FAMILYBANK") ||
            upperSender.contains("FAMILY BANK") || upperSender.contains("FAMILYBK") ||
            upperSender.contains("FAMILY-BK") || upperSender.contains("FAMILY BK") -> "Family Bank"

            // Standard Chartered patterns
            upperSender.contains("STANDARD") || upperSender.contains("STANDARDBANK") ||
            upperSender.contains("STANDARD-BANK") || upperSender.contains("STANDARD_BANK") ||
            upperSender.contains("STANDARD BANK") || upperSender.contains("SC-BANK") ||
            upperSender.contains("SC_BANK") || upperSender.contains("SC BANK") ||
            upperSender.contains("STANCHART") || upperSender.contains("STAN-CHART") ||
            upperSender.contains("STAN CHART") || upperSender.contains("STANCHARTKE") -> "Standard Chartered"

            // Absa Bank (formerly Barclays) patterns
            upperSender.contains("ABSA") || upperSender.contains("ABSA-BANK") ||
            upperSender.contains("ABSA_BANK") || upperSender.contains("ABSA BANK") ||
            upperSender.contains("ABSABANK") || upperSender.contains("BARCLAYS") ||
            upperSender.contains("BARCLAYS-BANK") || upperSender.contains("BARCLAYS_BANK") ||
            upperSender.contains("BARCLAYS BANK") || upperSender.contains("BARCLAYSBANK") -> "Absa Bank"

            // NCBA Bank patterns
            upperSender.contains("NCBA") || upperSender.contains("NCBA-BANK") ||
            upperSender.contains("NCBA_BANK") || upperSender.contains("NCBA BANK") ||
            upperSender.contains("NCBABANK") || upperSender.contains("CBA") ||
            upperSender.contains("CBA-BANK") || upperSender.contains("CBA_BANK") ||
            upperSender.contains("CBA BANK") || upperSender.contains("CBABANK") ||
            upperSender.contains("NIC") || upperSender.contains("NIC-BANK") ||
            upperSender.contains("NIC BANK") || upperSender.contains("NICBANK") -> "NCBA Bank"

            // I&M Bank patterns (handling various separators)
            upperSender.contains("I&M") || upperSender.contains("I&MBANK") ||
            upperSender.contains("I&M-BANK") || upperSender.contains("I&M_BANK") ||
            upperSender.contains("I&M BANK") || upperSender.contains("IM-BANK") ||
            upperSender.contains("IM_BANK") || upperSender.contains("IM BANK") ||
            upperSender.contains("IMBANK") || upperSender.contains("I AND M") ||
            upperSender.contains("I-AND-M") || upperSender.contains("I_AND_M") ||
            upperSender.contains("I M BANK") || upperSender.contains("I-M-BANK") ||
            upperSender.contains("I_M_BANK") -> "I&M Bank"

            // Stanbic Bank patterns
            upperSender.contains("STANBIC") || upperSender.contains("STANBIC-BANK") ||
            upperSender.contains("STANBIC_BANK") || upperSender.contains("STANBIC BANK") ||
            upperSender.contains("STANBICBANK") || upperSender.contains("STANDARD-BIC") ||
            upperSender.contains("STANDARD BIC") || upperSender.contains("STANDARDBIC") -> "Stanbic Bank"

            // CRDB Bank patterns
            upperSender.contains("CRDB") || upperSender.contains("CRDB-BANK") ||
            upperSender.contains("CRDB_BANK") || upperSender.contains("CRDB BANK") ||
            upperSender.contains("CRDBBANK") -> "CRDB Bank"

            // Prime Bank patterns
            upperSender.contains("PRIME") || upperSender.contains("PRIME-BANK") ||
            upperSender.contains("PRIME_BANK") || upperSender.contains("PRIME BANK") ||
            upperSender.contains("PRIMEBANK") -> "Prime Bank"

            // UBA Bank patterns
            upperSender.contains("UBA") || upperSender.contains("UBA-BANK") ||
            upperSender.contains("UBA_BANK") || upperSender.contains("UBA BANK") ||
            upperSender.contains("UBABANK") -> "UBA Bank"

            // Citibank patterns
            upperSender.contains("CITIBANK") || upperSender.contains("CITI-BANK") ||
            upperSender.contains("CITI_BANK") || upperSender.contains("CITI BANK") ||
            upperSender.contains("CITI") -> "Citibank"

            // HFC Bank patterns
            upperSender.contains("HFC") || upperSender.contains("HFC-BANK") ||
            upperSender.contains("HFC_BANK") || upperSender.contains("HFC BANK") ||
            upperSender.contains("HFCBANK") -> "HFC Bank"

            // First Community Bank patterns
            upperSender.contains("FCB") || upperSender.contains("FCB-BANK") ||
            upperSender.contains("FCB_BANK") || upperSender.contains("FCB BANK") ||
            upperSender.contains("FCBBANK") || upperSender.contains("FIRST-COMMUNITY") ||
            upperSender.contains("FIRST_COMMUNITY") || upperSender.contains("FIRST COMMUNITY") ||
            upperSender.contains("FIRSTCOMMUNITY") -> "First Community Bank"

            // Microfinance patterns
            upperSender.contains("KWFT") || upperSender.contains("KWFT-BANK") ||
            upperSender.contains("KWFT BANK") || upperSender.contains("KWFTBANK") -> "KWFT Bank"

            upperSender.contains("FAULU") || upperSender.contains("FAULU-BANK") ||
            upperSender.contains("FAULU BANK") || upperSender.contains("FAULUBANK") -> "Faulu Bank"

            upperSender.contains("SMEP") || upperSender.contains("SMEP-BANK") ||
            upperSender.contains("SMEP BANK") || upperSender.contains("SMEPBANK") -> "SMEP Bank"

            // Digital Services patterns
            upperSender.contains("TALA") || upperSender.contains("MKOPO") ||
            upperSender.contains("MKOPO-RAHISI") || upperSender.contains("MKOPO RAHISI") ||
            upperSender.contains("MKOPORAHISI") -> "Tala"

            upperSender.contains("BRANCH") || upperSender.contains("BRANCH-KE") ||
            upperSender.contains("BRANCH KE") || upperSender.contains("BRANCHKE") -> "Branch"

            upperSender.contains("TIMIZA") || upperSender.contains("CBALOOP") ||
            upperSender.contains("CBA-LOOP") || upperSender.contains("CBA LOOP") ||
            upperSender.contains("CBA_LOOP") -> "Timiza"

            // Payment Platforms patterns
            upperSender.contains("PESALINK") || upperSender.contains("PESA-LINK") ||
            upperSender.contains("PESA_LINK") || upperSender.contains("PESA LINK") -> "PesaLink"

            upperSender.contains("JENGA") || upperSender.contains("JENGA-BANK") ||
            upperSender.contains("JENGA BANK") || upperSender.contains("JENGABANK") -> "Jenga"

            upperSender.contains("CELLULANT") || upperSender.contains("TINGG") -> "Cellulant"

            upperSender.contains("IPAY") || upperSender.contains("IPAY-AFRICA") ||
            upperSender.contains("IPAY AFRICA") || upperSender.contains("IPAYAFRICA") -> "iPay"

            // Government and Utilities patterns
            upperSender.contains("KPLC") || upperSender.contains("KENYAPOWER") ||
            upperSender.contains("KENYA-POWER") || upperSender.contains("KENYA_POWER") ||
            upperSender.contains("KENYA POWER") -> "Kenya Power"

            upperSender.contains("NHIF") || upperSender.contains("NHIF-KE") ||
            upperSender.contains("NHIF KE") || upperSender.contains("NHIFKE") -> "NHIF"

            upperSender.contains("KRA") || upperSender.contains("ITAX") ||
            upperSender.contains("I-TAX") || upperSender.contains("I TAX") -> "KRA"

            // SACCO patterns
            upperSender.contains("SACCO") || upperSender.contains("HARAMBEE") ||
            upperSender.contains("UNAITAS") || upperSender.contains("TOWER") -> "SACCO"

            // If no pattern matches, return null
            else -> null
        }
    }
}