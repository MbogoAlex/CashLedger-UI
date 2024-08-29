package com.records.pesa.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.records.pesa.db.dao.CategoryDao
import com.records.pesa.db.dao.TransactionsDao
import com.records.pesa.db.dao.UserDao
import com.records.pesa.db.migration.MIGRATION_20_21
import com.records.pesa.db.models.Budget
import com.records.pesa.db.models.CategoryKeyword
import com.records.pesa.db.models.Transaction
import com.records.pesa.db.models.TransactionCategory
import com.records.pesa.db.models.TransactionCategoryCrossRef
import com.records.pesa.db.models.UserAccount
import com.records.pesa.models.dbModel.AppLaunchStatus
import com.records.pesa.models.dbModel.UserDetails

@Database(entities = [UserDetails::class, AppLaunchStatus::class, Budget::class, TransactionCategory::class, Transaction::class, CategoryKeyword::class, UserAccount::class, TransactionCategoryCrossRef::class], version = 21, exportSchema = false)
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
                val builder = if (!dbFile.exists()) {
                    Room.databaseBuilder(context, AppDatabase::class.java, "CashLedger_db")
                        .createFromAsset("database/app_launch.db")
                        .fallbackToDestructiveMigration()
                } else {
                    Room.databaseBuilder(context, AppDatabase::class.java, "CashLedger_db")
//                        .fallbackToDestructiveMigration()
                        .addMigrations(
//                            MIGRATION_7_8,
//                            MIGRATION_8_9,
//                            MIGRATION_9_10,
//                            MIGRATION_10_11,
//                            MIGRATION_11_12,
//                            MIGRATION_12_13,
//                            MIGRATION_13_14,
//                            MIGRATION_14_15,
//                            MIGRATION_15_16,
//                            MIGRATION_16_17,
//                            MIGRATION_17_18,
//                            MIGRATION_18_19,
                            MIGRATION_20_21,
                        )
                }

                builder.build().also { Instance = it }
            }

        }
    }
}





