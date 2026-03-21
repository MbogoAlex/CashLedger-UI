package com.records.pesa.reusables

/**
 * Comprehensive list of SMS sender addresses for financial institutions in Kenya
 * Includes mobile money providers, banks, and other financial services
 */
object SmsProviders {

    /**
     * Mobile Money Providers
     */
    val MOBILE_MONEY_PROVIDERS = listOf(
        // Safaricom M-PESA
        "MPESA", "M-PESA", "SAFARICOM", "MSHWARI",

        // Airtel Money
        "AIRTEL", "AIRTELMONEY", "AIRTEL-MONEY", "23283",

        // T-Kash (Telkom)
        "TKASH", "T-KASH", "TELKOM", "TKASH-KE"
    )

    /**
     * Commercial Banks
     */
    val BANKS = listOf(
        // KCB Bank
        "KCB", "KCB-BANK", "KCB_BANK", "KCBBANK", "KCB-INFO",

        // Equity Bank
        "EQUITY", "EQUITYBANK", "EQUITY-BANK", "EAZZYBANKING", "EQUITEL",

        // Cooperative Bank
        "COOPERATIVE", "COOP-BANK", "COOPBANK", "CO-OP", "COOP",

        // DTB (Diamond Trust Bank)
        "DTB", "DTB-BANK", "DTBBANK", "DIAMOND", "DIAMOND-TRUST",

        // Family Bank
        "FAMILY", "FAMILY-BANK", "FAMILYBANK", "FAMILYBK",

        // Standard Chartered
        "STANDARDBANK", "STANDARD", "SC-BANK", "STANCHART",

        // Absa Bank (formerly Barclays)
        "ABSA", "ABSA-BANK", "BARCLAYS", "BARCLAYS-BANK",

        // NCBA Bank
        "NCBA", "NCBA-BANK", "NCBABANK", "CBA", "NIC",

        // I&M Bank
        "I&MBANK", "I&M", "IM-BANK", "IMBANK",

        // Stanbic Bank
        "STANBIC", "STANBIC-BANK", "STANBICBANK",

        // CRDB Bank
        "CRDB", "CRDB-BANK", "CRDBBANK",

        // Prime Bank
        "PRIME", "PRIME-BANK", "PRIMEBANK",

        // UBA Bank
        "UBA", "UBA-BANK", "UBABANK",

        // Citibank
        "CITIBANK", "CITI", "CITI-BANK",

        // HFC Bank (now part of Co-operative Bank)
        "HFC", "HFC-BANK", "HFCBANK",

        // First Community Bank
        "FCB", "FCB-BANK", "FCBBANK", "FIRST-COMMUNITY"
    )

    /**
     * Microfinance and SACCOs
     */
    val MICROFINANCE = listOf(
        // Kenya Women Microfinance Bank
        "KWFT", "KWFT-BANK", "KWFTBANK",

        // Faulu Microfinance
        "FAULU", "FAULU-BANK", "FAULUBANK",

        // SMEP Microfinance
        "SMEP", "SMEP-BANK", "SMEPBANK",

        // Various SACCOs
        "SACCO", "HARAMBEE", "UNAITAS", "TOWER"
    )

    /**
     * Digital Financial Services
     */
    val DIGITAL_SERVICES = listOf(
        // Tala (formerly Mkopo Rahisi)
        "TALA", "MKOPO", "MKOPO-RAHISI",

        // Branch
        "BRANCH", "BRANCH-KE",

        // KCB-MPESA
        "KCB-MPESA", "KCBMPESA", "KCB_MPESA",

        // Timiza (CBA Loop)
        "TIMIZA", "CBALOOP", "CBA-LOOP",

        // Fuliza
        "FULIZA", "FULIZA-MPESA"
    )

    /**
     * Payment Platforms and Fintechs
     */
    val PAYMENT_PLATFORMS = listOf(
        // PesaLink
        "PESALINK", "PESA-LINK",

        // Jenga
        "JENGA", "JENGA-BANK",

        // Cellulant
        "CELLULANT", "TINGG",

        // iPay
        "IPAY", "IPAY-AFRICA"
    )

    /**
     * Government and Utilities
     */
    val GOVERNMENT_UTILITIES = listOf(
        // Kenya Power
        "KPLC", "KENYAPOWER", "KENYA-POWER",

        // Nairobi Water
        "NAIROBI-WATER", "NCWSC",

        // KRA (Kenya Revenue Authority)
        "KRA", "ITAX",

        // NHIF
        "NHIF", "NHIF-KE"
    )

    /**
     * Get all financial SMS provider patterns
     */
    fun getAllProviderPatterns(): List<String> {
        return MOBILE_MONEY_PROVIDERS +
               BANKS +
               MICROFINANCE +
               DIGITAL_SERVICES +
               PAYMENT_PLATFORMS +
               GOVERNMENT_UTILITIES
    }

    /**
     * Check if a sender address matches any known financial provider
     */
    fun isFinancialProvider(senderAddress: String): Boolean {
        val upperCaseSender = senderAddress.uppercase()
        return getAllProviderPatterns().any { pattern ->
            upperCaseSender.contains(pattern.uppercase())
        }
    }

    /**
     * Get provider type for a sender address
     */
    fun getProviderType(senderAddress: String): String {
        val upperCaseSender = senderAddress.uppercase()

        return when {
            MOBILE_MONEY_PROVIDERS.any { upperCaseSender.contains(it.uppercase()) } -> "MOBILE_MONEY"
            BANKS.any { upperCaseSender.contains(it.uppercase()) } -> "BANK"
            MICROFINANCE.any { upperCaseSender.contains(it.uppercase()) } -> "MICROFINANCE"
            DIGITAL_SERVICES.any { upperCaseSender.contains(it.uppercase()) } -> "DIGITAL_SERVICE"
            PAYMENT_PLATFORMS.any { upperCaseSender.contains(it.uppercase()) } -> "PAYMENT_PLATFORM"
            GOVERNMENT_UTILITIES.any { upperCaseSender.contains(it.uppercase()) } -> "UTILITY"
            else -> "UNKNOWN"
        }
    }

    /**
     * Create SQL WHERE clause for filtering SMS by financial providers
     */
    fun createSmsFilterQuery(): Pair<String, Array<String>> {
        val patterns = getAllProviderPatterns()
        val whereClause = patterns.joinToString(" OR ") { "${android.provider.Telephony.Sms.ADDRESS} LIKE ?" }
        val whereArgs = patterns.map { "%$it%" }.toTypedArray()

        return Pair(whereClause, whereArgs)
    }
}