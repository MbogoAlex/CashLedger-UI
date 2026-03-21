package com.records.pesa.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.records.pesa.db.models.UserPreferences
import com.records.pesa.db.models.UserSession
import com.records.pesa.models.dbModel.UserDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.time.LocalDateTime

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mledger_prefs")

interface DataStoreRepository {
    suspend fun saveUserPreferences(userPreferences: UserPreferences)
    fun getUserPreferences(): Flow<UserPreferences>
    suspend fun clearUserPreferences()

    suspend fun saveUserDetails(userDetails: UserDetails)
    fun getUserDetails(): Flow<UserDetails>
    suspend fun clearUserDetails()

    suspend fun saveUserSession(userSession: UserSession)
    fun getUserSession(): Flow<UserSession>
    suspend fun clearUserSession()

    suspend fun clearAll()
}

class DataStoreRepositoryImpl(private val context: Context) : DataStoreRepository {

    private object Keys {
        // UserPreferences
        val LOGGED_IN = booleanPreferencesKey("logged_in")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val RESTORED_DATA = booleanPreferencesKey("restored_data")
        val LAST_RESTORE = stringPreferencesKey("last_restore")
        val PAID = booleanPreferencesKey("paid")
        val PERMANENT = booleanPreferencesKey("permanent")
        val PAID_AT = stringPreferencesKey("paid_at")
        val EXPIRY_DATE = stringPreferencesKey("expiry_date")
        val SHOW_BALANCE = booleanPreferencesKey("show_balance")
        val HAS_SUBMITTED_MESSAGES = booleanPreferencesKey("has_submitted_messages")
        val SAFARICOM_MIGRATION_COMPLETED = booleanPreferencesKey("safaricom_migration_completed")

        // UserDetails
        val USER_ID = intPreferencesKey("user_id")
        val BACKUP_USER_ID = longPreferencesKey("backup_user_id")
        val DYNAMO_USER_ID = longPreferencesKey("dynamo_user_id")
        val FIRST_NAME = stringPreferencesKey("first_name")
        val LAST_NAME = stringPreferencesKey("last_name")
        val EMAIL = stringPreferencesKey("email")
        val PHONE_NUMBER = stringPreferencesKey("phone_number")
        val PASSWORD = stringPreferencesKey("password")
        val TOKEN = stringPreferencesKey("token")
        val PAYMENT_STATUS = booleanPreferencesKey("payment_status")
        val PAID_AT_USER = stringPreferencesKey("paid_at_user")
        val EXPIRED_AT = stringPreferencesKey("expired_at")
        val SUPABASE_LOGIN = booleanPreferencesKey("supabase_login")
        val PERMANENT_USER = booleanPreferencesKey("permanent_user")
        val BACKUP_SET = booleanPreferencesKey("backup_set")
        val BACKUP_WORKER_INITIATED = booleanPreferencesKey("backup_worker_initiated")
        val LAST_BACKUP = stringPreferencesKey("last_backup")
        val BACKED_UP_ITEMS_SIZE = intPreferencesKey("backed_up_items_size")
        val TRANSACTIONS = intPreferencesKey("transactions")
        val CATEGORIES = intPreferencesKey("categories")
        val CATEGORY_KEYWORDS = intPreferencesKey("category_keywords")
        val CATEGORY_MAPPINGS = intPreferencesKey("category_mappings")
        val DARK_THEME_SET = booleanPreferencesKey("dark_theme_set")

        // UserSession
        val SESSION_USER_ID = longPreferencesKey("session_user_id")
        val ACCESS_TOKEN = stringPreferencesKey("access_token")
        val REFRESH_TOKEN = stringPreferencesKey("refresh_token")
        val TOKEN_TYPE = stringPreferencesKey("token_type")
        val ACCESS_TOKEN_EXPIRES_IN = longPreferencesKey("access_token_expires_in")
        val REFRESH_TOKEN_EXPIRES_IN = longPreferencesKey("refresh_token_expires_in")
    }

    // ─── UserPreferences ────────────────────────────────────────────────────

