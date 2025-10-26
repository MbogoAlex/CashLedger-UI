package com.records.pesa.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.records.pesa.db.dao.CategoryDao
import com.records.pesa.db.dao.TransactionsDao
import com.records.pesa.db.dao.UserDao
import com.records.pesa.db.migration.MIGRATION_29_30
import com.records.pesa.db.migration.MIGRATION_30_31
import com.records.pesa.db.migration.MIGRATION_40_41
import com.records.pesa.db.migration.MIGRATION_48_49
import com.records.pesa.db.models.Budget
import com.records.pesa.db.models.CategoryKeyword
import com.records.pesa.db.models.DeletedTransaction
import com.records.pesa.db.models.Transaction
import com.records.pesa.db.models.TransactionCategory
import com.records.pesa.db.models.TransactionCategoryCrossRef
import com.records.pesa.db.models.UserAccount
import com.records.pesa.db.models.UserPreferences
import com.records.pesa.db.models.UserSession
import com.records.pesa.models.dbModel.AppLaunchStatus
import com.records.pesa.models.dbModel.UserDetails

@Database(entities = [UserDetails::class, UserSession::class, AppLaunchStatus::class, Budget::class, TransactionCategory::class, Transaction::class, CategoryKeyword::class, UserAccount::class, TransactionCategoryCrossRef::class, DeletedTransaction::class, UserPreferences::class], version = 49, exportSchema = false)
@TypeConverters(Coverters::class)
abstract class AppDatabase: RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun transactionDao(): TransactionsDao
    abstract fun categoryDao(): CategoryDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var Instance: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase {
            return Instance ?: synchronized(this) {
                val dbFile = context.getDatabasePath("CashLedger_db")
                val callback = object : RoomDatabase.Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Insert default preferences for new databases
                        db.execSQL("""
                            INSERT INTO `userPreferences` (
                                `id`, `loggedIn`, `darkMode`, `restoredData`, `lastRestore`,
                                `paid`, `permanent`, `paidAt`, `expiryDate`, `showBalance`, `hasSubmittedMessages`
                            ) VALUES (
                                1, 0, 0, 0, NULL, 0, 0, NULL, NULL, 1, 0
                            )
                        """)
                    }
                }

                val builder = Room.databaseBuilder(context, AppDatabase::class.java, "CashLedger_db")
                    .addMigrations(MIGRATION_29_30, MIGRATION_30_31, MIGRATION_40_41, MIGRATION_48_49)
                    .addCallback(callback)
                    .fallbackToDestructiveMigration()

                builder.build().also { Instance = it }
            }

        }
    }
}