    override suspend fun saveUserPreferences(userPreferences: UserPreferences) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LOGGED_IN] = userPreferences.loggedIn
            prefs[Keys.DARK_MODE] = userPreferences.darkMode
            prefs[Keys.RESTORED_DATA] = userPreferences.restoredData
            prefs[Keys.LAST_RESTORE] = userPreferences.lastRestore?.toString() ?: ""
            prefs[Keys.PAID] = userPreferences.paid
            prefs[Keys.PERMANENT] = userPreferences.permanent
            prefs[Keys.PAID_AT] = userPreferences.paidAt?.toString() ?: ""
            prefs[Keys.EXPIRY_DATE] = userPreferences.expiryDate?.toString() ?: ""
            prefs[Keys.SHOW_BALANCE] = userPreferences.showBalance
            prefs[Keys.HAS_SUBMITTED_MESSAGES] = userPreferences.hasSubmittedMessages
            prefs[Keys.SAFARICOM_MIGRATION_COMPLETED] = userPreferences.safaricomMigrationCompleted
        }
    }

    override fun getUserPreferences(): Flow<UserPreferences> {
        return context.dataStore.data
            .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
            .map { prefs ->
                UserPreferences(
                    loggedIn = prefs[Keys.LOGGED_IN] ?: false,
                    darkMode = prefs[Keys.DARK_MODE] ?: false,
                    restoredData = prefs[Keys.RESTORED_DATA] ?: false,
                    lastRestore = prefs[Keys.LAST_RESTORE]?.takeIf { it.isNotEmpty() }
                        ?.let { LocalDateTime.parse(it) },
                    paid = prefs[Keys.PAID] ?: false,
                    permanent = prefs[Keys.PERMANENT] ?: false,
                    paidAt = prefs[Keys.PAID_AT]?.takeIf { it.isNotEmpty() }
                        ?.let { LocalDateTime.parse(it) },
                    expiryDate = prefs[Keys.EXPIRY_DATE]?.takeIf { it.isNotEmpty() }
                        ?.let { LocalDateTime.parse(it) },
                    showBalance = prefs[Keys.SHOW_BALANCE] ?: true,
                    hasSubmittedMessages = prefs[Keys.HAS_SUBMITTED_MESSAGES] ?: false,
                    safaricomMigrationCompleted = prefs[Keys.SAFARICOM_MIGRATION_COMPLETED] ?: false,
                )
            }
    }

    override suspend fun clearUserPreferences() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.LOGGED_IN)
            prefs.remove(Keys.DARK_MODE)
            prefs.remove(Keys.RESTORED_DATA)
            prefs.remove(Keys.LAST_RESTORE)
            prefs.remove(Keys.PAID)
            prefs.remove(Keys.PERMANENT)
            prefs.remove(Keys.PAID_AT)
            prefs.remove(Keys.EXPIRY_DATE)
            prefs.remove(Keys.SHOW_BALANCE)
            prefs.remove(Keys.HAS_SUBMITTED_MESSAGES)
        }
    }

    // ─── UserDetails ────────────────────────────────────────────────────────

    override suspend fun saveUserDetails(userDetails: UserDetails) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USER_ID] = userDetails.userId
            prefs[Keys.BACKUP_USER_ID] = userDetails.backUpUserId
            userDetails.dynamoUserId?.let { prefs[Keys.DYNAMO_USER_ID] = it }
            prefs[Keys.FIRST_NAME] = userDetails.firstName ?: ""
            prefs[Keys.LAST_NAME] = userDetails.lastName ?: ""
            prefs[Keys.EMAIL] = userDetails.email ?: ""
            prefs[Keys.PHONE_NUMBER] = userDetails.phoneNumber
            prefs[Keys.PASSWORD] = userDetails.password
            prefs[Keys.TOKEN] = userDetails.token
            prefs[Keys.PAYMENT_STATUS] = userDetails.paymentStatus
            prefs[Keys.PAID_AT_USER] = userDetails.paidAt ?: ""
            prefs[Keys.EXPIRED_AT] = userDetails.expiredAt ?: ""
            prefs[Keys.SUPABASE_LOGIN] = userDetails.supabaseLogin
            prefs[Keys.PERMANENT_USER] = userDetails.permanent
            prefs[Keys.BACKUP_SET] = userDetails.backupSet
            prefs[Keys.BACKUP_WORKER_INITIATED] = userDetails.backupWorkerInitiated
            prefs[Keys.LAST_BACKUP] = userDetails.lastBackup?.toString() ?: ""
            prefs[Keys.BACKED_UP_ITEMS_SIZE] = userDetails.backedUpItemsSize
            prefs[Keys.TRANSACTIONS] = userDetails.transactions
            prefs[Keys.CATEGORIES] = userDetails.categories
            prefs[Keys.CATEGORY_KEYWORDS] = userDetails.categoryKeywords
            prefs[Keys.CATEGORY_MAPPINGS] = userDetails.categoryMappings
            prefs[Keys.DARK_THEME_SET] = userDetails.darkThemeSet
        }
    }

    override fun getUserDetails(): Flow<UserDetails> {
        return context.dataStore.data
            .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
            .map { prefs ->
                UserDetails(
                    userId = prefs[Keys.USER_ID] ?: 0,
                    backUpUserId = prefs[Keys.BACKUP_USER_ID] ?: 0L,
                    dynamoUserId = prefs[Keys.DYNAMO_USER_ID],
                    firstName = prefs[Keys.FIRST_NAME]?.takeIf { it.isNotEmpty() },
                    lastName = prefs[Keys.LAST_NAME]?.takeIf { it.isNotEmpty() },
                    email = prefs[Keys.EMAIL]?.takeIf { it.isNotEmpty() },
                    phoneNumber = prefs[Keys.PHONE_NUMBER] ?: "",
                    password = prefs[Keys.PASSWORD] ?: "",
                    token = prefs[Keys.TOKEN] ?: "",
                    paymentStatus = prefs[Keys.PAYMENT_STATUS] ?: false,
                    paidAt = prefs[Keys.PAID_AT_USER]?.takeIf { it.isNotEmpty() },
                    expiredAt = prefs[Keys.EXPIRED_AT]?.takeIf { it.isNotEmpty() },
                    supabaseLogin = prefs[Keys.SUPABASE_LOGIN] ?: false,
                    permanent = prefs[Keys.PERMANENT_USER] ?: false,
                    backupSet = prefs[Keys.BACKUP_SET] ?: false,
                    backupWorkerInitiated = prefs[Keys.BACKUP_WORKER_INITIATED] ?: false,
                    lastBackup = prefs[Keys.LAST_BACKUP]?.takeIf { it.isNotEmpty() }
                        ?.let { LocalDateTime.parse(it) },
                    backedUpItemsSize = prefs[Keys.BACKED_UP_ITEMS_SIZE] ?: 0,
                    transactions = prefs[Keys.TRANSACTIONS] ?: 0,
                    categories = prefs[Keys.CATEGORIES] ?: 0,
                    categoryKeywords = prefs[Keys.CATEGORY_KEYWORDS] ?: 0,
                    categoryMappings = prefs[Keys.CATEGORY_MAPPINGS] ?: 0,
                    darkThemeSet = prefs[Keys.DARK_THEME_SET] ?: false,
                )
            }
    }

    override suspend fun clearUserDetails() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.USER_ID)
            prefs.remove(Keys.BACKUP_USER_ID)
            prefs.remove(Keys.DYNAMO_USER_ID)
            prefs.remove(Keys.FIRST_NAME)
            prefs.remove(Keys.LAST_NAME)
            prefs.remove(Keys.EMAIL)
            prefs.remove(Keys.PHONE_NUMBER)
            prefs.remove(Keys.PASSWORD)
            prefs.remove(Keys.TOKEN)
            prefs.remove(Keys.PAYMENT_STATUS)
            prefs.remove(Keys.PAID_AT_USER)
            prefs.remove(Keys.EXPIRED_AT)
            prefs.remove(Keys.SUPABASE_LOGIN)
            prefs.remove(Keys.PERMANENT_USER)
            prefs.remove(Keys.BACKUP_SET)
            prefs.remove(Keys.BACKUP_WORKER_INITIATED)
            prefs.remove(Keys.LAST_BACKUP)
            prefs.remove(Keys.BACKED_UP_ITEMS_SIZE)
            prefs.remove(Keys.TRANSACTIONS)
            prefs.remove(Keys.CATEGORIES)
            prefs.remove(Keys.CATEGORY_KEYWORDS)
            prefs.remove(Keys.CATEGORY_MAPPINGS)
            prefs.remove(Keys.DARK_THEME_SET)
        }
    }

    // ─── UserSession ────────────────────────────────────────────────────────

    override suspend fun saveUserSession(userSession: UserSession) {
        context.dataStore.edit { prefs ->
            userSession.userId?.let { prefs[Keys.SESSION_USER_ID] = it }
            prefs[Keys.ACCESS_TOKEN] = userSession.accessToken ?: ""
            prefs[Keys.REFRESH_TOKEN] = userSession.refreshToken ?: ""
            prefs[Keys.TOKEN_TYPE] = userSession.tokenType ?: ""
            userSession.accessTokenExpiresIn?.let { prefs[Keys.ACCESS_TOKEN_EXPIRES_IN] = it }
            userSession.refreshTokenExpiresIn?.let { prefs[Keys.REFRESH_TOKEN_EXPIRES_IN] = it }
        }
    }

    override fun getUserSession(): Flow<UserSession> {
        return context.dataStore.data
            .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
            .map { prefs ->
                UserSession(
                    userId = prefs[Keys.SESSION_USER_ID],
                    accessToken = prefs[Keys.ACCESS_TOKEN]?.takeIf { it.isNotEmpty() },
                    refreshToken = prefs[Keys.REFRESH_TOKEN]?.takeIf { it.isNotEmpty() },
                    tokenType = prefs[Keys.TOKEN_TYPE]?.takeIf { it.isNotEmpty() },
                    accessTokenExpiresIn = prefs[Keys.ACCESS_TOKEN_EXPIRES_IN],
                    refreshTokenExpiresIn = prefs[Keys.REFRESH_TOKEN_EXPIRES_IN],
                )
            }
    }

    override suspend fun clearUserSession() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.SESSION_USER_ID)
            prefs.remove(Keys.ACCESS_TOKEN)
            prefs.remove(Keys.REFRESH_TOKEN)
            prefs.remove(Keys.TOKEN_TYPE)
            prefs.remove(Keys.ACCESS_TOKEN_EXPIRES_IN)
            prefs.remove(Keys.REFRESH_TOKEN_EXPIRES_IN)
        }
    }

    // ─── Clear all ──────────────────────────────────────────────────────────

    override suspend fun clearAll() {
        context.dataStore.edit { it.clear() }
    }
}
